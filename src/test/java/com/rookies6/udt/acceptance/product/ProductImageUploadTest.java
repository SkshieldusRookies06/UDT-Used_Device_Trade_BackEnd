package com.rookies6.udt.acceptance.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rookies6.udt.acceptance.AcceptanceSupport;
import com.rookies6.udt.acceptance.AcceptanceTest;
import com.rookies6.udt.common.BusinessException;
import com.rookies6.udt.common.ErrorCode;
import com.rookies6.udt.dto.ProductCreateRequest;
import com.rookies6.udt.dto.ProductDetailResponse;
import com.rookies6.udt.dto.ProductImageResponse;
import com.rookies6.udt.entity.Category;
import com.rookies6.udt.entity.User;
import com.rookies6.udt.service.ProductService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * 기능: 상품 사진 저장(SPEC §4.4 · T-007 · T-023의 FileStorageService 호출) — BE-C 0절 ②.
 * 컨트롤러를 거치지 않고 {@code ProductService.create(sellerId, request, images)}를 직접 불러 ⑤(CurrentUser)와 분리했다.
 * 업로드 폴더는 테스트용 임시 폴더다(AcceptanceSupport.UPLOAD_DIR).
 */
@AcceptanceTest
@DisplayName("[BE-C ②] 상품 사진 저장 — 디스크·DB에 남고, 실패하면 아무것도 안 남는다")
class ProductImageUploadTest extends AcceptanceSupport {

    @Autowired private ProductService productService;

    private User seller;
    private Category category;

    @BeforeEach
    void setUp() {
        seller = member("iu-seller", 0L);
        category = category();
        flushAndClear();
    }

    private ProductCreateRequest request() {
        return new ProductCreateRequest("사진 테스트", "설명입니다", 100_000L, "A", String.valueOf(category.getId()));
    }

    private static MockMultipartFile emptyPart() {
        return new MockMultipartFile("images", "", "application/octet-stream", new byte[0]); // 파일 미선택
    }

    @Test
    @DisplayName("② 사진 3장 → 응답 images 3개 · sortOrder 0·1·2 · url은 /api/products/{id}/images/{imageId}")
    void 사진_세_장이_순서대로_붙는다() {
        ProductDetailResponse res = productService.create(seller.getId(), request(),
                List.of(png("a.png"), png("b.png"), png("c.png")));

        List<ProductImageResponse> images = res.images();
        assertThat(images).hasSize(3);
        assertThat(images).extracting(ProductImageResponse::sortOrder).containsExactly(0, 1, 2);
        assertThat(images.get(0).url()).startsWith("/api/products/" + res.id() + "/images/");
    }

    @Test
    @DisplayName("② 사진 3장 → 업로드 폴더(products)에 파일 3개가 실제로 생긴다")
    void 사진이_디스크에_저장된다() {
        long before = storedProductFiles();

        productService.create(seller.getId(), request(), List.of(png("a.png"), png("b.png"), png("c.png")));

        assertThat(storedProductFiles() - before).isEqualTo(3);
    }

    @Test
    @DisplayName("② 빈 파트(파일 미선택)는 세지도 저장하지도 않는다 — 사진 5장 + 빈 파트 1개는 FILE_COUNT_EXCEEDED가 아니다")
    void 빈_파트는_무시한다() {
        ProductDetailResponse res = productService.create(seller.getId(), request(),
                List.of(png("1.png"), png("2.png"), png("3.png"), png("4.png"), png("5.png"), emptyPart()));

        assertThat(res.images()).hasSize(5);
    }

    @Test
    @DisplayName("② 위장 파일(글자를 .jpg로) → FILE_TYPE_NOT_ALLOWED가 그대로 나가고, 앞서 저장한 사진 파일도 지워진다")
    void 위장_파일이면_아무것도_안_남는다() {
        long filesBefore = storedProductFiles();
        MockMultipartFile fake = new MockMultipartFile("images", "fake.jpg", "image/jpeg",
                "this is not an image".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> productService.create(seller.getId(), request(), List.<MultipartFile>of(png("ok.png"), fake)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_TYPE_NOT_ALLOWED);

        assertThat(storedProductFiles()).as("ok.png가 디스크에 남으면 고아 파일 — catch에서 delete 해야 한다").isEqualTo(filesBefore);
    }

    @Test
    @DisplayName("(지킴) 사진 6장 → FILE_COUNT_EXCEEDED")
    void 여섯_장은_안_된다() {
        assertThatThrownBy(() -> productService.create(seller.getId(), request(),
                List.of(png("1.png"), png("2.png"), png("3.png"), png("4.png"), png("5.png"), png("6.png"))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_COUNT_EXCEEDED);
    }

    @Test
    @DisplayName("(지킴) ③ 없는 카테고리 → VALIDATION_ERROR — Long.parseLong으로 바꿔도 그대로")
    void 없는_카테고리는_입력값_오류() {
        ProductCreateRequest noSuchCategory = new ProductCreateRequest("t", "d", 1000L, "A", "999999999");
        assertThatThrownBy(() -> productService.create(seller.getId(), noSuchCategory, List.of()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }
}
