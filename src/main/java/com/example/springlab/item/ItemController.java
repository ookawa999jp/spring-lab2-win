package com.example.springlab.item;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/items")
public class ItemController {

  private final ItemService itemService;

  public ItemController(ItemService itemService) {
    this.itemService = itemService;
  }

  @PostMapping
  public ResponseEntity<ItemResponse> create(@Valid @RequestBody CreateItemRequest request) {
    Item item = itemService.create(request.name(), request.price(), request.description());
    return ResponseEntity.created(URI.create("/items/" + item.getId()))
        .body(ItemResponse.from(item));
  }

  @GetMapping
  public List<ItemResponse> findAll() {
    return itemService.findAll().stream().map(ItemResponse::from).toList();
  }
}
