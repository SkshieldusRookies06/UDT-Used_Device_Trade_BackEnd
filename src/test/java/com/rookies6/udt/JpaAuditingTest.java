package com.rookies6.udt;

import static org.assertj.core.api.Assertions.assertThat;

import com.rookies6.udt.entity.Role;
import com.rookies6.udt.entity.User;
import com.rookies6.udt.repository.UserRepository;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class JpaAuditingTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void createdAt_is_filled_on_save() {
        OffsetDateTime before = OffsetDateTime.now().minusMinutes(1);

        User saved = userRepository.saveAndFlush(User.builder()
                .email("auditing-probe@udt.test")
                .password("not-a-real-hash")
                .nickname("auditing-probe")
                .role(Role.MEMBER)
                .balanceKrw(0L)
                .build());

        OffsetDateTime createdAt = saved.getCreatedAt();
        assertThat(createdAt).isNotNull();
        assertThat(createdAt.getOffset()).isNotNull();
        assertThat(createdAt).isAfter(before);
    }
}
