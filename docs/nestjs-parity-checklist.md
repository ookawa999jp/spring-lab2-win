# 実装チェックリスト: NestJS Item機能の Spring Boot 移植

前提資料: [nestjs-parity-investigation.md](./nestjs-parity-investigation.md)

各項目は「完了条件」を満たしたらチェック。上から順に進めると依存関係が破綻しない。

> **実装ステータス: 完了（2026-08-13）。** 全エンドポイントを起動・実機検証済み（§9参照）。
> 計画との差異1点: `name` 列は Hibernate が `@Size(max=40)` を優先し **varchar(40)**
> になった（計画では255）。入力上限が40のため機能上問題なし。

---

## 0. 決定事項（確定済み）

- [x] ID は **`Long` IDENTITY を維持**（UUID化しない。Spring Boot の都合を優先）
- [x] `/items` は **いったん認証なしで全部動く**ようにする（既存セキュリティコードは残す）
- [x] enum は **varchar 方式**（`@Enumerated(EnumType.STRING)`）で進める
- [x] 404 のエラー形式は移植先の `ErrorResponse` に合わせる
- [x] 開発DBの `item` テーブルを空/再作成できることを確認（`status` NOT NULL 追加のため）

---

## 1. ドメインモデル

- [x] `ItemStatus` enum を新規作成（`ON_SALE`, `SOLD_OUT`）
- [x] `Item` エンティティ改修
  - [x] `id` は **`Long` + `@GeneratedValue(strategy = GenerationType.IDENTITY)` を維持**（変更不要）
  - [x] `name`: `@NotBlank` + `@Size(max = 40)` / 列 `@Column(length = 255)`
  - [x] `price`: `@NotNull @Min(1)`
  - [x] `description`: 任意化（`@NotBlank`削除）/ `@Size(max = 1000)` / `@Column(columnDefinition = "text")` / nullable
  - [x] `status`: `@Enumerated(EnumType.STRING)`、既定 `ON_SALE`、`@Column(nullable = false)`
  - [x] `createdAt`: `LocalDateTime` + `@CreationTimestamp`
  - [x] `updatedAt`: `LocalDateTime` + `@UpdateTimestamp`
  - [x] コンストラクタ / getter を新フィールドに合わせて更新
  - [x] status 更新用の振る舞い（例: `markSoldOut()`）を用意（setter乱用を避ける）

## 2. リポジトリ

- [x] `ItemRepository` は `JpaRepository<Item, Long>` のまま（変更不要）

## 3. DTO / レスポンス

- [x] `CreateItemRequest` 改修
  - [x] `name`: `@NotBlank @Size(max = 40)`
  - [x] `price`: `@NotNull @Min(1)`
  - [x] `description`: 任意（`@Size(max = 1000)`、必須制約なし）
- [x] `ItemResponse` 改修
  - [x] `id(Long)`, `name`, `price`, `description`, `status`, `createdAt`, `updatedAt` を含める
  - [x] `from(Item)` を更新

## 4. 例外・エラーハンドリング

- [x] `NotFoundException`（またはドメイン例外）を用意（メッセージ「商品が存在しません」）
- [x] `GlobalExceptionHandler` に 404 ハンドラ追加（`code="NOT_FOUND"`）
- [x] `GlobalExceptionHandler` に 400 ハンドラ追加（`MethodArgumentTypeMismatchException` = 数値でない不正ID）

## 5. サービス層（トランザクション付与が主眼）

- [x] `create`: `@Transactional(rollbackFor = Exception.class)`、status=ON_SALE で保存
- [x] `findAll`: `@Transactional(readOnly = true)`
- [x] `findById(Long)`: `@Transactional(readOnly = true)`、無ければ `NotFoundException`
- [x] `updateStatus(Long)`: `@Transactional(rollbackFor = Exception.class)`、SOLD_OUT に更新、無ければ404
- [x] `delete(Long)`: `@Transactional(rollbackFor = Exception.class)`、無ければ404

## 6. コントローラ層

- [x] `GET /items` → 全件（`List<ItemResponse>`）
- [x] `GET /items/{id}` → 1件（`@PathVariable Long id`）
- [x] `POST /items` → 作成（201 + Location）
- [x] `PUT /items/{id}` → SOLD_OUT 更新（200 + `ItemResponse`）
- [x] `DELETE /items/{id}` → 削除（204 No Content）

## 7. セキュリティ（いったん認証なしで全部動かす）

- [x] `SecurityPaths.PUBLIC_ENDPOINTS` に `/items/**` を追加し、`/items` を公開する
- [x] 追加箇所に**コメントで説明**を入れる（例:「NestJS版に合わせ一時的に認証なし。
      本番相当に戻す際はこの `/items/**` を削除する」）
- [x] **既存のセキュリティ関連コードは削除しない**
      （`SecurityConfig` / oauth2 resource server / `AuthController` / `AuthService` /
      JWT 設定はそのまま残す）
- [x] JWT なしで全 `/items` エンドポイントが 401 にならず叩けることを確認

## 8. 設定 / DB

- [x] 開発DBの `item` テーブルを空にする or drop して再作成させる
      （`status` NOT NULL 追加で既存行が整合しなくなるのを防ぐ）
- [x] 起動時に Hibernate が新スキーマ（status, createdAt, updatedAt）で作成/更新されることを確認

## 9. 動作確認

- [x] アプリ起動成功（`./mvnw spring-boot:run` 等）
- [x] `POST /items`（正常）→ 201, status=ON_SALE, createdAt/updatedAt 付与
- [x] `POST /items`（name>40 / price<1）→ 400
- [x] `GET /items` → 一覧に反映
- [x] JWT を付けずに全エンドポイントが叩ける（401 にならない）
- [x] `GET /items/{id}`（存在）→ 200 / （不存在）→ 404「商品が存在しません」
- [x] `GET /items/{数値でないID}` → 400
- [x] `PUT /items/{id}` → status=SOLD_OUT, updatedAt 更新
- [x] `DELETE /items/{id}`（存在）→ 204 / （不存在）→ 404
- [x] トランザクション確認: `create`/`updateStatus` 内で例外を意図的に発生させ、ロールバックされること
      （`ItemTransactionRollbackTest` で「save 後に例外→行が残らない」ことを検証済み）

## 10. 品質

- [x] Windows + IntelliJ IDEA 版では Spotless を外し、IntelliJ IDEA 標準フォーマッター方針に合わせた（`pom.xml` から `spotless-maven-plugin` を削除済み）
- [x] 既存テスト（`SpringLab2WinApplicationTests`）がグリーン
- [x] （任意）Item CRUD の統合テスト追加（`ItemIntegrationTest`: MockMvc で CRUD/404/400 を検証、10件）
</content>
