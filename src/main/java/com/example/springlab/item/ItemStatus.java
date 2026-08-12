package com.example.springlab.item;

/**
 * 商品ステータス。
 *
 * <p>元の NestJS(Prisma) の enum {@code ItemStatus} と同一。 Prisma はネイティブの PostgreSQL enum を使うが、こちらは
 * Hibernate の {@code @Enumerated(EnumType.STRING)} で VARCHAR として保存する。
 */
public enum ItemStatus {
  ON_SALE,
  SOLD_OUT
}
