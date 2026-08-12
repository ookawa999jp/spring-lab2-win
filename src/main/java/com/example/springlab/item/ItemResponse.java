package com.example.springlab.item;

public record ItemResponse(Long id, String name, Integer price, String description) {

  public static ItemResponse from(Item item) {
    return new ItemResponse(item.getId(), item.getName(), item.getPrice(), item.getDescription());
  }
}
