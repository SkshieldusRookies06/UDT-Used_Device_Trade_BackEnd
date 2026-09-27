package com.rookies6.udt.acceptance;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인수 테스트 공통 설정 — 실제 스프링 컨텍스트 + MariaDB(local 프로파일) + MockMvc. 테스트마다 롤백.
 * 이 어노테이션을 붙이고 {@link AcceptanceSupport}를 상속하면 된다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
public @interface AcceptanceTest {
}
