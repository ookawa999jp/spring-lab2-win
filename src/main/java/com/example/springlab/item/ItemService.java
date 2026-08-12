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

  @Transactional(rollbackFor = Exception.class)
  public Item create(String name, Integer price, String description) {
    Item item = new Item(name, price, description);
    return itemRepository.save(item);
  }

  @Transactional(readOnly = true)
  public List<Item> findAll() {
    return itemRepository.findAll();
  }
}
