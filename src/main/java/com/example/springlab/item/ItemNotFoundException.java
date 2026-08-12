package com.example.springlab.item;

/**
 * 商品が存在しない場合にスローする例外。
 *
 * <p>元の NestJS の {@code NotFoundException('商品が存在しません')} に相当。 {@link
 * com.example.springlab.error.GlobalExceptionHandler} で 404 に変換される。
 */
public class ItemNotFoundException extends RuntimeException {

  public ItemNotFoundException() {
    super("商品が存在しません");
  }
}
