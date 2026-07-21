# 詳細設計

- [1. 詳細設計方針](#1-詳細設計方針)
- [2. 共通処理設計](#2-共通処理設計)
  - [2.1 認証ユーザー取得](#21-認証ユーザー取得)
  - [2.2 ロール認可](#22-ロール認可)
  - [2.3 店舗単位アクセス制御](#23-店舗単位アクセス制御)
  - [2.4 共通例外ハンドリング](#24-共通例外ハンドリング)
  - [2.5 トランザクション方針](#25-トランザクション方針)
  - [2.6 楽観ロック方針](#26-楽観ロック方針)
  - [2.7 ログ出力方針](#27-ログ出力方針)
- [3. 主要業務処理詳細](#3-主要業務処理詳細)
  - [3.1 テーブル利用開始](#31-テーブル利用開始)
    - [処理手順](#処理手順)
    - [更新テーブル](#更新テーブル)
    - [主なエラー](#主なエラー)
  - [3.2 注文登録](#32-注文登録)
    - [処理手順](#処理手順-1)
    - [異常注文の扱い](#異常注文の扱い)
    - [更新テーブル](#更新テーブル-1)
    - [主なエラー](#主なエラー-1)
  - [3.3 注文ステータス更新](#33-注文ステータス更新)
    - [処理手順](#処理手順-2)
    - [状態遷移方針](#状態遷移方針)
    - [更新テーブル](#更新テーブル-2)
    - [主なエラー](#主なエラー-2)
  - [3.4 調理ステータス更新](#34-調理ステータス更新)
    - [処理手順](#処理手順-3)
    - [状態遷移方針](#状態遷移方針-1)
    - [更新テーブル](#更新テーブル-3)
    - [主なエラー](#主なエラー-3)
  - [3.5 会計金額確認](#35-会計金額確認)
    - [処理手順](#処理手順-4)
    - [参照テーブル](#参照テーブル)
    - [主なエラー](#主なエラー-4)
  - [3.6 会計完了](#36-会計完了)
    - [処理手順](#処理手順-5)
    - [更新テーブル](#更新テーブル-4)
    - [主なエラー](#主なエラー-5)
  - [3.7 メニュー登録](#37-メニュー登録)
    - [処理手順](#処理手順-6)
    - [更新テーブル](#更新テーブル-5)
    - [主なエラー](#主なエラー-6)
  - [3.8 メニュー更新](#38-メニュー更新)
    - [処理手順](#処理手順-7)
    - [更新テーブル](#更新テーブル-6)
    - [主なエラー](#主なエラー-7)
  - [3.9 メニュー販売ステータス変更](#39-メニュー販売ステータス変更)
    - [処理手順](#処理手順-8)
    - [更新テーブル](#更新テーブル-7)
    - [主なエラー](#主なエラー-8)
  - [3.10 テーブルステータス変更](#310-テーブルステータス変更)
    - [状態遷移方針](#状態遷移方針-2)
    - [処理手順](#処理手順-9)
    - [更新テーブル](#更新テーブル-8)
    - [主なエラー](#主なエラー-9)

## 1. 詳細設計方針

本書では、API設計で定義した主要APIについて、サービス層で実施する認証・認可、入力チェック、状態チェック、DB更新、トランザクション境界、例外変換の方針を定義する。

Controller は HTTPリクエストの受け取り、入力DTOへの変換、レスポンスDTOの返却を担当する。
Service は業務ルール、状態遷移、トランザクション制御を担当する。
Mapper はDBアクセスのみを担当し、業務判断は持たない。

来店客向け注文画面と業務管理画面はフロントエンド上は2系統とするが、API側では必ずサーバー側で注文可否、ロール認可、店舗単位アクセス制御を判定する。
画面の表示制御やURLだけを信頼して、APIの認可処理を省略してはならない。

主要な更新処理では、DB設計の version カラムを用いた楽観ロックを行う。
状態遷移を伴う処理では、更新前の現在状態を必ず取得し、許可された状態からの変更であることを確認してから更新する。

## 2. 共通処理設計

### 2.1 認証ユーザー取得

店舗業務API、本部管理API、バッチ管理APIでは、Spring Security により検証済みの Cognito JWT から認証ユーザー情報を取得する。
来店客向けAPIでは Cognito 認証を行わず、qrToken、visitToken、テーブル状態、来店セッション状態を組み合わせて注文可否を判定する。

| No | 処理 | 内容 |
| --- | --- | --- |
| 1 | JWT検証 | 署名、issuer、有効期限、token_use、client_id を検証する |
| 2 | subject取得 | JWT の subject から Cognito User ID を取得する |
| 3 | users取得 | users.cognito_user_id と subject を紐づけて業務ユーザーを取得する |
| 4 | ユーザー状態確認 | users.deleted_at が NULL であることを確認する |
| 5 | 認証コンテキスト生成 | userId、role、storeId、username を後続処理で参照できる形にする |

JWT自体が不正、期限切れ、issuer不正、署名不正、token_use不正、client_id不一致の場合は `401 Unauthorized` とする。
JWTは有効だが users に該当ユーザーが存在しない、または users.deleted_at が NULL ではない場合は `403 Forbidden` とする。

### 2.2 ロール認可

APIごとに必要ロールを定義し、認証コンテキストの role が必要ロールを満たすことを確認する。

| 必要ロール | 許可するロール |
| --- | --- |
| STORE_STAFF | STORE_STAFF, STORE_MANAGER, ADMIN |
| STORE_MANAGER | STORE_MANAGER, ADMIN |
| ADMIN | ADMIN |

必要ロールを満たさない場合は `403 Forbidden` とする。
ロール認可は Controller のアノテーションだけに依存せず、店舗単位アクセス制御や子リソース検証と合わせて Service 層でも業務整合性を確認する。

### 2.3 店舗単位アクセス制御

店舗業務APIでは、URL の `storeId` と認証ユーザーの users.store_id が一致することを確認する。
ADMIN は全店舗データにアクセス可能なため、原則として storeId 一致チェックの制限を受けない。

子リソースIDを指定するAPIでは、URL の `storeId` だけでは店舗制御として不十分である。
対象データをDBから取得したうえで、実データの store_id が URL の `storeId` と一致することを確認する。

| 対象ID | 店舗確認方法 |
| --- | --- |
| tableId | restaurant_tables.store_id を確認する |
| menuId | menus.store_id を確認する |
| orderId | orders.store_id を確認する |
| orderItemId | order_items -> orders -> store_id を辿って確認する |
| paymentId | payments.store_id を確認する |
| visitSessionId | visit_sessions.store_id を確認する |

URL の `storeId` と対象データの store_id が一致しない場合、または STORE_STAFF / STORE_MANAGER が自店舗外データへアクセスした場合は `403 Forbidden` とする。

### 2.4 共通例外ハンドリング

業務例外は共通のエラーレスポンス形式に変換する。

```json
{
  "error": "ERROR_CODE",
  "message": "エラーメッセージ"
}
```

| 例外分類 | HTTPステータス | エラーコード例 | 内容 |
| --- | --- | --- | --- |
| 入力値不正 | 400 | VALIDATION_ERROR | 必須不足、形式不正、範囲外 |
| 未認証 | 401 | UNAUTHORIZED | JWTなし、JWT不正、期限切れ |
| 認可失敗 | 403 | FORBIDDEN | ロール不足、自店舗外アクセス |
| 対象なし | 404 | NOT_FOUND | 対象店舗、テーブル、メニュー、注文などが存在しない |
| 状態不整合 | 409 | CONFLICT | 注文不可状態、会計済み、販売不可など |
| 楽観ロック | 409 | VERSION_CONFLICT | version が一致しない |
| 想定外エラー | 500 | INTERNAL_SERVER_ERROR | DB障害、未分類例外 |

入力チェックは Bean Validation と Service 層の業務チェックを組み合わせて行う。
Bean Validation では型、必須、桁数、範囲を確認し、Service 層では店舗整合性、状態遷移、販売可否、金額整合性などを確認する。

### 2.5 トランザクション方針

参照系処理は原則として読み取り専用トランザクションとする。
更新系処理は Service メソッド単位でトランザクションを開始し、関連テーブルの更新を1つのトランザクションで完了させる。

| 処理 | 同一トランザクションで行う操作 |
| --- | --- |
| テーブル利用開始 | restaurant_tables 更新、visit_sessions 作成 |
| テーブルステータス変更 | restaurant_tables 更新、必要に応じた visit_sessions 確認 |
| 注文登録 | orders 作成、order_items 作成、注文金額計算 |
| 会計完了 | payments 作成、visit_sessions 更新、restaurant_tables 更新 |
| メニュー更新 | menus 更新、カテゴリ店舗整合性確認 |

途中で業務例外またはシステム例外が発生した場合はロールバックする。
外部サービス呼び出しを伴う場合は、DBトランザクションの中に長時間の外部I/Oを含めない。

### 2.6 楽観ロック方針

更新対象テーブルに version カラムが存在する場合は、リクエストの version とDBの version が一致することを確認する。
更新時は `WHERE id = ? AND version = ?` の条件で更新し、更新件数が0件の場合は楽観ロックエラーとする。

| 対象処理 | 対象テーブル |
| --- | --- |
| テーブル利用開始 | restaurant_tables |
| テーブルステータス変更 | restaurant_tables |
| 注文ステータス更新 | orders |
| 調理ステータス更新 | order_items |
| 会計完了 | visit_sessions, restaurant_tables |
| メニュー更新 | menus |
| メニュー販売ステータス変更 | menus |

楽観ロックエラー時は `409 Conflict`、エラーコードは `VERSION_CONFLICT` とする。

### 2.7 ログ出力方針

認証失敗、認可失敗、状態不整合、異常注文検知、会計処理、管理操作、バッチ実行はログ出力対象とする。

| 項目 | 内容 |
| --- | --- |
| userId | 業務ユーザーID。来店客APIでは出力しない |
| role | 業務ロール |
| storeId | 対象店舗ID |
| requestPath | リクエストパス |
| action | 業務操作名 |
| result | SUCCESS / FAILURE |
| errorCode | エラーコード |

JWTアクセストークン、visitToken、qrToken は値そのものをログに出力しない。
調査上必要な場合は、ハッシュ化した値または末尾数文字のみを出力する。
来店客APIでは必要に応じて storeId、tableId、visitSessionId、orderId などの内部IDを出力する。

## 3. 主要業務処理詳細

### 3.1 テーブル利用開始

対象APIは `API-S-003 テーブル利用開始` とする。
空席テーブルを利用中に変更し、来店セッションと visitToken を発行する。

| 項目 | 内容 |
| --- | --- |
| 入力 | storeId, tableId, restaurant_tables.version |
| 必要ロール | STORE_STAFF |
| 正常時ステータス | 201 Created |
| トランザクション | 必須 |

#### 処理手順

| No | 処理 | 内容 |
| --- | --- | --- |
| 1 | 認証・認可 | JWTを検証し、STORE_STAFF 以上であることを確認する |
| 2 | 店舗制御 | STORE_STAFF / STORE_MANAGER の場合、users.store_id と URL の storeId が一致することを確認する |
| 3 | テーブル取得 | restaurant_tables を tableId で取得する |
| 4 | 店舗整合性確認 | restaurant_tables.store_id が URL の storeId と一致することを確認する |
| 5 | 状態確認 | restaurant_tables.status が AVAILABLE であることを確認する |
| 6 | ACTIVEセッション確認 | 対象テーブルに ACTIVE な visit_sessions が存在しないことを確認する |
| 7 | visitToken発行 | 推測困難なランダム値として visitToken を発行する |
| 8 | テーブル更新 | tableId とリクエスト version を条件に restaurant_tables.status を OCCUPIED に更新し、version をインクリメントする |
| 9 | 来店セッション作成 | visit_sessions を ACTIVE として作成し、store_id、table_id、visit_token、started_at、expires_at を設定する |
| 10 | レスポンス生成 | テーブル情報と currentVisitSession を返却する |

restaurant_tables 更新時は、tableId とリクエスト version を条件に含めて更新する。
更新件数が0件の場合は、楽観ロック競合として `409 Conflict`、`VERSION_CONFLICT` を返す。
同時に複数ユーザーが同じテーブルの利用開始を実行した場合、最初に更新に成功した1件のみを有効とし、後続の更新は楽観ロック競合または状態不整合として失敗させる。

#### 更新テーブル

| テーブル | 操作 | 主な更新内容 |
| --- | --- | --- |
| restaurant_tables | UPDATE | status=OCCUPIED, updated_at, updated_by, version+1 |
| visit_sessions | INSERT | status=ACTIVE, visit_token, store_id, table_id, started_at, expires_at |

#### 主なエラー

| 条件 | HTTPステータス | エラーコード |
| --- | --- | --- |
| 対象テーブルが存在しない | 404 | NOT_FOUND |
| 対象テーブルが自店舗外 | 403 | FORBIDDEN |
| テーブルが AVAILABLE ではない | 409 | TABLE_NOT_AVAILABLE |
| ACTIVE な来店セッションが存在する | 409 | ACTIVE_VISIT_SESSION_EXISTS |
| version が一致しない | 409 | VERSION_CONFLICT |

### 3.2 注文登録

対象APIは `API-C-004 注文登録` と `API-S-011 注文登録` とする。
来店客注文とスタッフ口頭注文は認証方式が異なるが、注文作成、明細作成、金額計算、販売可否確認の基本処理は共通化する。

来店客注文の場合は Cognito 認証を行わず、qrToken と visitToken の整合性により注文可否を判定する。
スタッフ注文の場合は JWT 認証、STORE_STAFF 以上のロール認可、URL の storeId と認証ユーザーの所属店舗IDの照合を行う。
スタッフ注文であっても、restaurant_tables.status が OCCUPIED ではない場合は注文登録を許可しない。
PAYMENT_WAITING、CLEANING、AVAILABLE のテーブルには注文登録できない。

| 区分 | 入力 |
| --- | --- |
| 来店客注文 | qrToken, visitToken, items[].menuId, items[].quantity |
| スタッフ注文 | storeId, tableId, items[].menuId, items[].quantity, 認証ユーザー |

#### 処理手順

| No | 処理 | 内容 |
| --- | --- | --- |
| 1 | 入力チェック | items が1件以上であること、quantity が1以上であること、明細数が許容上限以内であること、同一リクエスト内で menuId が重複していないことを確認する |
| 2 | 注文元判定 | 来店客注文は qrToken / visitToken、スタッフ注文は storeId / tableId とJWTで制御する |
| 3 | テーブル取得 | restaurant_tables を取得し、存在と店舗整合性を確認する |
| 4 | 来店セッション取得 | 対象テーブルの ACTIVE な visit_sessions を取得する |
| 5 | 注文可否確認 | restaurant_tables.status が OCCUPIED、visit_sessions.status が ACTIVE、expires_at が現在時刻より後であることを確認する |
| 6 | メニュー取得 | items[].menuId の menus を取得し、すべて対象店舗に所属することを確認する |
| 7 | 販売可否確認 | menus.status が AVAILABLE であり、deleted_at が NULL であることを確認する |
| 8 | 金額計算 | 注文時点の menus.price を order_items.unit_price として採用し、小計と合計金額を計算する |
| 9 | 異常注文判定 | 警告閾値を超える高額・大量注文は異常注文として検知し、処理可能な注文は登録したうえでWARN ログを出力する。 |
| 10 | 注文作成 | orders を ORDERED として作成する |
| 11 | 注文明細作成 | order_items を WAITING として作成する |
| 12 | レスポンス生成 | orderId、totalAmount、orderItems を返却する |

#### 異常注文の扱い

| 区分 | 扱い |
| --- | --- |
| 数量が0以下 | 400 Bad Request |
| 明細数が許容上限を超える | 400 Bad Request |
| システム上限金額を超える | 400 Bad Request または 409 Conflict |
| 販売不可メニューを含む | 409 Conflict |
| 業務上成立し得る大量・高額注文 | 注文登録し、WARNログを出力する |

同一リクエスト内で menuId が重複している場合は、入力値不正として `400 Bad Request` を返す。
orders 作成と order_items 作成は同一トランザクションで実行する。
order_items 作成途中で例外が発生した場合、orders の作成もロールバックする。
業務管理画面での警告表示は、将来拡張または詳細設計時に検討する。

#### 更新テーブル

| テーブル | 操作 | 主な更新内容 |
| --- | --- | --- |
| orders | INSERT | store_id, table_id, visit_session_id, status=ORDERED, total_amount |
| order_items | INSERT | order_id, menu_id, quantity, unit_price, subtotal_amount, cooking_status=WAITING |

#### 主なエラー

| 条件 | HTTPステータス | エラーコード |
| --- | --- | --- |
| 入力値不正 | 400 | VALIDATION_ERROR |
| 同一リクエスト内で menuId が重複している | 400 | VALIDATION_ERROR |
| qrToken / visitToken 不整合 | 403 | FORBIDDEN |
| 対象テーブルなし | 404 | NOT_FOUND |
| テーブルが OCCUPIED ではない | 409 | TABLE_NOT_ORDERABLE |
| visit_sessions.status が ACTIVE ではない | 409 | VISIT_SESSION_NOT_ACTIVE |
| visit_sessions.expires_at が期限切れ | 409 | VISIT_SESSION_EXPIRED |
| メニューが販売可能ではない | 409 | MENU_NOT_AVAILABLE |

### 3.3 注文ステータス更新

対象APIは `API-S-013 注文ステータス変更` とする。
注文全体のステータスを更新し、注文キャンセルや完了を管理する。
orders.status は注文受付単位の有効状態を表す。
調理・提供状況は order_items.cooking_status で管理する。
COMPLETED は注文全体の業務処理が完了した状態を表し、個別明細の調理状態とは別に扱う。

| 項目 | 内容 |
| --- | --- |
| 入力 | storeId, orderId, status, orders.version |
| 必要ロール | STORE_STAFF |
| 正常時ステータス | 200 OK |

#### 処理手順

| No | 処理 | 内容 |
| --- | --- | --- |
| 1 | 認証・認可 | STORE_STAFF 以上であることを確認する |
| 2 | 注文取得 | orders を orderId で取得する |
| 3 | 店舗制御 | orders.store_id が URL の storeId と一致し、認証ユーザーの店舗権限を満たすことを確認する |
| 4 | 入力確認 | 変更後 status が許可値であることを確認する |
| 5 | 状態遷移確認 | 現在ステータスから変更可能な遷移であることを確認する |
| 6 | 楽観ロック確認 | リクエスト version と orders.version が一致することを確認する |
| 7 | 注文更新 | orders.status を更新し、version をインクリメントする |
| 8 | レスポンス生成 | 更新後の注文詳細を返却する |

#### 状態遷移方針

| 現在状態 | 変更可能な状態 |
| --- | --- |
| ORDERED | COMPLETED, CANCELLED |
| COMPLETED | なし |
| CANCELLED | なし |

COMPLETED または CANCELLED の注文は確定済みとして扱い、原則として再変更しない。

#### 更新テーブル

| テーブル | 操作 | 主な更新内容 |
| --- | --- | --- |
| orders | UPDATE | status, updated_at, updated_by, version+1 |

#### 主なエラー

| 条件 | HTTPステータス | エラーコード |
| --- | --- | --- |
| 対象注文が存在しない | 404 | NOT_FOUND |
| 自店舗外の注文 | 403 | FORBIDDEN |
| 不正なステータス | 400 | VALIDATION_ERROR |
| 許可されない状態遷移 | 409 | INVALID_STATUS_TRANSITION |
| version が一致しない | 409 | VERSION_CONFLICT |

### 3.4 調理ステータス更新

対象APIは `API-S-012 注文明細調理ステータス更新` とする。
注文明細単位で厨房・提供状況を管理する。

| 項目 | 内容 |
| --- | --- |
| 入力 | storeId, orderItemId, cookingStatus, order_items.version |
| 必要ロール | STORE_STAFF |
| 正常時ステータス | 200 OK |

#### 処理手順

| No | 処理 | 内容 |
| --- | --- | --- |
| 1 | 認証・認可 | STORE_STAFF 以上であることを確認する |
| 2 | 注文明細取得 | order_items と親 orders を取得する |
| 3 | 店舗制御 | orders.store_id が URL の storeId と一致することを確認する |
| 4 | 入力確認 | cookingStatus が WAITING / COOKING / READY / SERVED / CANCELLED のいずれかであることを確認する |
| 5 | 状態遷移確認 | 現在の cooking_status から変更可能であることを確認する |
| 6 | 楽観ロック確認 | リクエスト version と order_items.version が一致することを確認する |
| 7 | 明細更新 | order_items.cooking_status を更新し、version をインクリメントする |
| 8 | レスポンス生成 | 更新後の orderItemId、orderId、cookingStatus、version を返却する |

#### 状態遷移方針

| 現在状態 | 変更可能な状態 |
| --- | --- |
| WAITING | COOKING, CANCELLED |
| COOKING | READY, CANCELLED |
| READY | SERVED, CANCELLED |
| SERVED | なし |
| CANCELLED | なし |

#### 更新テーブル

| テーブル | 操作 | 主な更新内容 |
| --- | --- | --- |
| order_items | UPDATE | cooking_status, updated_at, updated_by, version+1 |

#### 主なエラー

| 条件 | HTTPステータス | エラーコード |
| --- | --- | --- |
| 対象注文明細が存在しない | 404 | NOT_FOUND |
| 自店舗外の注文明細 | 403 | FORBIDDEN |
| 不正な調理ステータス | 400 | VALIDATION_ERROR |
| 許可されない状態遷移 | 409 | INVALID_STATUS_TRANSITION |
| version が一致しない | 409 | VERSION_CONFLICT |

### 3.5 会計金額確認

対象APIは `API-S-014 会計金額確認` とする。
指定来店セッションに紐づく CANCELLED 以外の注文合計金額を算出し、会計予定金額を返却する。

| 項目 | 内容 |
| --- | --- |
| 入力 | storeId, visitSessionId |
| 必要ロール | STORE_STAFF |
| 正常時ステータス | 200 OK |

#### 処理手順

| No | 処理 | 内容 |
| --- | --- | --- |
| 1 | 認証・認可 | STORE_STAFF 以上であることを確認する |
| 2 | 来店セッション取得 | visit_sessions を visitSessionId で取得する |
| 3 | 店舗制御 | visit_sessions.store_id が URL の storeId と一致することを確認する |
| 4 | テーブル取得 | visit_sessions.table_id に紐づく restaurant_tables を取得する |
| 5 | 会計状態確認 | 既に PAID の payment が存在する場合は、支払い済みとして扱う |
| 6 | 金額集計 | visitSessionId に紐づく CANCELLED 以外の orders.total_amount を合計する |
| 7 | レスポンス生成 | visitSessionId、storeId、tableId、tableNumber、amount、paymentStatus を返却する |

既に PAID の payment が存在する場合でも、会計金額確認APIでは `200 OK` を返し、paymentStatus=PAID として返却する。
ただし、会計完了APIでは二重会計防止のため `409 Conflict` を返す。

#### 参照テーブル

| テーブル | 用途 |
| --- | --- |
| visit_sessions | 来店セッション、店舗、テーブル確認 |
| restaurant_tables | テーブル番号取得 |
| orders | 会計対象金額集計 |
| payments | 支払い状態確認 |

#### 主なエラー

| 条件 | HTTPステータス | エラーコード |
| --- | --- | --- |
| 来店セッションが存在しない | 404 | NOT_FOUND |
| 自店舗外の来店セッション | 403 | FORBIDDEN |

### 3.6 会計完了

対象APIは `API-S-015 会計処理` とする。
来店セッション単位で会計を完了し、支払い情報、来店セッション、テーブル状態を同一トランザクションで更新する。

| 項目 | 内容 |
| --- | --- |
| 入力 | storeId, visitSessionId, amount, visitSessionVersion, tableVersion |
| 必要ロール | STORE_STAFF |
| 正常時ステータス | 200 OK |
| トランザクション | 必須 |

#### 処理手順

| No | 処理 | 内容 |
| --- | --- | --- |
| 1 | 認証・認可 | STORE_STAFF 以上であることを確認する |
| 2 | 来店セッション取得 | visit_sessions を visitSessionId で取得する |
| 3 | 店舗制御 | visit_sessions.store_id が URL の storeId と一致することを確認する |
| 4 | テーブル取得 | visit_sessions.table_id の restaurant_tables を取得する |
| 5 | 状態確認 | visit_sessions.status が ACTIVE、かつ restaurant_tables.status が PAYMENT_WAITING であることを確認する |
| 6 | 二重会計確認 | 対象 visitSessionId に PAID の payments が存在しないことを確認する |
| 7 | 金額集計 | CANCELLED 以外の orders.total_amount を合計する |
| 8 | 金額決定・照合 | サーバー側の再集計金額を最終的な支払い金額とし、リクエスト amount と照合する |
| 9 | 楽観ロック確認 | visitSessionVersion、tableVersion が一致することを確認する |
| 10 | payment作成 | payments を PAID として登録する |
| 11 | 来店セッション更新 | visit_sessions.status を CLOSED、ended_at を現在時刻に更新する |
| 12 | テーブル更新 | restaurant_tables.status を CLEANING に更新する |
| 13 | レスポンス生成 | paymentId、amount、status、paidAt を返却する |

会計完了は、visit_sessions.status = ACTIVE かつ restaurant_tables.status = PAYMENT_WAITING の場合のみ許可する。
OCCUPIED のまま会計完了できると、会計処理中に追加注文が入る可能性があるため、原則として会計前に restaurant_tables.status を PAYMENT_WAITING に変更する。
会計完了時は restaurant_tables.status を CLEANING に更新する。
清掃完了後、API-S-004 テーブルステータス変更により CLEANING から AVAILABLE へ変更する。

リクエストの amount は会計画面で表示された金額との照合用として扱う。
最終的な支払い金額は、サーバー側で visitSessionId に紐づく CANCELLED 以外の orders.total_amount を再集計して決定する。

visit_sessions 更新時は visitSessionId と visitSessionVersion を条件に含める。
restaurant_tables 更新時は tableId と tableVersion を条件に含める。
いずれかの更新件数が0件の場合は `409 Conflict`、`VERSION_CONFLICT` とする。

#### 更新テーブル

| テーブル | 操作 | 主な更新内容 |
| --- | --- | --- |
| payments | INSERT | store_id, visit_session_id, amount, status=PAID, paid_at |
| visit_sessions | UPDATE | status=CLOSED, ended_at, updated_at, version+1 |
| restaurant_tables | UPDATE | status=CLEANING, updated_at, updated_by, version+1 |

#### 主なエラー

| 条件 | HTTPステータス | エラーコード |
| --- | --- | --- |
| 来店セッションが存在しない | 404 | NOT_FOUND |
| 自店舗外の来店セッション | 403 | FORBIDDEN |
| 既に会計済み | 409 | PAYMENT_ALREADY_COMPLETED |
| 会計対象注文が存在しない | 409 | NO_PAYABLE_ORDER |
| visit_sessions.status が ACTIVE ではない | 409 | CONFLICT |
| restaurant_tables.status が PAYMENT_WAITING ではない | 409 | CONFLICT |
| 金額が一致しない | 409 | PAYMENT_AMOUNT_MISMATCH |
| version が一致しない | 409 | VERSION_CONFLICT |

### 3.7 メニュー登録

対象APIは `API-M-003 自店舗メニュー登録` とする。
店長が自店舗のメニューを登録する。

| 項目 | 内容 |
| --- | --- |
| 入力 | storeId, menuName, categoryId, price, description, imageUrl, status |
| 必要ロール | STORE_MANAGER |
| 正常時ステータス | 201 Created |

#### 処理手順

| No | 処理 | 内容 |
| --- | --- | --- |
| 1 | 認証・認可 | STORE_MANAGER 以上であることを確認する |
| 2 | 店舗制御 | STORE_MANAGER の場合、users.store_id と URL の storeId が一致することを確認する |
| 3 | 入力チェック | menuName、categoryId、price、status の妥当性を確認する |
| 4 | カテゴリ取得 | menu_categories を categoryId で取得する |
| 5 | カテゴリ店舗確認 | menu_categories.store_id が URL の storeId と一致することを確認する |
| 6 | 重複確認 | 同一店舗・同一カテゴリ内に同名メニューが存在しないことを確認する |
| 7 | メニュー作成 | DTOの price を menus.price に保存する。status 未指定時は AVAILABLE とする |
| 8 | レスポンス生成 | menuId、menuName、categoryName、price、status、version を返却する |

#### 更新テーブル

| テーブル | 操作 | 主な更新内容 |
| --- | --- | --- |
| menus | INSERT | store_id, category_id, name, description, price, image_url, status, version=0 |

#### 主なエラー

| 条件 | HTTPステータス | エラーコード |
| --- | --- | --- |
| 入力値不正 | 400 | VALIDATION_ERROR |
| 必要ロール不足 | 403 | FORBIDDEN |
| カテゴリが存在しない | 404 | NOT_FOUND |
| カテゴリが自店舗外 | 403 | FORBIDDEN |
| 同一カテゴリ内に同名メニューが存在する | 409 | MENU_ALREADY_EXISTS |

### 3.8 メニュー更新

対象APIは `API-M-004 自店舗メニュー更新` とする。
店長が自店舗の既存メニュー情報を更新する。

| 項目 | 内容 |
| --- | --- |
| 入力 | storeId, menuId, menuName, categoryId, price, description, imageUrl, status, version |
| 必要ロール | STORE_MANAGER |
| 正常時ステータス | 200 OK |

#### 処理手順

| No | 処理 | 内容 |
| --- | --- | --- |
| 1 | 認証・認可 | STORE_MANAGER 以上であることを確認する |
| 2 | メニュー取得 | menus を menuId で取得し、deleted_at が NULL であることを確認する |
| 3 | 店舗制御 | menus.store_id が URL の storeId と一致することを確認する |
| 4 | 入力チェック | menuName、categoryId、price、status、version の妥当性を確認する |
| 5 | カテゴリ取得 | menu_categories を categoryId で取得する |
| 6 | カテゴリ店舗確認 | menu_categories.store_id が URL の storeId と一致することを確認する |
| 7 | 重複確認 | 同一店舗・同一カテゴリ内に、自分以外の同名メニューが存在しないことを確認する |
| 8 | 楽観ロック確認 | リクエスト version と menus.version が一致することを確認する |
| 9 | メニュー更新 | DTOの price を menus.price に保存し、version をインクリメントする |
| 10 | レスポンス生成 | 更新後のメニュー情報を返却する |

#### 更新テーブル

| テーブル | 操作 | 主な更新内容 |
| --- | --- | --- |
| menus | UPDATE | category_id, name, description, price, image_url, status, updated_at, updated_by, version+1 |

#### 主なエラー

| 条件 | HTTPステータス | エラーコード |
| --- | --- | --- |
| メニューが存在しない | 404 | NOT_FOUND |
| メニューが自店舗外 | 403 | FORBIDDEN |
| カテゴリが存在しない | 404 | NOT_FOUND |
| カテゴリが自店舗外 | 403 | FORBIDDEN |
| 同一カテゴリ内に自分以外の同名メニューが存在する | 409 | MENU_ALREADY_EXISTS |
| version が一致しない | 409 | VERSION_CONFLICT |

### 3.9 メニュー販売ステータス変更

対象APIは `API-S-008 自店舗メニュー販売ステータス変更` とする。
日次運用で売り切れや一時停止を反映するため、メニューの販売ステータスを変更する。

| 項目 | 内容 |
| --- | --- |
| 入力 | storeId, menuId, status, version |
| 必要ロール | STORE_STAFF |
| 正常時ステータス | 200 OK |

#### 処理手順

| No | 処理 | 内容 |
| --- | --- | --- |
| 1 | 認証・認可 | STORE_STAFF 以上であることを確認する |
| 2 | メニュー取得 | menus を menuId で取得し、deleted_at が NULL であることを確認する |
| 3 | 店舗制御 | menus.store_id が URL の storeId と一致することを確認する |
| 4 | 入力チェック | status が AVAILABLE / SOLD_OUT / SUSPENDED のいずれかであることを確認する |
| 5 | 楽観ロック確認 | リクエスト version と menus.version が一致することを確認する |
| 6 | ステータス更新 | menus.status を更新し、version をインクリメントする |
| 7 | レスポンス生成 | 更新後のメニュー情報を返却する |

#### 更新テーブル

| テーブル | 操作 | 主な更新内容 |
| --- | --- | --- |
| menus | UPDATE | status, updated_at, updated_by, version+1 |

#### 主なエラー

| 条件 | HTTPステータス | エラーコード |
| --- | --- | --- |
| 入力値不正 | 400 | VALIDATION_ERROR |
| メニューが存在しない | 404 | NOT_FOUND |
| メニューが自店舗外 | 403 | FORBIDDEN |
| version が一致しない | 409 | VERSION_CONFLICT |

### 3.10 テーブルステータス変更

対象APIは `API-S-004 テーブルステータス変更` とする。
会計前にテーブルを PAYMENT_WAITING に変更し、追加注文を停止する。
また、清掃完了後に CLEANING から AVAILABLE に戻す。

| 項目 | 内容 |
| --- | --- |
| 入力 | storeId, tableId, status, restaurant_tables.version |
| 必要ロール | STORE_STAFF |
| 正常時ステータス | 204 No Content |

#### 状態遷移方針

| 現在状態 | 変更可能な状態 |
| --- | --- |
| OCCUPIED | PAYMENT_WAITING |
| CLEANING | AVAILABLE |

PAYMENT_WAITING は会計待ち状態であり、来店客注文およびスタッフ注文の追加登録を許可しない。
会計完了APIは、原則としてこの状態に遷移したテーブルのみを処理対象とする。
status=PAYMENT_WAITING は、現在状態が OCCUPIED の場合のみ許可する。
status=AVAILABLE は、現在状態が CLEANING の場合のみ許可する。
OCCUPIED から PAYMENT_WAITING へ変更する場合、対象テーブルに ACTIVE な visit_sessions が存在することを確認する。

#### 処理手順

| No | 処理 | 内容 |
| --- | --- | --- |
| 1 | 認証・認可 | STORE_STAFF 以上であることを確認する |
| 2 | テーブル取得 | restaurant_tables を tableId で取得する |
| 3 | 店舗制御 | restaurant_tables.store_id が URL の storeId と一致することを確認する |
| 4 | 入力チェック | status が PAYMENT_WAITING または AVAILABLE であることを確認する |
| 5 | 状態遷移確認 | 現在状態と変更後状態の組み合わせが許可された遷移であることを確認する |
| 6 | ACTIVEセッション確認 | OCCUPIED から PAYMENT_WAITING へ変更する場合、対象テーブルに ACTIVE な visit_sessions が存在することを確認する |
| 7 | 楽観ロック確認 | リクエスト version と restaurant_tables.version が一致することを確認する |
| 8 | テーブル更新 | restaurant_tables.status を更新し、version をインクリメントする |

#### 更新テーブル

| テーブル | 操作 | 主な更新内容 |
| --- | --- | --- |
| restaurant_tables | UPDATE | status, updated_at, updated_by, version+1 |

#### 主なエラー

| 条件 | HTTPステータス | エラーコード |
| --- | --- | --- |
| 対象テーブルが存在しない | 404 | NOT_FOUND |
| 自店舗外のテーブル | 403 | FORBIDDEN |
| 不正なステータス | 400 | VALIDATION_ERROR |
| ACTIVE な来店セッションが存在しない | 409 | ACTIVE_VISIT_SESSION_NOT_FOUND |
| 許可されない状態遷移 | 409 | INVALID_STATUS_TRANSITION |
| version が一致しない | 409 | VERSION_CONFLICT |
