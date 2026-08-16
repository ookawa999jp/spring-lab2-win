package com.example.springlab.item;

import jakarta.validation.Valid;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @GetMapping
    public List<ItemResponse> findAll() {
        return itemService.findAll().stream().map(ItemResponse::from).toList();
    }

    // id は Long。数値でない値が来た場合は MethodArgumentTypeMismatchException → 400（GlobalExceptionHandler）。
    @GetMapping("/{id}")
    public ItemResponse findById(@PathVariable Long id) {
        return ItemResponse.from(itemService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody CreateItemRequest request) {
        Item item = itemService.create(request.name(), request.price(), request.description());
        return ResponseEntity.created(URI.create("/items/" + item.getId()))
                .body(ItemResponse.from(item));
    }

    // 元 NestJS の PUT /items/:id と同じく、ステータスを SOLD_OUT に更新する。
    @PutMapping("/{id}")
    public ItemResponse updateStatus(@PathVariable Long id) {
        return ItemResponse.from(itemService.updateStatus(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        itemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
