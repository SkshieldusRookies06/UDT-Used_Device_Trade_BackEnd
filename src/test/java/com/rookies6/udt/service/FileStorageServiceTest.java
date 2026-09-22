package com.rookies6.udt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rookies6.udt.common.BusinessException;
import com.rookies6.udt.common.ErrorCode;
import com.rookies6.udt.dto.StoredFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class FileStorageServiceTest {

    private static final byte[] PNG_HEAD = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] JPEG_HEAD = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};

    @TempDir
    Path uploadDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(uploadDir.toString());
        fileStorageService.createDirectories();
    }

    @Test
    @DisplayName("기동하면 저장 하위 디렉터리를 만든다")
    void createsSubdirectories() {
        assertThat(uploadDir.resolve("products")).isDirectory();
        assertThat(uploadDir.resolve("disputes")).isDirectory();
    }

    @Test
    @DisplayName("저장 이름은 UUID + 확장자이고 원본 이름은 반환값에만 남는다")
    void storesUnderUuidName() throws IOException {
        StoredFile stored = fileStorageService.store(png("내 사진.png"), FileStorageService.PRODUCTS);

        assertThat(stored.originalName()).isEqualTo("내 사진.png");
        assertThat(stored.sizeBytes()).isEqualTo(PNG_HEAD.length);
        assertThat(stored.storedName()).endsWith(".png");
        assertThat(UUID.fromString(stored.storedName().replace(".png", ""))).isNotNull();
        assertThat(uploadDir.resolve("products").resolve(stored.storedName())).exists();
        assertThat(storedNames()).containsExactly(stored.storedName());
    }

    @Test
    @DisplayName("확장자만 이미지인 위장 파일은 내용 검사에서 걸린다")
    void rejectsDisguisedFile() {
        MultipartFile disguised = new MockMultipartFile(
                "images", "fake.jpg", "image/jpeg", "이건 그냥 텍스트다".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> fileStorageService.store(disguised, FileStorageService.PRODUCTS))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        assertThat(storedNames()).isEmpty();
    }

    @Test
    @DisplayName("확장자와 앞머리 바이트가 어긋나면 거부한다")
    void rejectsMismatchedSignature() {
        MultipartFile pngBytesNamedJpg = new MockMultipartFile("images", "x.jpg", "image/jpeg", PNG_HEAD);

        assertThatThrownBy(() -> fileStorageService.store(pngBytesNamedJpg, FileStorageService.PRODUCTS))
                .isInstanceOf(BusinessException.class);
        assertThat(storedNames()).isEmpty();
    }

    @Test
    @DisplayName("5MB를 넘으면 FILE_TOO_LARGE")
    void rejectsOversizedFile() {
        byte[] sixMegabytes = new byte[6 * 1024 * 1024];
        System.arraycopy(JPEG_HEAD, 0, sixMegabytes, 0, JPEG_HEAD.length);
        MultipartFile big = new MockMultipartFile("images", "big.jpg", "image/jpeg", sixMegabytes);

        assertThatThrownBy(() -> fileStorageService.store(big, FileStorageService.PRODUCTS))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_TOO_LARGE);
        assertThat(storedNames()).isEmpty();
    }

    @Test
    @DisplayName("원본 이름이 경로 조작이어도 저장 위치를 벗어나지 않는다")
    void ignoresPathInOriginalName() {
        StoredFile stored = fileStorageService.store(png("../../evil.png"), FileStorageService.PRODUCTS);

        assertThat(stored.originalName()).isEqualTo("evil.png");
        assertThat(stored.storedName()).doesNotContain("..").doesNotContain("evil");
        assertThat(uploadDir.resolve("products").resolve(stored.storedName())).exists();
        assertThat(uploadDir.getParent().resolve("evil.png")).doesNotExist();
    }

    @Test
    @DisplayName("저장 이름이 상위 경로를 가리키면 읽지 않는다")
    void rejectsTraversalOnLoad() {
        assertThatThrownBy(() -> fileStorageService.load(FileStorageService.PRODUCTS, "../../secret.png"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_STORAGE_ERROR);
    }

    @Test
    @DisplayName("pdf는 분쟁 증빙에만 허용된다")
    void allowsPdfOnlyForDisputes() {
        MultipartFile pdf = new MockMultipartFile(
                "files", "증빙.pdf", "application/pdf", "%PDF-1.4 본문".getBytes(StandardCharsets.UTF_8));

        assertThat(fileStorageService.store(pdf, FileStorageService.DISPUTES).storedName()).endsWith(".pdf");

        MultipartFile samePdf = new MockMultipartFile(
                "images", "증빙.pdf", "application/pdf", "%PDF-1.4 본문".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> fileStorageService.store(samePdf, FileStorageService.PRODUCTS))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_TYPE_NOT_ALLOWED);
    }

    @Test
    @DisplayName("contentType은 저장 이름의 확장자에서 나온다")
    void resolvesContentType() {
        assertThat(fileStorageService.contentType("a.jpg")).isEqualTo("image/jpeg");
        assertThat(fileStorageService.contentType("a.png")).isEqualTo("image/png");
        assertThat(fileStorageService.contentType("a.webp")).isEqualTo("image/webp");
        assertThat(fileStorageService.contentType("a.pdf")).isEqualTo("application/pdf");
        assertThat(fileStorageService.contentType("a")).isEqualTo("application/octet-stream");
    }

    private MultipartFile png(String originalName) {
        return new MockMultipartFile("images", originalName, "image/png", PNG_HEAD);
    }

    private List<String> storedNames() {
        try (var files = Files.list(uploadDir.resolve("products"))) {
            return files.map(p -> p.getFileName().toString()).toList();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
