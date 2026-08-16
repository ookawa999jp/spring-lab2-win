package com.example.springlab.item;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springlab.counter.CounterMapper;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final CounterMapper counterMapper;

    public ItemService(ItemRepository itemRepository, CounterMapper counterMapper) {
        this.itemRepository = itemRepository;
        this.counterMapper = counterMapper;
    }

    // 更新系はトランザクション境界を明示。元(Prisma)は未考慮だが、こちらでは @Transactional で担保する。
    @Transactional(rollbackFor = Exception.class)
    public Item create(String name, Integer price, String description) {
        Item item = new Item(name, price, description);
        return itemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Item findById(Long id) {
        return itemRepository.findById(id).orElseThrow(ItemNotFoundException::new);
    }

    /**
     * ステータスを SOLD_OUT に更新する（元 NestJS の updateStatus 相当）。
     *
     * 方向性3: Hibernate/JPA の更新と MyBatis の更新を同じ @Transactional 内に混在させ、
     * 例外発生時に両方まとめてロールバックされることを確認するための実装。
     */
    @Transactional(rollbackFor = Exception.class)
    public Item updateStatus(Long id) {
        Item item = itemRepository.findById(id).orElseThrow(ItemNotFoundException::new);

        item.markSoldOut();

        // flush() を呼ぶ理由:
        // item.markSoldOut() だけだと、Hibernate の dirty checking による UPDATE SQL は
        // トランザクションの commit 直前まで DB へ送られない場合がある。
        // その状態で例外を投げると、「Hibernate の UPDATE が DB に送られてからロールバックされた」のか、
        // 「そもそも UPDATE が DB に送られていなかっただけ」なのかが判別できず、検証として弱くなる。
        // flush() を呼ぶことで、ここで確実に UPDATE SQL を DB へ送り込んだうえで、
        // その後の例外によって Item 更新と Counter 更新が両方ロールバックされることを確認できる。
        itemRepository.flush();

        // 本来は UPDATE counter SET counter_value = counter_value + 1 の1文で足りるが、
        // 今回は Hibernate と MyBatis の混在トランザクション検証のため、
        // MyBatis の SELECT XML と UPDATE XML を両方通す冗長な実装にしている。
        Integer currentValue = counterMapper.selectCounterValue(1L);
        if (currentValue == null) {
            throw new IllegalStateException("counter row is not initialized (id=1 が存在しません)");
        }
        counterMapper.updateCounterValue(1L, currentValue);

        // ロールバック確認用。コメントを外して PUT /items/{id} を実行すると、
        // Hibernate の Item 更新（markSoldOut + flush 済み）と MyBatis の Counter 更新が
        // 同じトランザクションでまとめてロールバックされることを確認できる。
        final boolean isRollBackTest = false;
        if(isRollBackTest) {
            throw new RuntimeException("rollback test for mixed Hibernate and MyBatis transaction");
        }
        return item;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (!itemRepository.existsById(id)) {
            throw new ItemNotFoundException();
        }
        itemRepository.deleteById(id);
    }
}
