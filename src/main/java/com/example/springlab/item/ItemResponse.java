package com.example.springlab.item;

import java.time.LocalDateTime;

/** 商品レスポンス。元 NestJS が Item 全体を返すのに合わせ、status / 作成・更新日時を含める。 */
public record ItemResponse(
    Long id,
    String name,
    Integer price,
    String description,
    ItemStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static ItemResponse from(Item item) {
    return new ItemResponse(
        item.getId(),
        item.getName(),
        item.getPrice(),
        item.getDescription(),
        item.getStatus(),
        item.getCreatedAt(),
        item.getUpdatedAt());
  }
}
