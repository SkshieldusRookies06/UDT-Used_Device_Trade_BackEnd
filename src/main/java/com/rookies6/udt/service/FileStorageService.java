package com.rookies6.udt.service;

import com.rookies6.udt.common.BusinessException;
import com.rookies6.udt.common.ErrorCode;
import com.rookies6.udt.dto.StoredFile;
import jakarta.annotation.PostConstruct;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    public static final String PRODUCTS = "products";
    public static final String DISPUTES = "disputes";

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final int ORIGINAL_NAME_MAX = 200;
    private static final int SIGNATURE_LENGTH = 12;

    private static final Map<String, Set<String>> ALLOWED_EXTENSIONS = Map.of(
            PRODUCTS, Set.of("jpg", "jpeg", "png", "webp"),
            DISPUTES, Set.of("jpg", "jpeg", "png", "webp", "pdf"));

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp",
            "pdf", "application/pdf");

    private final Path root;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void createDirectories() {
        try {
            for (String subdir : ALLOWED_EXTENSIONS.keySet()) {
                Files.createDirectories(root.resolve(subdir));
            }
        } catch (IOException e) {
            throw new IllegalStateException("업로드 디렉터리를 만들지 못했습니다: " + root, e);
        }
    }

    public StoredFile store(MultipartFile file, String subdir) {
        Set<String> allowed = allowedExtensions(subdir);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        String originalName = originalName(file);
        String extension = extensionOf(originalName);
        if (!allowed.contains(extension)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }

        String storedName = UUID.randomUUID() + "." + extension;
        Path target = root.resolve(subdir).resolve(storedName);
        try (InputStream in = new BufferedInputStream(file.getInputStream())) {
            in.mark(SIGNATURE_LENGTH);
            byte[] head = in.readNBytes(SIGNATURE_LENGTH);
            if (!matchesSignature(head, extension)) {
                throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
            }
            in.reset();
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }
        return new StoredFile(storedName, originalName, file.getSize());
    }

    /** 존재 여부는 검사하지 않는다 — 없는 파일을 무엇으로 볼지는 호출하는 쪽 계약이다. */
    public Resource load(String subdir, String storedName) {
        allowedExtensions(subdir);
        Path directory = root.resolve(subdir);
        Path target = directory.resolve(storedName).normalize();
        if (!target.startsWith(directory)) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }
        try {
            return new UrlResource(target.toUri());
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    public String contentType(String storedName) {
        return CONTENT_TYPES.getOrDefault(extensionOf(storedName), "application/octet-stream");
    }

    private Set<String> allowedExtensions(String subdir) {
        Set<String> allowed = ALLOWED_EXTENSIONS.get(subdir);
        if (allowed == null) {
            throw new IllegalArgumentException("허용되지 않은 저장 위치: " + subdir);
        }
        return allowed;
    }

    private String originalName(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        name = name.substring(slash + 1);
        return name.length() > ORIGINAL_NAME_MAX ? name.substring(0, ORIGINAL_NAME_MAX) : name;
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** 확장자를 믿지 않는다 — 앞머리 바이트가 확장자와 맞을 때만 저장한다. */
    private boolean matchesSignature(byte[] head, String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> startsWith(head, 0xFF, 0xD8, 0xFF);
            case "png" -> startsWith(head, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "webp" -> hasAscii(head, 0, "RIFF") && hasAscii(head, 8, "WEBP");
            case "pdf" -> hasAscii(head, 0, "%PDF-");
            default -> false;
        };
    }

    private boolean startsWith(byte[] head, int... signature) {
        if (head.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((head[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean hasAscii(byte[] head, int offset, String expected) {
        byte[] bytes = expected.getBytes(StandardCharsets.US_ASCII);
        if (head.length < offset + bytes.length) {
            return false;
        }
        for (int i = 0; i < bytes.length; i++) {
            if (head[offset + i] != bytes[i]) {
                return false;
            }
        }
        return true;
    }
}
