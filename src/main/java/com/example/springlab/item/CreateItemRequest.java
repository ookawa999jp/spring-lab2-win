package com.example.springlab.item;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 商品作成リクエスト。元 NestJS の CreateItemDto と同一のバリデーション。
 *
 * <ul>
 *   <li>name: 必須・最大40文字
 *   <li>price: 必須・最小1
 *   <li>description: 任意・最大1000文字
 * </ul>
 */
public record CreateItemRequest(
        @NotBlank @Size(max = 40) String name,
        @NotNull @Min(1) Integer price,
        @Size(max = 1000) String description) {
}
