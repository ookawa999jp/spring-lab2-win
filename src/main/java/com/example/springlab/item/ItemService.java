package com.example.springlab.item;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
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
     */
    @Transactional(rollbackFor = Exception.class)
    public Item updateStatus(Long id) {
        Item item = itemRepository.findById(id).orElseThrow(ItemNotFoundException::new);
        item.markSoldOut();
        // トランザクション内のため、dirty checking により flush 時に UPDATE される。
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
