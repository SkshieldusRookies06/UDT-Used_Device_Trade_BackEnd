package com.rookies6.udt.acceptance;

import com.rookies6.udt.entity.Category;
import com.rookies6.udt.entity.ConditionGrade;
import com.rookies6.udt.entity.Product;
import com.rookies6.udt.entity.ProductStatus;
import com.rookies6.udt.entity.Role;
import com.rookies6.udt.entity.User;
import com.rookies6.udt.repository.CategoryRepository;
import com.rookies6.udt.repository.ProductRepository;
import com.rookies6.udt.repository.UserRepository;
import com.rookies6.udt.service.TransactionService;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 인수 테스트 공통 도우미 — 시드에 의존하지 않고 테스트마다 사용자·상품을 새로 만든다(@Transactional로 롤백).
 *
 * <p>인증은 JwtAuthenticationFilter가 SecurityContext에 넣는 것과 같은 모양(principal = userId 문자열)으로
 * {@code .with(authentication(memberAuth(user)))}로 직접 넣는다. 그래서 로그인 API(T-005)가 없어도
 * 거래·상품 컨트롤러 계약을 검사할 수 있다. (필터 자체는 auth 패키지 테스트가 본다.)
 *
 * <p>테스트 클래스 이름의 [BE-X ①②] = onboarding/역할별/BE-X.md 0절 표의 순서 번호.
 */
public abstract class AcceptanceSupport {

    /**
     * 업로드 폴더 — JVM 하나에 임시 폴더 하나. 스프링 컨텍스트는 테스트 클래스끼리 캐시·공유되므로
     * 클래스마다 지워지는 @TempDir을 쓰면 두 번째 클래스부터 폴더가 사라진다. 그래서 고정 임시 폴더를 쓴다.
     */
    protected static final Path UPLOAD_DIR = createUploadDir();

    @DynamicPropertySource
    static void uploadDirProperty(DynamicPropertyRegistry registry) {
        registry.add("app.upload-dir", UPLOAD_DIR::toString);
    }

    /** PNG 시그니처 8바이트 + 여유. FileStorageService가 확장자와 앞머리 바이트를 둘 다 본다. */
    protected static final byte[] PNG_HEAD = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 0, 0, 0, 0};

    @Autowired protected MockMvc mvc;
    @Autowired protected EntityManager em;
    @Autowired protected UserRepository userRepository;
    @Autowired protected CategoryRepository categoryRepository;
    @Autowired protected ProductRepository productRepository;
    @Autowired protected TransactionService transactionService;

    protected User member(String prefix, long balance) {
        return userRepository.saveAndFlush(User.builder()
                .email(prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@udt.test")
                .password("not-a-real-hash").nickname(prefix + "닉").role(Role.MEMBER).balanceKrw(balance).build());
    }

    /** data.sql 시드 계정 — 티켓 수용 기준의 시드 수치를 그대로 검사할 때 쓴다. 읽기만 하므로 롤백으로 원상복구된다. */
    protected User seedUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("시드에 없는 계정: " + email));
    }

    protected Category category() {
        return categoryRepository.saveAndFlush(Category.builder()
                .name("acc-" + UUID.randomUUID().toString().substring(0, 8)).build());
    }

    /** 판매 중 상품. 제목은 항상 {@link #PRODUCT_TITLE}. */
    protected Product onSaleProduct(User seller, long price) {
        Product p = productRepository.saveAndFlush(Product.builder()
                .seller(seller).category(category()).title(PRODUCT_TITLE).description("설명")
                .priceKrw(price).conditionGrade(ConditionGrade.A).build());
        p.changeStatus(ProductStatus.ON_SALE);
        return productRepository.saveAndFlush(p);
    }

    protected static final String PRODUCT_TITLE = "인수테스트 상품";

    /** 서비스로 구매해 PAID 거래를 하나 만들고 id를 돌려준다. 컨텍스트를 비워 다음 호출이 DB에서 새로 읽게 한다. */
    protected Long paidTransaction(User buyer, Product product) {
        Long txId = Long.valueOf(transactionService.purchase(buyer.getId(), product.getId()).id());
        flushAndClear();
        return txId;
    }

    /** 실제 요청처럼 — 쓴 것은 DB로 보내고, 1차 캐시는 비운다. */
    protected void flushAndClear() {
        em.flush();
        em.clear();
    }

    /** JwtAuthenticationFilter가 세우는 것과 동일한 인증 객체. */
    protected static Authentication memberAuth(User user) {
        return new UsernamePasswordAuthenticationToken(String.valueOf(user.getId()), null,
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
    }

    protected static MockMultipartFile png(String name) {
        return new MockMultipartFile("images", name, "image/png", PNG_HEAD);
    }

    /** uploads/products에 실제로 있는 파일 수 — "사진이 디스크에 남았나 / 실패 시 지웠나"를 본다. */
    protected static long storedProductFiles() {
        Path dir = UPLOAD_DIR.resolve("products");
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.count();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void deleteRecursively(Path dir) {
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
            // 임시 폴더라 남아도 해가 없다
        }
    }

    private static Path createUploadDir() {
        try {
            Path dir = Files.createTempDirectory("udt-test-uploads-");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteRecursively(dir)));
            return dir;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
