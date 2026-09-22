package com.rookies6.udt;

import static org.assertj.core.api.Assertions.assertThat;

import com.rookies6.udt.entity.Category;
import com.rookies6.udt.entity.ConditionGrade;
import com.rookies6.udt.entity.Product;
import com.rookies6.udt.entity.Role;
import com.rookies6.udt.entity.Transaction;
import com.rookies6.udt.entity.User;
import com.rookies6.udt.repository.CategoryRepository;
import com.rookies6.udt.repository.ProductRepository;
import com.rookies6.udt.repository.TransactionRepository;
import com.rookies6.udt.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class TransactionPerProductTest {

    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private TransactionRepository transactionRepository;

    @Test
    void same_product_can_have_more_than_one_transaction() {
        User seller = userRepository.saveAndFlush(user("uk-seller@udt.test"));
        User buyer = userRepository.saveAndFlush(user("uk-buyer@udt.test"));
        Category category = categoryRepository.saveAndFlush(Category.builder().name("uk-probe").build());
        Product product = productRepository.saveAndFlush(Product.builder()
                .seller(seller).category(category).title("uk-probe")
                .description("refunded then sold again").priceKrw(100_000L)
                .conditionGrade(ConditionGrade.A).build());

        transactionRepository.saveAndFlush(tx(product, buyer));
        transactionRepository.saveAndFlush(tx(product, buyer));

        assertThat(transactionRepository.findAll().stream()
                .filter(t -> t.getProduct().getId().equals(product.getId()))
                .count()).isEqualTo(2);
    }

    private User user(String email) {
        return User.builder().email(email).password("not-a-real-hash")
                .nickname(email).role(Role.MEMBER).balanceKrw(1_000_000L).build();
    }

    private Transaction tx(Product product, User buyer) {
        return Transaction.builder().product(product).buyer(buyer)
                .amountKrw(product.getPriceKrw()).build();
    }
}
