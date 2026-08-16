# NestJS(Prisma) → Spring Boot(Hibernate) 移植調査

対象: `/udemy/NestJs/nestjs-fileamarket` の Item 機能を、このコードベース
(`spring-lab2-win`) で同一仕様に再現する。

- 調査日: 2026-08-13
- 元アプリ: NestJS 11 + Prisma 7 (PostgreSQL, `@prisma/adapter-pg`)
- 移植先: Spring Boot 4.1.0 + Spring Data JPA(Hibernate) + PostgreSQL

---

## 1. 結論（先に要点）

- **再現は可能。** Prisma の Item モデル / CRUD は Hibernate + Spring Data JPA で
  そのままマッピングできる。技術的なブロッカーは無い。
- **トランザクションは `@Transactional` で意識できる。** 元(Prisma)は各操作が
  暗黙の単発クエリでトランザクション設計されていないが、移植先では
  Service 層に `@Transactional` を付けることで明示的に制御できる（既に
  `ItemService` に導入実績あり）。
- ただし **現状の `Item` エンティティと API は NestJS 版と差分が大きい**ため、
  そのままでは「同一仕様」にならない。下記の差分を埋める実装が必要。

### 方針決定（このプロジェクトの前提）

- **ID: `Long` IDENTITY を維持**（UUID化しない。Spring Boot / JPA の都合を優先）。
- **認証: いったん無効化し、`/items` を認証なしで全部動くようにする**。
  既存のセキュリティ関連コードは削除せず残し、コメントで一時対応の旨・戻し方を明記する。
- enum は varchar 方式、エラー形式は移植先の `ErrorResponse` に統一（詳細は §5）。

---

## 2. 元(NestJS/Prisma)の仕様

### データモデル (`prisma/schema.prisma` / migration.sql)

| フィールド    | 型 / DB                         | 制約・既定値                    |
|--------------|--------------------------------|-------------------------------|
| id           | String / `UUID`                | PK, `@default(uuid())`         |
| name         | String / `VARCHAR(255)`        | NOT NULL                       |
| price        | Int / `INTEGER`                | NOT NULL                       |
| description  | String? / `TEXT`               | NULL 可                        |
| status       | enum `ItemStatus` (`ON_SALE`/`SOLD_OUT`) | NOT NULL, 既定 `ON_SALE` |
| createdAt    | DateTime / `TIMESTAMP(0)`      | 既定 `now()`                   |
| updatedAt    | DateTime / `TIMESTAMP(0)`      | `@updatedAt`（更新時自動）      |

### 入力バリデーション (`create-item.dto.ts`)

- `name`: string / 空不可 / **最大40文字**（※DBは255だが入力制限は40）
- `price`: int / **最小1**
- `description`: 任意 / string / 最大1000文字

### API (`items.controller.ts` / `items.service.ts`)

| メソッド | パス          | 動作                                    | 異常系                       |
|---------|--------------|----------------------------------------|-----------------------------|
| GET     | `/items`     | 全件取得                                | -                           |
| GET     | `/items/:id` | 1件取得（`ParseUUIDPipe`でUUID検証）     | 無ければ404「商品が存在しません」 |
| POST    | `/items`     | 作成（status は `ON_SALE` 固定）         | バリデーション400            |
| PUT     | `/items/:id` | ステータスを `SOLD_OUT` に更新           | 無ければ404「商品が存在しません」 |
| DELETE  | `/items/:id` | 削除                                    | 無ければ404「商品が存在しません」 |

- グローバルに `ValidationPipe` 適用。
- **認証は無し**（誰でも叩ける）。
- **トランザクションは未使用**（単発クエリのみ）。

---

## 3. 移植先(Spring Boot)の現状

### 既にあるもの

- `Item`（エンティティ / `@Entity`）、`ItemRepository`(`JpaRepository<Item, Long>`)、
  `ItemService`、`ItemController`、`CreateItemRequest`、`ItemResponse`
- `GlobalExceptionHandler`（バリデーション400 / 500 を集約）
- Spring Security + JWT リソースサーバ（`/auth/login` 以外は**認証必須**）
- `ItemService` は既に `@Transactional` を使用（`create` は
  `rollbackFor=Exception.class`、`findAll` は `readOnly=true`）
- `spring.jpa.hibernate.ddl-auto=update`

### 現状の `Item` と NestJS 版の差分

| 項目        | 現状(Spring)                  | NestJS版             | 対応                         |
|------------|------------------------------|---------------------|------------------------------|
| id         | `Long` / `IDENTITY`          | `String` / UUID     | **現状維持（Long）** ※Spring都合を優先 |
| name       | `@NotBlank`（長さ制限なし）    | 空不可 + 最大40      | `@Size(max=40)` 追加、列は255 |
| price      | `@Min(0)`                    | 最小1                | **`@Min(1)`へ変更**          |
| description| `@NotBlank`（必須）           | 任意(NULL可)/最大1000| **任意化** + `@Size(max=1000)`|
| status     | なし                         | enum(既定ON_SALE)    | **追加**                     |
| createdAt  | なし                         | 自動                 | **追加**（`@CreationTimestamp`）|
| updatedAt  | なし                         | 自動                 | **追加**（`@UpdateTimestamp`）|

### 現状の API 差分

- 実装済: `POST /items`, `GET /items`
- **未実装: `GET /items/:id`, `PUT /items/:id`(SOLD_OUT), `DELETE /items/:id`**
- `ItemResponse` に `status` / `createdAt` / `updatedAt` が無い（Prisma は
  Item 全体を返すため要追加）
- 404「商品が存在しません」に相当する例外・ハンドラが無い

---

## 4. Prisma → Hibernate マッピング方針

| Prisma                                | Hibernate / JPA                                              |
|---------------------------------------|-------------------------------------------------------------|
| `id String @default(uuid()) @db.Uuid` | **【決定】UUID化せず `Long id` + `@GeneratedValue(strategy=IDENTITY)` を維持**（Spring Boot の都合を優先。DB の id 型移行が不要） |
| `name @db.VarChar(255)`               | `@Column(length = 255)`                                      |
| `price Int`                           | `Integer` / `INTEGER`                                        |
| `description String? @db.Text`        | `@Column(columnDefinition="text")`、nullable                |
| `status ItemStatus @default(ON_SALE)` | `enum ItemStatus` + `@Enumerated(EnumType.STRING)`（下記注意） |
| `createdAt @default(now())`           | `@CreationTimestamp`                                         |
| `updatedAt @updatedAt`                | `@UpdateTimestamp`                                           |

### enum マッピングの注意
- Prisma はネイティブの PostgreSQL enum 型（`CREATE TYPE "ItemStatus"`）を使う。
- Hibernate 側は **`@Enumerated(EnumType.STRING)` で `VARCHAR` に保存するのが最も簡単**。
  DB 上の物理型は Prisma と厳密一致しない（PG enum vs varchar）が、
  アプリの振る舞い（ON_SALE/SOLD_OUT）は同一にできる。
- 物理型まで一致させたい場合は Hibernate の PostgreSQL enum カスタムマッピングが
  必要（実装コスト増）。**推奨は varchar 方式**。

### トランザクション方針（今回の主眼）
- `create` / `updateStatus` / `delete`（更新系）→ `@Transactional(rollbackFor = Exception.class)`
- `findAll` / `findById`（参照系）→ `@Transactional(readOnly = true)`
- これで「元は未考慮だったトランザクション」を移植先で明示的に担保できる。

---

## 5. 論点・決定事項

1. **ID を UUID にするか Long のままか** → 【決定】**`Long` IDENTITY を維持**
   - Spring Boot / JPA の都合を優先。UUID化しないため、既存 `item` テーブルの
     id 型移行（`bigint`→`uuid`）は**不要**。
   - `GET/PUT/DELETE /items/{id}` の `id` は `Long`（`@PathVariable Long id`）で受ける。
2. **認証の扱い** → 【決定】**いったん認証なしで全部動くようにする**
   - `/items` を公開するため `SecurityPaths.PUBLIC_ENDPOINTS` に `/items/**` を追加する。
   - **既存のセキュリティ関連コード（`SecurityConfig` / JWT リソースサーバ設定 /
     `AuthController` 等）は削除せずそのまま残す。**
   - なぜ公開にするか・元は認証必須だった旨を**コメントで説明**する
     （後で認証を戻せるように、意図を明記）。
   - 動作確認は JWT 不要で全エンドポイントを叩ける状態にする。
3. **enum の物理型** → 【決定】varchar 方式（`@Enumerated(EnumType.STRING)`）で進める（4章参照）。
4. **404 メッセージ / エラー形式** → 【決定】移植先の `ErrorResponse` 形式に合わせる
   （`code="NOT_FOUND"`, `message="商品が存在しません"`）。
5. **不正IDのハンドリング**
   - NestJS は `ParseUUIDPipe` で400。移植先は `@PathVariable Long` で自動変換し、
     数値でない場合は `MethodArgumentTypeMismatchException` → 400 にハンドラ追加。

---

## 6. リスク / 留意点

- ID は `Long` 維持のため id 型移行は不要。ただし `status`（NOT NULL）を追加するため、
  **既存 `item` テーブルに行が残っていると `ddl-auto=update` の列追加で既存行が
  NULL になり整合しない**可能性がある。開発DBのため、`item` テーブルを空にするか
  再作成してから起動するのが無難。将来的には Flyway/Liquibase 等のマイグレーション
  導入が望ましい（今回のスコープ外）。
- `open-in-view=false` のため、Lazy 参照は Service 層(トランザクション内)で解決が必要。
  今回の Item は関連を持たないため影響なし。
- タイムゾーン: `TIMESTAMP(0)`（秒精度・TZなし）。`LocalDateTime` で受ける想定。
- **認証は一時的に無効化（`/items` 公開）**。本番相当に戻す際は `/items/**` の公開設定を
  外すだけで済むよう、コメントで戻し方を明記しておく。

---

## 7. 移植後の想定ファイル構成（`com.example.springlab.item`）

- `Item.java`（改修: **id は Long 維持**, status/createdAt/updatedAt 追加, 制約調整）
- `ItemStatus.java`（新規: enum ON_SALE/SOLD_OUT）
- `ItemRepository.java`（現状維持: `JpaRepository<Item, Long>`）
- `ItemService.java`（改修: findById/updateStatus/delete 追加, 全メソッド `@Transactional`）
- `ItemController.java`（改修: GET/:id, PUT/:id, DELETE/:id 追加, `id` は `Long`）
- `CreateItemRequest.java`（改修: name最大40, price最小1, description任意・最大1000）
- `ItemResponse.java`（改修: status/createdAt/updatedAt 追加）
- `error/`（改修: NotFound用の例外 + ハンドラ、型不一致400ハンドラ）
- `security/SecurityPaths.java`（改修: `/items/**` を公開に追加。**コメントで一時対応の旨を明記**）
- ※ `SecurityConfig` / `AuthController` など既存セキュリティ資産は**そのまま残す**
</content>
</invoke>
