package com.rookies6.udt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 시드 시각이 실제 지금과 맞는지 본다. DB 서버의 시간대 설정이 PC마다 달라 시드가 ±9시간으로 저장되는 사고를
 * 기계가 잡는다 — 게이트는 ISO 오프셋 형식만 보고 값이 말이 되는지는 보지 않는다.
 */
@SpringBootTest
@ActiveProfiles("local")
class SeedTimestampTest {

    @Autowired private JdbcTemplate jdbc;

    @Test
    void seed_timestamps_are_close_to_now() {
        OffsetDateTime now = OffsetDateTime.now();

        for (String sql : new String[]{
                "select created_at from products order by id limit 1",
                "select created_at from transactions order by id limit 1",
                "select created_at from users order by id limit 1"}) {

            OffsetDateTime seeded = jdbc.queryForObject(sql, OffsetDateTime.class);
            assertThat(seeded).as(sql).isNotNull();
            assertThat(Duration.between(seeded, now).abs())
                    .as("시드 시각이 현재와 %s 차이 (%s) — DB 시간대 설정을 확인하라: SELECT NOW(), UTC_TIMESTAMP(), @@system_time_zone",
                            Duration.between(seeded, now).abs(), seeded)
                    .isLessThan(Duration.ofHours(2));
        }
    }
}
