package com.example.springlab.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

/**
 * トランザクションのロールバック挙動の明示検証。
 *
 * <p>「元(Prisma)は未考慮だったトランザクションを、こちらでは {@code @Transactional} で担保できる」ことの裏付け。
 *
 * <p>意図的に「save 後に例外」を発生させ、コミットされずロールバックされる（＝DBに残らない）ことを確認する。 このテストは <b>クラスレベルの
 * {@code @Transactional} を付けない</b>。付けると外側のテスト用トランザクションが 全体を巻き戻してしまい、実際のコミット/ロールバックを検証できなくなるため。
 * 作成したデータは {@code @AfterEach} で明示的に後始末する。
 */
@SpringBootTest
class ItemTransactionRollbackTest {

    private static final String MARKER = "___rollback_marker___";

    @Autowired
    private FailingService failingService;
    @Autowired
    private ItemRepository itemRepository;

    @AfterEach
    void cleanUp() {
        itemRepository.findAll().stream()
                .filter(it -> MARKER.equals(it.getName()))
                .forEach(it -> itemRepository.deleteById(it.getId()));
    }

    @Test
    void savedRowIsRolledBackWhenExceptionThrownInsideTransaction() {
        long before = countMarkerRows();

        assertThatThrownBy(() -> failingService.saveThenFail(MARKER))
                .isInstanceOf(RuntimeException.class);

        long after = countMarkerRows();
        // 例外でロールバックされたため、save したはずの行は残っていない。
        assertThat(after).isEqualTo(before);
    }

    private long countMarkerRows() {
        return itemRepository.findAll().stream().filter(it -> MARKER.equals(it.getName())).count();
    }

    /**
     * 「save 後に例外を投げる」だけのテスト用サービス。{@code @Transactional} の proxy を効かせるため Bean 化する。
     */
    static class FailingService {
        private final ItemRepository itemRepository;

        FailingService(ItemRepository itemRepository) {
            this.itemRepository = itemRepository;
        }

        @Transactional(rollbackFor = Exception.class)
        void saveThenFail(String name) {
            itemRepository.save(new Item(name, 999, "ロールバックされるはず"));
            throw new RuntimeException("intentional failure after save");
        }
    }

    @TestConfiguration
    static class RollbackTestConfig {
        @Bean
        FailingService failingService(ItemRepository itemRepository) {
            return new FailingService(itemRepository);
        }
    }
}
