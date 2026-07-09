# テスト設計

- [1. テスト設計方針](#1-テスト設計方針)
- [2. テスト対象範囲](#2-テスト対象範囲)
- [3. テスト種別](#3-テスト種別)
  - [3.1 単体テスト](#31-単体テスト)
  - [3.2 API結合テスト](#32-api結合テスト)
  - [3.3 セキュリティテスト](#33-セキュリティテスト)
  - [3.4 バッチテスト](#34-バッチテスト)
- [4. 共通テスト観点](#4-共通テスト観点)
  - [4.1 正常系](#41-正常系)
  - [4.2 入力値異常](#42-入力値異常)
  - [4.3 業務エラー](#43-業務エラー)
  - [4.4 認証・認可](#44-認証認可)
  - [4.5 店舗単位アクセス制御](#45-店舗単位アクセス制御)
  - [4.6 トランザクション](#46-トランザクション)
  - [4.7 楽観ロック](#47-楽観ロック)
- [5. 主要機能別テスト観点](#5-主要機能別テスト観点)
  - [5.1 テーブル利用開始](#51-テーブル利用開始)
  - [5.2 テーブルステータス変更](#52-テーブルステータス変更)
  - [5.3 注文登録](#53-注文登録)
  - [5.4 注文ステータス更新](#54-注文ステータス更新)
  - [5.5 調理ステータス更新](#55-調理ステータス更新)
  - [5.6 会計金額確認](#56-会計金額確認)
  - [5.7 会計完了](#57-会計完了)
  - [5.8 メニュー登録](#58-メニュー登録)
  - [5.9 メニュー更新](#59-メニュー更新)
  - [5.10 メニュー販売ステータス変更](#510-メニュー販売ステータス変更)
- [6. セキュリティテスト観点](#6-セキュリティテスト観点)
- [7. バッチテスト観点](#7-バッチテスト観点)
- [8. テストデータ方針](#8-テストデータ方針)

## 1. テスト設計方針

本システムでは、飲食店の注文・会計・売上管理に関わる業務処理を対象に、正常系、異常系、境界値、認証・認可、店舗単位アクセス制御、トランザクション、楽観ロックを重点的にテストする。

特に、注文登録、会計完了、テーブルステータス変更など、複数テーブル更新や状態遷移を伴う処理では、データ不整合が発生しないことを確認する。

テストでは以下を重視する。

| No | 観点 | 内容 |
| --- | --- | --- |
| 1 | 業務ルール | 注文可否、会計可否、状態遷移が仕様通りであること |
| 2 | 認証・認可 | JWT認証、ロール認可、自店舗制御が機能すること |
| 3 | データ整合性 | 複数テーブル更新時に不整合が発生しないこと |
| 4 | 排他制御 | version 不一致時に 409 Conflict となること |
| 5 | エラー応答 | HTTPステータスとエラーコードが設計通りであること |

## 2. テスト対象範囲

本テスト設計では、Spring Boot API の Service 層、Controller 層、Mapper 層、および主要業務APIを対象とする。

| 対象 | テスト内容 |
| --- | --- |
| Controller | リクエストDTOの入力チェック、HTTPステータス、レスポンス形式 |
| Service | 業務ルール、状態遷移、認可、トランザクション、楽観ロック |
| Mapper | SQL実行結果、条件指定、登録・更新・取得結果 |
| API | MockMvc 等によるリクエストからレスポンスまでの確認 |
| Security | JWT認証、ロール認可、店舗単位アクセス制御 |
| Batch | 日次売上集計バッチの正常終了・異常終了 |

フロントエンド画面の表示テストは本設計の主対象外とする。
ただし、API側では画面表示制御に依存せず、必ずサーバー側で認証・認可・業務チェックを行うことをテスト対象とする。

## 3. テスト種別

### 3.1 単体テスト

単体テストでは、Service 層の業務ロジックを中心に検証する。
外部依存をモック化し、入力値、業務状態、認可条件に応じて期待する結果または例外が返ることを確認する。

主な対象は以下とする。

| 対象 | 内容 |
| --- | --- |
| Service | 注文可否判定、会計可否判定、状態遷移、金額計算 |
| Validator | 入力値チェック、境界値チェック |
| Utility | トークン生成、金額計算補助など |

### 3.2 API結合テスト

API結合テストでは、MockMvc 等を利用し、HTTPリクエストからレスポンスまでを検証する。
Controller、Service、Mapper、例外ハンドリング、HTTPステータス、レスポンスJSONを確認対象とする。

主な確認内容は以下とする。

| 観点 | 内容 |
| --- | --- |
| 正常系 | 期待するHTTPステータスとレスポンスが返ること |
| 入力値異常 | Bean Validation エラー時に 400 Bad Request となること |
| 業務エラー | 状態不整合時に 409 Conflict となること |
| 認可エラー | ロール不足・自店舗外アクセス時に 403 Forbidden となること |
| 対象なし | 存在しないID指定時に 404 Not Found となること |

### 3.3 セキュリティテスト

セキュリティテストでは、Cognito JWT 認証、ロール認可、店舗単位アクセス制御、来店客向けQR注文制御を確認する。

主な確認内容は以下とする。

| 観点 | 内容 |
| --- | --- |
| JWT認証 | JWTなし、不正JWT、期限切れJWTを拒否する |
| ロール認可 | 必要ロールを満たさないユーザーを拒否する |
| ロール階層 | STORE_MANAGER が STORE_STAFF 権限APIを利用できる |
| 店舗単位制御 | STORE_STAFF / STORE_MANAGER が他店舗データにアクセスできない |
| QR注文制御 | qrToken 単体では注文できず、visitToken と状態チェックが必要 |

### 3.4 バッチテスト

バッチテストでは、日次売上集計バッチを対象に、対象日の支払い済みデータを正しく集計できることを確認する。

詳細なバッチテストケースは、バッチ設計および実装直前に整理する。
現時点では、正常終了、対象データなし、異常終了、再実行時の扱いをテスト観点として保持する。

## 4. 共通テスト観点

### 4.1 正常系

| 観点 | 期待結果 |
| --- | --- |
| 正しい入力値でAPIを実行する | 設計通りのHTTPステータスとレスポンスが返る |
| 登録・更新APIを実行する | DBに期待通りのデータが登録・更新される |
| 参照APIを実行する | 条件に一致するデータが返る |

### 4.2 入力値異常

| 観点 | 期待結果 |
| --- | --- |
| 必須項目が未指定 | 400 Bad Request |
| 不正な形式の値を指定 | 400 Bad Request |
| 数値項目に範囲外の値を指定 | 400 Bad Request |
| Enum項目に未定義値を指定 | 400 Bad Request |
| 配列項目が空 | 400 Bad Request |

### 4.3 業務エラー

| 観点 | 期待結果 |
| --- | --- |
| 存在しないIDを指定 | 404 Not Found |
| 処理できない状態のデータを更新 | 409 Conflict |
| 販売停止中メニューを注文 | 409 Conflict |
| 会計済み来店セッションに再度会計完了 | 409 Conflict |
| 許可されない状態遷移を実行 | 409 Conflict |

### 4.4 認証・認可

| 観点 | 期待結果 |
| --- | --- |
| JWTなしでスタッフ系APIへアクセス | 401 Unauthorized |
| 不正JWTでアクセス | 401 Unauthorized |
| JWTは有効だが必要ロール不足 | 403 Forbidden |
| users に存在しない Cognito User でアクセス | 403 Forbidden |
| 論理削除済みユーザーでアクセス | 403 Forbidden |

### 4.5 店舗単位アクセス制御

| 観点 | 期待結果 |
| --- | --- |
| 自店舗データにアクセス | 成功 |
| 他店舗の storeId を指定 | 403 Forbidden |
| 他店舗の orderId を指定 | 403 Forbidden |
| 他店舗の orderItemId を指定 | 403 Forbidden |
| 他店舗の visitSessionId を指定 | 403 Forbidden |

### 4.6 トランザクション

| 観点 | 期待結果 |
| --- | --- |
| 注文登録中に order_items 登録で例外発生 | orders も登録されない |
| 会計完了中に restaurant_tables 更新で例外発生 | payments / visit_sessions も更新されない |
| テーブル利用開始中に visit_sessions 作成で例外発生 | restaurant_tables が OCCUPIED に残らない |

### 4.7 楽観ロック

| 観点 | 期待結果 |
| --- | --- |
| 古い version で更新 | 409 Conflict |
| 同時にテーブル利用開始を実行 | 片方のみ成功し、もう片方は 409 Conflict |
| 同時に会計完了を実行 | 片方のみ成功し、もう片方は 409 Conflict |
| 同時にメニュー更新を実行 | 片方のみ成功し、もう片方は 409 Conflict |

## 5. 主要機能別テスト観点

### 5.1 テーブル利用開始

対象APIは `API-S-003 テーブル利用開始` とする。
空席テーブルを利用中に変更し、来店セッションが作成されることを確認する。

| No | テスト観点 | 期待結果 |
| --- | --- | --- |
| 1 | AVAILABLE のテーブルで利用開始する | 201 Created、restaurant_tables.status が OCCUPIED、visit_sessions が ACTIVE で作成される |
| 2 | STORE_STAFF が自店舗テーブルを利用開始する | 成功 |
| 3 | STORE_STAFF が他店舗テーブルを利用開始する | 403 Forbidden |
| 4 | 存在しない tableId を指定する | 404 Not Found |
| 5 | 既に OCCUPIED のテーブルを利用開始する | 409 Conflict |
| 6 | ACTIVE な visit_sessions が既に存在するテーブルを利用開始する | 409 Conflict |
| 7 | 古い version で利用開始する | 409 Conflict、VERSION_CONFLICT |
| 8 | visit_sessions 作成時に例外が発生する | restaurant_tables.status が OCCUPIED に残らない |

### 5.2 テーブルステータス変更

対象APIは `API-S-004 テーブルステータス変更` とする。
会計前の追加注文停止、および清掃完了後の空席戻しを確認する。

| No | テスト観点 | 期待結果 |
| --- | --- | --- |
| 1 | OCCUPIED のテーブルを PAYMENT_WAITING に変更する | 204 No Content、restaurant_tables.status が PAYMENT_WAITING になる |
| 2 | CLEANING のテーブルを AVAILABLE に変更する | 204 No Content、restaurant_tables.status が AVAILABLE になる |
| 3 | OCCUPIED から AVAILABLE に変更しようとする | 409 Conflict、INVALID_STATUS_TRANSITION |
| 4 | AVAILABLE から PAYMENT_WAITING に変更しようとする | 409 Conflict、INVALID_STATUS_TRANSITION |
| 5 | ACTIVE な visit_sessions が存在しない状態で PAYMENT_WAITING に変更する | 409 Conflict、ACTIVE_VISIT_SESSION_NOT_FOUND |
| 6 | 他店舗テーブルを変更する | 403 Forbidden |
| 7 | 古い version で更新する | 409 Conflict、VERSION_CONFLICT |

### 5.3 注文登録

対象APIは `API-C-004 注文登録` および `API-S-011 注文登録` とする。
来店客注文とスタッフ口頭注文について、注文可否、金額計算、販売可否、トランザクションを確認する。

| No | テスト観点 | 期待結果 |
| --- | --- | --- |
| 1 | 有効な qrToken、visitToken、AVAILABLE メニューで来店客注文する | 201 Created、orders と order_items が作成される |
| 2 | スタッフが OCCUPIED の自店舗テーブルに注文登録する | 201 Created |
| 3 | qrToken のみで注文登録する | 400 Bad Request |
| 4 | qrToken と visitToken のテーブルが一致しない | 403 Forbidden |
| 5 | テーブルが AVAILABLE の状態で注文登録する | 409 Conflict、TABLE_NOT_ORDERABLE |
| 6 | テーブルが PAYMENT_WAITING の状態で注文登録する | 409 Conflict、TABLE_NOT_ORDERABLE |
| 7 | visit_sessions.status が CLOSED の状態で注文登録する | 409 Conflict、VISIT_SESSION_NOT_ACTIVE |
| 8 | visit_sessions.expires_at が期限切れの状態で注文登録する | 409 Conflict、VISIT_SESSION_EXPIRED |
| 9 | 販売停止中または売り切れメニューを注文する | 409 Conflict、MENU_NOT_AVAILABLE |
| 10 | メニューが別店舗に所属している | 409 Conflict、MENU_NOT_AVAILABLE |
| 11 | quantity が0または負数 | 400 Bad Request |
| 12 | 同一リクエスト内で menuId が重複する | 400 Bad Request |
| 13 | 注文金額がサーバー側の menus.price から計算される | order_items.unit_price、subtotal_amount、orders.total_amount が正しい |
| 14 | order_items 登録中に例外が発生する | orders も登録されない |
| 15 | 警告閾値を超える高額注文を登録する | 注文は登録され、WARNログが出力される |

### 5.4 注文ステータス更新

対象APIは `API-S-013 注文ステータス変更` とする。
注文全体の有効状態と状態遷移を確認する。

| No | テスト観点 | 期待結果 |
| --- | --- | --- |
| 1 | ORDERED から COMPLETED に変更する | 200 OK、orders.status が COMPLETED になる |
| 2 | ORDERED から CANCELLED に変更する | 200 OK、orders.status が CANCELLED になる |
| 3 | COMPLETED から CANCELLED に変更しようとする | 409 Conflict、INVALID_STATUS_TRANSITION |
| 4 | CANCELLED から ORDERED に戻そうとする | 409 Conflict、INVALID_STATUS_TRANSITION |
| 5 | 不正な status を指定する | 400 Bad Request |
| 6 | 他店舗の注文を更新する | 403 Forbidden |
| 7 | 古い version で更新する | 409 Conflict、VERSION_CONFLICT |

### 5.5 調理ステータス更新

対象APIは `API-S-012 注文明細調理ステータス更新` とする。
注文明細単位の調理・提供状態の遷移を確認する。

| No | テスト観点 | 期待結果 |
| --- | --- | --- |
| 1 | WAITING から COOKING に変更する | 200 OK、cooking_status が COOKING になる |
| 2 | COOKING から READY に変更する | 200 OK、cooking_status が READY になる |
| 3 | READY から SERVED に変更する | 200 OK、cooking_status が SERVED になる |
| 4 | WAITING から SERVED に直接変更しようとする | 409 Conflict、INVALID_STATUS_TRANSITION |
| 5 | SERVED から COOKING に戻そうとする | 409 Conflict、INVALID_STATUS_TRANSITION |
| 6 | 不正な cookingStatus を指定する | 400 Bad Request |
| 7 | 他店舗の orderItemId を指定する | 403 Forbidden |
| 8 | 古い version で更新する | 409 Conflict、VERSION_CONFLICT |

### 5.6 会計金額確認

対象APIは `API-S-014 会計金額確認` とする。
来店セッション単位の会計予定金額を確認する。

| No | テスト観点 | 期待結果 |
| --- | --- | --- |
| 1 | 未会計の来店セッションの会計金額を確認する | 200 OK、CANCELLED 以外の orders.total_amount の合計が返る |
| 2 | CANCELLED の注文が含まれる | CANCELLED の注文金額は合計に含まれない |
| 3 | 既に PAID の payment が存在する | 200 OK、paymentStatus=PAID |
| 4 | 存在しない visitSessionId を指定する | 404 Not Found |
| 5 | 他店舗の visitSessionId を指定する | 403 Forbidden |

### 5.7 会計完了

対象APIは `API-S-015 会計処理` とする。
支払い情報、来店セッション、テーブル状態が同一トランザクションで更新されることを確認する。

| No | テスト観点 | 期待結果 |
| --- | --- | --- |
| 1 | PAYMENT_WAITING の来店セッションを会計完了する | 200 OK、payments が PAID、visit_sessions が CLOSED、restaurant_tables が CLEANING になる |
| 2 | OCCUPIED のまま会計完了しようとする | 409 Conflict |
| 3 | 既に PAID の payment が存在する状態で会計完了する | 409 Conflict、PAYMENT_ALREADY_COMPLETED |
| 4 | 会計対象注文が存在しない | 409 Conflict、NO_PAYABLE_ORDER |
| 5 | リクエスト amount とサーバー再集計金額が一致しない | 409 Conflict、PAYMENT_AMOUNT_MISMATCH |
| 6 | 他店舗の visitSessionId を指定する | 403 Forbidden |
| 7 | 古い visitSessionVersion または tableVersion で会計完了する | 409 Conflict、VERSION_CONFLICT |
| 8 | restaurant_tables 更新中に例外が発生する | payments / visit_sessions も更新されない |
| 9 | 同時に会計完了を実行する | 片方のみ成功し、もう片方は 409 Conflict |

### 5.8 メニュー登録

対象APIは `API-M-003 自店舗メニュー登録` とする。
店長が自店舗メニューを登録できることを確認する。

| No | テスト観点 | 期待結果 |
| --- | --- | --- |
| 1 | STORE_MANAGER が自店舗メニューを登録する | 201 Created、menus が作成される |
| 2 | STORE_STAFF がメニュー登録する | 403 Forbidden |
| 3 | 他店舗の storeId にメニュー登録する | 403 Forbidden |
| 4 | 存在しない categoryId を指定する | 404 Not Found |
| 5 | 他店舗の categoryId を指定する | 403 Forbidden |
| 6 | price が0以下 | 400 Bad Request |
| 7 | 同一店舗・同一カテゴリ内に同名メニューを登録する | 409 Conflict、MENU_ALREADY_EXISTS |

### 5.9 メニュー更新

対象APIは `API-M-004 自店舗メニュー更新` とする。
既存メニューの情報更新、カテゴリ整合性、楽観ロックを確認する。

| No | テスト観点 | 期待結果 |
| --- | --- | --- |
| 1 | STORE_MANAGER が自店舗メニューを更新する | 200 OK、menus が更新される |
| 2 | STORE_STAFF がメニュー更新する | 403 Forbidden |
| 3 | 他店舗の menuId を更新する | 403 Forbidden |
| 4 | 存在しない menuId を指定する | 404 Not Found |
| 5 | 他店舗の categoryId を指定する | 403 Forbidden |
| 6 | 自分以外の同名メニューと重複する | 409 Conflict、MENU_ALREADY_EXISTS |
| 7 | 古い version で更新する | 409 Conflict、VERSION_CONFLICT |

### 5.10 メニュー販売ステータス変更

対象APIは `API-S-008 自店舗メニュー販売ステータス変更` とする。
日次運用で売り切れや一時停止を反映できることを確認する。

| No | テスト観点 | 期待結果 |
| --- | --- | --- |
| 1 | STORE_STAFF が AVAILABLE から SOLD_OUT に変更する | 200 OK、menus.status が SOLD_OUT になる |
| 2 | STORE_STAFF が SOLD_OUT から AVAILABLE に戻す | 200 OK、menus.status が AVAILABLE になる |
| 3 | STORE_MANAGER が販売ステータスを変更する | 200 OK |
| 4 | 他店舗の menuId を変更する | 403 Forbidden |
| 5 | 不正な status を指定する | 400 Bad Request |
| 6 | 古い version で更新する | 409 Conflict、VERSION_CONFLICT |

## 6. セキュリティテスト観点

| No | テスト観点 | 期待結果 |
| --- | --- | --- |
| 1 | Authorization ヘッダーなしでスタッフ系APIへアクセスする | 401 Unauthorized |
| 2 | token_use が access ではないJWTでアクセスする | 401 Unauthorized |
| 3 | 想定外の client_id のJWTでアクセスする | 401 Unauthorized |
| 4 | users に存在しない Cognito User でアクセスする | 403 Forbidden |
| 5 | 論理削除済み users でアクセスする | 403 Forbidden |
| 6 | STORE_STAFF が STORE_MANAGER 権限APIへアクセスする | 403 Forbidden |
| 7 | STORE_MANAGER が STORE_STAFF 権限APIへアクセスする | 成功 |
| 8 | STORE_STAFF が他店舗データへアクセスする | 403 Forbidden |
| 9 | qrToken のみで注文登録する | 400 Bad Request |
| 10 | qrToken と visitToken の対象テーブルが一致しない | 403 Forbidden |
| 11 | 期限切れ visitToken で注文登録する | 409 Conflict |

## 7. バッチテスト観点

日次売上集計バッチについて、以下の観点を確認する。
詳細なテストケースは、バッチ設計および実装直前に整理する。

| No | テスト観点 | 期待結果 |
| --- | --- | --- |
| 1 | 対象日に PAID の payments が存在する | daily_sales_summaries に店舗別売上が作成される |
| 2 | 対象日に PAID の payments が存在しない | 集計対象なしとして正常終了し、batch_job_histories に COMPLETED を記録する |
| 3 | 同一対象日で再実行する | 既存集計を更新または洗い替えし、重複登録しない |
| 4 | バッチ実行中に例外が発生する | batch_job_histories に FAILED とエラー内容を記録する |
| 5 | バッチが正常終了する | batch_job_histories に COMPLETED を記録する |

## 8. テストデータ方針

テストでは、ロール、店舗、テーブル状態、来店セッション状態、メニュー状態を組み合わせたデータを用意する。

| データ種別 | 方針 |
| --- | --- |
| 店舗 | 自店舗、他店舗を判定できるように複数店舗を用意する |
| ユーザー | ADMIN、STORE_MANAGER、STORE_STAFF を用意する |
| テーブル | AVAILABLE、OCCUPIED、PAYMENT_WAITING、CLEANING の各状態を用意する |
| 来店セッション | ACTIVE、CLOSED、期限切れの状態を用意する |
| メニュー | AVAILABLE、SOLD_OUT、SUSPENDED、論理削除済みを用意する |
| 注文 | ORDERED、COMPLETED、CANCELLED を用意する |
| 注文明細 | WAITING、COOKING、READY、SERVED、CANCELLED を用意する |
| 会計 | 未会計、PAID を用意する |
| version | 正常な version、古い version、不一致 version を用意する |

テストデータは、正常系だけでなく、他店舗データ、期限切れセッション、販売不可メニュー、古い version など、異常系を再現できるように準備する。
