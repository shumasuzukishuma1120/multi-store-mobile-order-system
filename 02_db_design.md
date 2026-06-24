# DB設- [DB設計](#db設計)

## 目次

- [DB設- DB設計](#db設--db設計)
  - [目次](#目次)
  - [1. DB設計方針](#1-db設計方針)
    - [マイグレーション管理方針](#マイグレーション管理方針)
  - [2. テーブル一覧](#2-テーブル一覧)
  - [3. テーブル定義](#3-テーブル定義)
    - [3.1 stores](#31-stores)
    - [3.2 users](#32-users)
    - [3.3 restaurant\_tables](#33-restaurant_tables)
    - [3.4 visit\_sessions](#34-visit_sessions)
    - [3.5 menu\_categories](#35-menu_categories)
    - [3.6 menus](#36-menus)
    - [3.7 orders](#37-orders)
    - [3.8 order\_items](#38-order_items)
    - [3.9 payments](#39-payments)
    - [3.10 daily\_sales\_summaries](#310-daily_sales_summaries)
    - [3.11 batch\_job\_histories](#311-batch_job_histories)
  - [4. リレーション設計](#4-リレーション設計)
    - [4.1 主リレーション](#41-主リレーション)
    - [4.2 監査用リレーション](#42-監査用リレーション)
  - [5. 制約設計](#5-制約設計)
    - [5.1 主キー制約](#51-主キー制約)
    - [5.2 外部キー制約](#52-外部キー制約)
    - [5.3 UNIQUE制約](#53-unique制約)
      - [5.3.1 単一カラムのユニーク制約](#531-単一カラムのユニーク制約)
      - [5.3.2 複合ユニーク制約](#532-複合ユニーク制約)
    - [5.4 NOT NULL制約](#54-not-null制約)
    - [5.5 業務制約](#55-業務制約)
  - [6. インデックス設計](#6-インデックス設計)
    - [6.1 インデックス設計方針](#61-インデックス設計方針)
    - [6.2 インデックス一覧](#62-インデックス一覧)
    - [6.2.1 制約により作成されるインデックス](#621-制約により作成されるインデックス)
      - [6.2.2 検索性能向上目的のインデックス](#622-検索性能向上目的のインデックス)
  - [7. 排他制御方針](#7-排他制御方針)
    - [7.1 排他制御の基本方針](#71-排他制御の基本方針)
    - [7.2 楽観ロック対象テーブル](#72-楽観ロック対象テーブル)
    - [7.3 楽観ロックの更新方式](#73-楽観ロックの更新方式)
    - [7.4 楽観ロックエラー時の扱い](#74-楽観ロックエラー時の扱い)
    - [7.5 トランザクション制御との関係](#75-トランザクション制御との関係)
  - [8. 論理削除方針](#8-論理削除方針)
    - [8.1 基本方針](#81-基本方針)
    - [8.2 論理削除対象テーブル](#82-論理削除対象テーブル)
    - [8.3 論理削除しないテーブル](#83-論理削除しないテーブル)
    - [8.4 論理削除時の扱い](#84-論理削除時の扱い)
  - [9. 監査項目方針](#9-監査項目方針)
    - [9.1 基本方針](#91-基本方針)
    - [9.2 作成日時・更新日時](#92-作成日時更新日時)
    - [9.3 作成者・更新者](#93-作成者更新者)
    - [9.4 論理削除日時](#94-論理削除日時)
    - [9.5 来店客操作・バッチ処理時の扱い](#95-来店客操作バッチ処理時の扱い)
  - [10. ステータス定義](#10-ステータス定義)
    - [10.1 ステータス管理方針](#101-ステータス管理方針)
    - [10.2 restaurant\_tables.status](#102-restaurant_tablesstatus)
    - [10.3 visit\_sessions.status](#103-visit_sessionsstatus)
    - [10.4 menus.status](#104-menusstatus)
    - [10.5 orders.status](#105-ordersstatus)
    - [10.6 order\_items.cooking\_status](#106-order_itemscooking_status)
    - [10.7 payments.status](#107-paymentsstatus)
    - [10.8 batch\_job\_histories.status](#108-batch_job_historiesstatus)

## 1. DB設計方針

本システムは複数店舗向けのモバイルオーダー・売上管理システムのため、店舗単位で扱う業務データは原則として store_id を持ち、店舗単位でデータを分離する。
ただし、親テーブルから店舗を特定できる子テーブルについては、冗長な store_id を持たせるかどうかをテーブルごとに判断する。

来店客以外のユーザーは Keycloak で認証するため、アプリケーション側のDBではパスワードを保持しない。
users テーブルでは Keycloak のユーザーID、業務ロール、所属店舗など、業務上必要な情報のみを管理する。

来店客は Keycloak による認証の対象外とし、テーブルに紐づく QR トークン、来店セッション、テーブルステータスによって注文可否を制御する。
QR トークン自体は注文権限として信用せず、注文可否はサーバー側で判定する。

1つの来店セッションに対して複数の注文を登録できる。
会計は注文単位ではなく来店セッション単位でまとめて行うため、payments は visit_session_id に紐づける。

マスタ系・トランザクション系の更新対象テーブルには version カラムを持たせ、楽観ロックに利用する。
集計結果・バッチ履歴など、追記中心または再作成可能なテーブルについては、必要性を個別に判断する。

本システムでは店内飲食のみを対象とし、消費税率は一律10%とする。
テイクアウト、デリバリー、軽減税率8%、複数税率、インボイス対応は対象外とする。
メニュー価格は税込価格として登録し、金額は INTEGER で保持する。
注文時点の税込単価を order_items.unit_price に保持し、注文後にメニュー価格が変更されても過去注文の金額には影響しない設計とする。

作成日時・更新日時は各テーブルに保持し、データの作成・更新履歴を追跡できるようにする。 
論理削除の要否はテーブルごとに判断し、マスタ系データは削除履歴を残せるようにする。
トランザクション系データは原則削除せず、ステータスで状態を管理する。

visit_token は、スタッフがテーブル利用開始操作を行い、テーブル状態が AVAILABLE から OCCUPIED に遷移するタイミングでサーバー側が発行する。
会計完了時に visit_sessions.status を CLOSED に更新し、ended_at を設定することで、該当 visit_token を無効化する。

### マイグレーション管理方針

本システムでは、DBスキーマの変更管理に Flyway を使用する。
テーブル作成、カラム追加、インデックス追加、制約追加などのDDLは、SQLマイグレーションファイルとして管理する。

マイグレーションファイルは `src/main/resources/db/migration` 配下に配置する。
ローカル環境ではアプリケーション起動時に未適用のマイグレーションを自動適用する。
本番相当環境では、デプロイ手順の中でマイグレーション実行タイミングを管理する。

ローカル環境、テスト環境、AWS環境で同一のマイグレーションを適用することで、環境差異によるDB不整合を防ぐ。

## 2. テーブル一覧

| テーブル名            | 概要                                                                         |
| --------------------- | ---------------------------------------------------------------------------- |
| stores                | 店舗情報を管理する                                                           |
| users                 | Keycloakと連携するスタッフの業務ユーザー情報、所属店舗、業務ロールを管理する |
| restaurant_tables     | 店舗内の客席テーブル情報、QRトークン、テーブルステータスを管理する           |
| visit_sessions        | 来店単位のセッション情報、有効期限、状態を管理する                           |
| menu_categories       | 店舗ごとのメニューカテゴリ情報を管理する                                     |
| menus                 | 店舗ごとのメニュー情報、価格、販売状態を管理する                             |
| orders                | 来店セッションに紐づく注文情報を管理する                                     |
| order_items           | 注文に紐づく注文明細、数量、注文時点の単価、調理ステータスを管理する         |
| payments              | 来店セッション単位の支払い情報、支払い金額、支払い状態を管理する             |
| daily_sales_summaries | 日次売上集計バッチで作成される店舗別の日次売上集計情報を管理する             |
| batch_job_histories   | バッチ実行履歴、実行結果、開始・終了時刻、エラー内容を管理する               |

## 3. テーブル定義

### 3.1 stores

| カラム名   | 型           | PK  | FK       | NOT NULL | UNIQUE | DEFAULT           | 説明                   |
| ---------- | ------------ | --- | -------- | -------- | ------ | ----------------- | ---------------------- |
| id         | BIGINT       | Y   | N        | Y        | N      | IDENTITY          | 店舗ID                 |
| name       | VARCHAR(100) | N   | N        | Y        | N      |                   | 店舗名                 |
| created_at | TIMESTAMP    | N   | N        | Y        | N      | CURRENT_TIMESTAMP | 作成日時               |
| created_by | BIGINT       | N   | users.id | N        | N      |                   | 作成者ユーザーID       |
| updated_at | TIMESTAMP    | N   | N        | Y        | N      | CURRENT_TIMESTAMP | 更新日時               |
| updated_by | BIGINT       | N   | users.id | N        | N      |                   | 更新者ユーザーID       |
| deleted_at | TIMESTAMP    | N   | N        | N        | N      | NULL              | 論理削除日時           |
| version    | INTEGER      | N   | N        | Y        | N      | 0                 | 楽観ロック用バージョン |

### 3.2 users

| カラム名         | 型           | PK  | FK        | NOT NULL | UNIQUE | DEFAULT           | 説明                                           |
| ---------------- | ------------ | --- | --------- | -------- | ------ | ----------------- | ---------------------------------------------- |
| id               | BIGINT       | Y   | N         | Y        | N      | IDENTITY          | ユーザーID                                     |
| keycloak_user_id | VARCHAR(255) | N   | N         | Y        | Y      |                   | KeycloakユーザーID                             |
| role             | VARCHAR(30)  | N   | N         | Y        | N      |                   | 業務ロール ADMIN / STORE_MANAGER / STORE_STAFF |
| user_name        | VARCHAR(100) | N   | N         | Y        | N      |                   | ユーザー名                                     |
| store_id         | BIGINT       | N   | stores.id | N        | N      |                   | 所属店舗ID  ADMINの場合はNULL                  |
| created_at       | TIMESTAMP    | N   | N         | Y        | N      | CURRENT_TIMESTAMP | 作成日時                                       |
| created_by       | BIGINT       | N   | users.id  | N        | N      |                   | 作成者ユーザーID                               |
| updated_at       | TIMESTAMP    | N   | N         | Y        | N      | CURRENT_TIMESTAMP | 更新日時                                       |
| updated_by       | BIGINT       | N   | users.id  | N        | N      |                   | 更新者ユーザーID                               |
| deleted_at       | TIMESTAMP    | N   | N         | N        | N      | NULL              | 論理削除日時                                   |
| version          | INTEGER      | N   | N         | Y        | N      | 0                 | 楽観ロック用バージョン                         |

### 3.3 restaurant_tables

| カラム名     | 型           | PK  | FK        | NOT NULL | UNIQUE | DEFAULT           | 説明                   |
| ------------ | ------------ | --- | --------- | -------- | ------ | ----------------- | ---------------------- |
| id           | BIGINT       | Y   | N         | Y        | N      | IDENTITY          | テーブルID             |
| table_number | VARCHAR(20)  | N   | N         | Y        | N      |                   | テーブル番号           |
| store_id     | BIGINT       | N   | stores.id | Y        | N      |                   | 所属店舗ID             |
| status       | VARCHAR(30)  | N   | N         | Y        | N      | 'AVAILABLE'       | テーブルステータス     |
| qr_token     | VARCHAR(255) | N   | N         | Y        | Y      |                   | 固定QRトークン         |
| created_at   | TIMESTAMP    | N   | N         | Y        | N      | CURRENT_TIMESTAMP | 作成日時               |
| created_by   | BIGINT       | N   | users.id  | N        | N      |                   | 作成者ユーザーID       |
| updated_at   | TIMESTAMP    | N   | N         | Y        | N      | CURRENT_TIMESTAMP | 更新日時               |
| updated_by   | BIGINT       | N   | users.id  | N        | N      |                   | 更新者ユーザーID       |
| deleted_at   | TIMESTAMP    | N   | N         | N        | N      | NULL              | 論理削除日時           |
| version      | INTEGER      | N   | N         | Y        | N      | 0                 | 楽観ロック用バージョン |

### 3.4 visit_sessions

| カラム名    | 型           | PK  | FK                   | NOT NULL | UNIQUE | DEFAULT           | 説明                       |
| ----------- | ------------ | --- | -------------------- | -------- | ------ | ----------------- | -------------------------- |
| id          | BIGINT       | Y   | N                    | Y        | N      | IDENTITY          | 来店セッションID           |
| status      | VARCHAR(30)  | N   | N                    | Y        | N      | 'ACTIVE'          | 来店セッションステータス   |
| visit_token | VARCHAR(255) | N   | N                    | Y        | Y      |                   | 来店トークン               |
| store_id    | BIGINT       | N   | stores.id            | Y        | N      |                   | 所属店舗ID                 |
| table_id    | BIGINT       | N   | restaurant_tables.id | Y        | N      |                   | テーブルID                 |
| created_at  | TIMESTAMP    | N   | N                    | Y        | N      | CURRENT_TIMESTAMP | 作成日時                   |
| updated_at  | TIMESTAMP    | N   | N                    | Y        | N      | CURRENT_TIMESTAMP | 更新日時                   |
| expires_at  | TIMESTAMP    | N   | N                    | Y        | N      |                   | 来店セッション有効期限日時 |
| started_at  | TIMESTAMP    | N   | N                    | Y        | N      | CURRENT_TIMESTAMP | 来店セッション開始日時     |
| ended_at    | TIMESTAMP    | N   | N                    | N        | N      | NULL              | 来店セッション終了日時     |
| version     | INTEGER      | N   | N                    | Y        | N      | 0                 | 楽観ロック用バージョン     |

### 3.5 menu_categories

| カラム名   | 型           | PK  | FK        | NOT NULL | UNIQUE | DEFAULT           | 説明                   |
| ---------- | ------------ | --- | --------- | -------- | ------ | ----------------- | ---------------------- |
| id         | BIGINT       | Y   | N         | Y        | N      | IDENTITY          | メニューカテゴリID     |
| name       | VARCHAR(100) | N   | N         | Y        | N      |                   | メニューカテゴリ名     |
| store_id   | BIGINT       | N   | stores.id | Y        | N      |                   | 所属店舗ID             |
| created_at | TIMESTAMP    | N   | N         | Y        | N      | CURRENT_TIMESTAMP | 作成日時               |
| created_by | BIGINT       | N   | users.id  | N        | N      |                   | 作成者ユーザーID       |
| updated_at | TIMESTAMP    | N   | N         | Y        | N      | CURRENT_TIMESTAMP | 更新日時               |
| updated_by | BIGINT       | N   | users.id  | N        | N      |                   | 更新者ユーザーID       |
| deleted_at | TIMESTAMP    | N   | N         | N        | N      | NULL              | 論理削除日時           |
| version    | INTEGER      | N   | N         | Y        | N      | 0                 | 楽観ロック用バージョン |

### 3.6 menus

| カラム名         | 型           | PK  | FK                 | NOT NULL | UNIQUE | DEFAULT           | 説明                   |
| ---------------- | ------------ | --- | ------------------ | -------- | ------ | ----------------- | ---------------------- |
| id               | BIGINT       | Y   | N                  | Y        | N      | IDENTITY          | メニューID             |
| name             | VARCHAR(100) | N   | N                  | Y        | N      |                   | メニュー名             |
| menu_category_id | BIGINT       | N   | menu_categories.id | Y        | N      |                   | メニューカテゴリID     |
| store_id         | BIGINT       | N   | stores.id          | Y        | N      |                   | 所属店舗ID             |
| price            | INTEGER      | N   | N                  | Y        | N      |                   | 価格                   |
| status           | VARCHAR(30)  | N   | N                  | Y        | N      | 'AVAILABLE'       | 販売状態               |
| created_at       | TIMESTAMP    | N   | N                  | Y        | N      | CURRENT_TIMESTAMP | 作成日時               |
| created_by       | BIGINT       | N   | users.id           | N        | N      |                   | 作成者ユーザーID       |
| updated_at       | TIMESTAMP    | N   | N                  | Y        | N      | CURRENT_TIMESTAMP | 更新日時               |
| updated_by       | BIGINT       | N   | users.id           | N        | N      |                   | 更新者ユーザーID       |
| deleted_at       | TIMESTAMP    | N   | N                  | N        | N      | NULL              | 論理削除日時           |
| version          | INTEGER      | N   | N                  | Y        | N      | 0                 | 楽観ロック用バージョン |

### 3.7 orders

| カラム名         | 型          | PK  | FK                   | NOT NULL | UNIQUE | DEFAULT           | 説明                   |
| ---------------- | ----------- | --- | -------------------- | -------- | ------ | ----------------- | ---------------------- |
| id               | BIGINT      | Y   | N                    | Y        | N      | IDENTITY          | 注文ID                 |
| visit_session_id | BIGINT      | N   | visit_sessions.id    | Y        | N      |                   | 来店セッションID       |
| store_id         | BIGINT      | N   | stores.id            | Y        | N      |                   | 所属店舗ID             |
| table_id         | BIGINT      | N   | restaurant_tables.id | Y        | N      |                   | レストランテーブルID   |
| status           | VARCHAR(30) | N   | N                    | Y        | N      | 'ORDERED'         | 注文ステータス         |
| total_amount     | INTEGER     | N   | N                    | Y        | N      | 0                 | 注文合計金額           |
| created_at       | TIMESTAMP   | N   | N                    | Y        | N      | CURRENT_TIMESTAMP | 作成日時               |
| created_by       | BIGINT      | N   | users.id             | N        | N      |                   | 作成者ユーザーID       |
| updated_at       | TIMESTAMP   | N   | N                    | Y        | N      | CURRENT_TIMESTAMP | 更新日時               |
| updated_by       | BIGINT      | N   | users.id             | N        | N      |                   | 更新者ユーザーID       |
| version          | INTEGER     | N   | N                    | Y        | N      | 0                 | 楽観ロック用バージョン |

### 3.8 order_items

| カラム名        | 型          | PK  | FK        | NOT NULL | UNIQUE | DEFAULT           | 説明                   |
| --------------- | ----------- | --- | --------- | -------- | ------ | ----------------- | ---------------------- |
| id              | BIGINT      | Y   | N         | Y        | N      | IDENTITY          | 注文明細ID             |
| order_id        | BIGINT      | N   | orders.id | Y        | N      |                   | 注文ID                 |
| menu_id         | BIGINT      | N   | menus.id  | Y        | N      |                   | メニューID             |
| cooking_status  | VARCHAR(30) | N   | N         | Y        | N      | 'WAITING'         | 調理ステータス         |
| quantity        | INTEGER     | N   | N         | Y        | N      |                   | 注文数量               |
| unit_price      | INTEGER     | N   | N         | Y        | N      |                   | 注文時点の単価         |
| subtotal_amount | INTEGER     | N   | N         | Y        | N      |                   | 注文明細小計金額       |
| created_at      | TIMESTAMP   | N   | N         | Y        | N      | CURRENT_TIMESTAMP | 作成日時               |
| updated_at      | TIMESTAMP   | N   | N         | Y        | N      | CURRENT_TIMESTAMP | 更新日時               |
| updated_by      | BIGINT      | N   | users.id  | N        | N      |                   | 更新者ユーザーID       |
| version         | INTEGER     | N   | N         | Y        | N      | 0                 | 楽観ロック用バージョン |

### 3.9 payments

| カラム名         | 型          | PK  | FK                | NOT NULL | UNIQUE | DEFAULT           | 説明                   |
| ---------------- | ----------- | --- | ----------------- | -------- | ------ | ----------------- | ---------------------- |
| id               | BIGINT      | Y   | N                 | Y        | N      | IDENTITY          | 支払いID               |
| store_id         | BIGINT      | N   | stores.id         | Y        | N      |                   | 店舗ID                 |
| visit_session_id | BIGINT      | N   | visit_sessions.id | Y        | Y      |                   | 来店セッションID       |
| amount           | INTEGER     | N   | N                 | Y        | N      | 0                 | 支払い金額             |
| status           | VARCHAR(30) | N   | N                 | Y        | N      | 'PENDING'         | 支払いステータス       |
| paid_at          | TIMESTAMP   | N   | N                 | N        | N      |                   | 支払い完了日時         |
| created_at       | TIMESTAMP   | N   | N                 | Y        | N      | CURRENT_TIMESTAMP | 作成日時               |
| created_by       | BIGINT      | N   | users.id          | N        | N      |                   | 作成者ユーザーID       |
| updated_at       | TIMESTAMP   | N   | N                 | Y        | N      | CURRENT_TIMESTAMP | 更新日時               |
| updated_by       | BIGINT      | N   | users.id          | N        | N      |                   | 更新者ユーザーID       |
| version          | INTEGER     | N   | N                 | Y        | N      | 0                 | 楽観ロック用バージョン |

### 3.10 daily_sales_summaries

| カラム名             | 型        | PK  | FK        | NOT NULL | UNIQUE | DEFAULT           | 説明           |
| -------------------- | --------- | --- | --------- | -------- | ------ | ----------------- | -------------- |
| id                   | BIGINT    | Y   | N         | Y        | N      | IDENTITY          | 日次売上集計ID |
| store_id             | BIGINT    | N   | stores.id | Y        | N      |                   | 店舗ID         |
| sales_date           | DATE      | N   | N         | Y        | N      |                   | 売上日         |
| total_sales_amount   | INTEGER   | N   | N         | Y        | N      | 0                 | 売上合計金額   |
| total_order_count    | INTEGER   | N   | N         | Y        | N      | 0                 | 注文件数       |
| total_customer_count | INTEGER   | N   | N         | Y        | N      | 0                 | 来店組数       |
| created_at           | TIMESTAMP | N   | N         | Y        | N      | CURRENT_TIMESTAMP | 作成日時       |
| updated_at           | TIMESTAMP | N   | N         | Y        | N      | CURRENT_TIMESTAMP | 更新日時       |

### 3.11 batch_job_histories

| カラム名      | 型           | PK  | FK        | NOT NULL | UNIQUE | DEFAULT           | 説明             |
| ------------- | ------------ | --- | --------- | -------- | ------ | ----------------- | ---------------- |
| id            | BIGINT       | Y   | N         | Y        | N      | IDENTITY          | バッチ実行履歴ID |
| store_id      | BIGINT       | N   | stores.id | N        | N      |                   | 所属店舗ID       |
| job_name      | VARCHAR(100) | N   | N         | Y        | N      |                   | バッチ名         |
| status        | VARCHAR(30)  | N   | N         | Y        | N      |                   | 実行ステータス   |
| target_date   | DATE         | N   | N         | N        | N      |                   | 処理対象日       |
| start_time    | TIMESTAMP    | N   | N         | Y        | N      | CURRENT_TIMESTAMP | 開始時刻         |
| end_time      | TIMESTAMP    | N   | N         | N        | N      |                   | 終了時刻         |
| error_message | TEXT         | N   | N         | N        | N      |                   | エラー内容       |

## 4. リレーション設計

### 4.1 主リレーション

| 親テーブル        | 子テーブル            | 関係 | 説明                                                                                                  |
| ----------------- | --------------------- | ---- | ----------------------------------------------------------------------------------------------------- |
| stores            | users                 | 1:N  | 1店舗に複数スタッフが所属する。ただし、ADMIN は特定店舗に所属しないため users.store_id は NULL とする |
| stores            | restaurant_tables     | 1:N  | 1店舗に複数の客席テーブルを持つ                                                                       |
| stores            | menu_categories       | 1:N  | 1店舗に複数のメニューカテゴリを持つ                                                                   |
| stores            | menus                 | 1:N  | 1店舗に複数のメニューを持つ。店舗ごとに販売メニュー・価格・販売状態を管理する                         |
| stores            | visit_sessions        | 1:N  | 1店舗に複数の来店セッションが発生する                                                                 |
| stores            | payments              | 1:N  | 1店舗に複数の支払いが発生する。売上集計・自店舗制御のため payments.store_id を保持する                |
| stores            | daily_sales_summaries | 1:N  | 1店舗に複数の日次売上集計結果を持つ                                                                   |
| stores            | batch_job_histories   | 1:N  | 店舗別バッチの場合、1店舗に複数のバッチ実行履歴が紐づく。全店舗対象バッチでは store_id は NULL とする |
| menu_categories   | menus                 | 1:N  | 1つのメニューカテゴリに複数のメニューが紐づく                                                         |
| restaurant_tables | visit_sessions        | 1:N  | 1つの客席テーブルに対して、来店ごとに複数の来店セッション履歴が発生する                               |
| visit_sessions    | orders                | 1:N  | 1つの来店セッションに複数の注文が発生する                                                             |
| orders            | order_items           | 1:N  | 1つの注文に複数の注文明細が紐づく                                                                     |
| visit_sessions    | payments              | 1:1  | 1つの来店セッションにつき1回の会計を行う                                                              |

※ orders は visit_session_id から店舗・テーブルを辿ることができるが、自店舗制御、検索、売上集計、画面表示を容易にするため store_id と table_id も保持する。orders.store_id / orders.table_id / orders.visit_session_id の整合性は業務ロジックで保証する。

### 4.2 監査用リレーション

| 親テーブル | 子テーブル        | 関係 | 説明                                                                                                                                       |
| ---------- | ----------------- | ---- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| users      | stores            | 1:N  | 1人のユーザーは複数の店舗情報を作成・更新できる。stores.created_by / updated_by が users.id を参照する                                     |
| users      | restaurant_tables | 1:N  | 1人のユーザーは複数のテーブル情報を作成・更新できる。restaurant_tables.created_by / updated_by が users.id を参照する                      |
| users      | menu_categories   | 1:N  | 1人のユーザーは複数のメニューカテゴリを作成・更新できる。menu_categories.created_by / updated_by が users.id を参照する                    |
| users      | menus             | 1:N  | 1人のユーザーは複数のメニューを作成・更新できる。menus.created_by / updated_by が users.id を参照する                                      |
| users      | orders            | 1:N  | 1人のユーザーは複数の注文情報を作成・更新できる。orders.created_by / updated_by が users.id を参照する。来店客注文の場合 created_by はNULL |
| users      | order_items       | 1:N  | 1人のユーザーは複数の注文明細を更新できる。order_items.updated_by が users.id を参照する                                                   |
| users      | payments          | 1:N  | 1人のユーザーは複数の会計処理を作成・更新できる。payments.created_by / updated_by が users.id を参照する                                   |

## 5. 制約設計

### 5.1 主キー制約

各テーブルは `id` を主キーとする。  
`id` は `BIGINT` とし、PostgreSQL の `IDENTITY` により自動採番する。

対象テーブル：

- stores
- users
- restaurant_tables
- visit_sessions
- menu_categories
- menus
- orders
- order_items
- payments
- daily_sales_summaries
- batch_job_histories

---

### 5.2 外部キー制約

| テーブル              | カラム           | 参照先               | 説明                                                                       |
| --------------------- | ---------------- | -------------------- | -------------------------------------------------------------------------- |
| users                 | store_id         | stores.id            | 所属店舗を参照する。ADMIN の場合は NULL を許容する                         |
| restaurant_tables     | store_id         | stores.id            | 客席テーブルが所属する店舗を参照する                                       |
| visit_sessions        | store_id         | stores.id            | 来店セッションが発生した店舗を参照する                                     |
| visit_sessions        | table_id         | restaurant_tables.id | 来店セッションが発生したテーブルを参照する                                 |
| menu_categories       | store_id         | stores.id            | メニューカテゴリが所属する店舗を参照する                                   |
| menus                 | store_id         | stores.id            | メニューが所属する店舗を参照する                                           |
| menus                 | menu_category_id | menu_categories.id   | メニューが所属するカテゴリを参照する                                       |
| orders                | store_id         | stores.id            | 注文が発生した店舗を参照する                                               |
| orders                | visit_session_id | visit_sessions.id    | 注文が紐づく来店セッションを参照する                                       |
| orders                | table_id         | restaurant_tables.id | 注文が発生したテーブルを参照する                                           |
| order_items           | order_id         | orders.id            | 注文明細が属する注文を参照する                                             |
| order_items           | menu_id          | menus.id             | 注文明細の対象メニューを参照する                                           |
| payments              | store_id         | stores.id            | 支払いが発生した店舗を参照する                                             |
| payments              | visit_session_id | visit_sessions.id    | 支払いが紐づく来店セッションを参照する                                     |
| daily_sales_summaries | store_id         | stores.id            | 日次売上集計の対象店舗を参照する                                           |
| batch_job_histories   | store_id         | stores.id            | 店舗別バッチの場合、対象店舗を参照する。全店舗対象の場合は NULL を許容する |
| stores                | created_by       | users.id             | 店舗情報の作成者を参照する。初期データ投入時は NULL を許容する             |
| stores                | updated_by       | users.id             | 店舗情報の最終更新者を参照する                                             |
| users                 | created_by       | users.id             | ユーザー情報の作成者を参照する。初期管理者作成時は NULL を許容する         |
| users                 | updated_by       | users.id             | ユーザー情報の最終更新者を参照する                                         |
| restaurant_tables     | created_by       | users.id             | テーブル情報の作成者を参照する                                             |
| restaurant_tables     | updated_by       | users.id             | テーブル情報の最終更新者を参照する                                         |
| menu_categories       | created_by       | users.id             | メニューカテゴリの作成者を参照する                                         |
| menu_categories       | updated_by       | users.id             | メニューカテゴリの最終更新者を参照する                                     |
| menus                 | created_by       | users.id             | メニューの作成者を参照する                                                 |
| menus                 | updated_by       | users.id             | メニューの最終更新者を参照する                                             |
| orders                | created_by       | users.id             | 注文作成者を参照する。来店客注文の場合は NULL を許容する                   |
| orders                | updated_by       | users.id             | 注文の最終更新者を参照する                                                 |
| order_items           | updated_by       | users.id             | 注文明細の最終更新者を参照する                                             |
| payments              | created_by       | users.id             | 会計処理の作成者を参照する                                                 |
| payments              | updated_by       | users.id             | 会計処理の最終更新者を参照する                                             |

---

### 5.3 UNIQUE制約

#### 5.3.1 単一カラムのユニーク制約

| テーブル          | カラム           | 説明                                        |
| ----------------- | ---------------- | ------------------------------------------- |
| users             | keycloak_user_id | Keycloak ユーザーIDはシステム内で一意とする |
| restaurant_tables | qr_token         | 固定QRトークンはシステム内で一意とする      |
| visit_sessions    | visit_token      | 来店トークンはシステム内で一意とする        |
| payments          | visit_session_id | 1つの来店セッションにつき支払いは1件とする  |

#### 5.3.2 複合ユニーク制約

| テーブル              | カラム                           | 説明                                               |
| --------------------- | -------------------------------- | -------------------------------------------------- |
| restaurant_tables     | store_id, table_number           | 同一店舗内でテーブル番号は重複させない             |
| menu_categories       | store_id, name                   | 同一店舗内でカテゴリ名は重複させない               |
| menus                 | store_id, menu_category_id, name | 同一店舗・同一カテゴリ内でメニュー名は重複させない |
| daily_sales_summaries | store_id, sales_date             | 同一店舗・同一売上日の集計結果は1件とする          |

---

### 5.4 NOT NULL制約

主キー、業務上必須となる外部キー、名称、ステータス、金額、作成日時、更新日時、`version` は原則 `NOT NULL` とする。

ただし、以下のカラムは `NULL` を許容する。

| テーブル                                                     | カラム                  | NULL許容理由                                           |
| ------------------------------------------------------------ | ----------------------- | ------------------------------------------------------ |
| users                                                        | store_id                | ADMIN は特定店舗に所属しないため                       |
| created_by / updated_by を持つ各テーブル                     | created_by / updated_by | 初期データ投入、来店客操作、未更新データを考慮するため |
| stores / users / restaurant_tables / menu_categories / menus | deleted_at              | 論理削除されていない場合は NULL                        |
| visit_sessions                                               | ended_at                | 来店セッション継続中は NULL                            |
| payments                                                     | paid_at                 | 支払い完了前は NULL                                    |
| batch_job_histories                                          | store_id                | 全店舗対象バッチの場合は NULL                          |
| batch_job_histories                                          | target_date             | 処理対象日を持たないバッチの場合は NULL                |
| batch_job_histories                                          | end_time                | 実行中は NULL                                          |
| batch_job_histories                                          | error_message           | 正常終了時は NULL                                      |

---

### 5.5 業務制約

以下の制約は、DB制約だけでなくアプリケーションの業務ロジックでも検証する。

| No  | 業務制約                                                                                                                                         | 検証タイミング           |
| --- | ------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------ |
| 1   | users.role が ADMIN の場合、store_id は NULL とする                                                                                              | ユーザー登録・更新時     |
| 2   | users.role が STORE_MANAGER / STORE_STAFF の場合、store_id は必須とする                                                                          | ユーザー登録・更新時     |
| 3   | restaurant_tables.status が OCCUPIED の場合のみ、来店客の注文を許可する                                                                          | 注文画面表示・注文登録時 |
| 4   | visit_sessions.status が ACTIVE かつ expires_at が現在時刻より後の場合のみ注文可能とする                                                         | 注文画面表示・注文登録時 |
| 5   | visit_sessions.store_id と restaurant_tables.store_id は一致している必要がある                                                                   | 来店セッション作成時     |
| 6   | menus.store_id と menu_categories.store_id は一致している必要がある                                                                              | メニュー登録・更新時     |
| 7   | orders.store_id、orders.table_id、orders.visit_session_id は同一店舗・同一来店セッションとして整合している必要がある                             | 注文登録時               |
| 8   | order_items.menu_id は注文店舗と同一店舗の販売中メニューである必要がある                                                                         | 注文登録時               |
| 9   | payments.amount は対象 visit_session に紐づく注文合計金額と一致する必要がある                                                                    | 会計処理時               |
| 10  | daily_sales_summaries は payments.status = PAID のデータをもとに集計する                                                                         | 日次売上集計バッチ実行時 |
| 11  | テーブル利用開始時は restaurant_tables.status が AVAILABLE の場合のみ visit_session を作成できる                                                 | テーブル利用開始時       |
| 12  | 会計完了時は payments.status を PAID、visit_sessions.status を CLOSED に更新し、restaurant_tables.status を CLEANING または AVAILABLE に更新する | 会計完了時               |

## 6. インデックス設計

### 6.1 インデックス設計方針

本システムでは、PostgreSQL の標準的な B-tree インデックスを基本とする。

インデックスは検索性能を向上させる一方で、INSERT / UPDATE / DELETE 時の更新コストが増えるため、必要以上に作成しない。
特に、店舗数・テーブル数・メニュー数が少ないマスタ系テーブルについては、通常インデックスの追加は最小限とする。

主キーおよび UNIQUE 制約にはインデックスが作成されるため、同一カラムに重複して通常インデックスは作成しない。
UNIQUE制約は検索性能目的だけでなく、業務上の一意性を保証する目的で設定する。

一方、外部キー制約を設定しても参照元カラムにインデックスが自動作成されるわけではないため、検索条件・JOIN・一覧表示・集計で頻繁に使用し、かつ将来的にデータ件数が増加するカラムには個別にインデックスを設定する。

本設計では、来店セッション、注文、注文明細、支払いなど、日々の業務で継続的に増加するトランザクション系テーブルを中心にインデックスを設定する。

### 6.2 インデックス一覧

### 6.2.1 制約により作成されるインデックス

主キー制約および UNIQUE 制約により、PostgreSQL では制約を実現するためのインデックスが作成される。
これらは DDL 上は `CREATE INDEX` ではなく、`PRIMARY KEY` または `UNIQUE` 制約として定義する。
同一カラムに通常インデックスを重複して作成しない。

| テーブル              | インデックス対象カラム           | 種別   | 目的                                                                                       |
| --------------------- | -------------------------------- | ------ | ------------------------------------------------------------------------------------------ |
| users                 | keycloak_user_id                 | UNIQUE | KeycloakユーザーIDの重複を防止し、1つのKeycloakユーザーを1人の業務ユーザーに対応させるため |
| restaurant_tables     | qr_token                         | UNIQUE | 固定QRトークンの重複を防止し、QRコードから1つの客席テーブルを一意に特定するため            |
| restaurant_tables     | store_id, table_number           | UNIQUE | 同一店舗内でテーブル番号の重複を防止するため                                               |
| visit_sessions        | visit_token                      | UNIQUE | 来店トークンの重複を防止し、1つの来店セッションを一意に特定するため                        |
| menu_categories       | store_id, name                   | UNIQUE | 同一店舗内でカテゴリ名の重複を防止するため                                                 |
| menus                 | store_id, menu_category_id, name | UNIQUE | 同一店舗・同一カテゴリ内でメニュー名の重複を防止するため                                   |
| payments              | visit_session_id                 | UNIQUE | 1つの来店セッションに対して支払い情報が重複作成されることを防止するため                    |
| daily_sales_summaries | store_id, sales_date             | UNIQUE | 同一店舗・同一売上日の集計結果が重複作成されることを防止するため                           |

#### 6.2.2 検索性能向上目的のインデックス

以下のインデックスは、データ件数が増加しやすいトランザクション系テーブルに対して、一覧取得、詳細取得、注文確認、売上集計などの検索性能を向上させるために設定する。
マスタ系テーブルは想定レコード数が少ないため、UNIQUE制約に伴うインデックスを除き、通常インデックスは原則追加しない。

| テーブル       | インデックス対象カラム | 種別 | 目的                                               |
| -------------- | ---------------------- | ---- | -------------------------------------------------- |
| visit_sessions | table_id, status       | 複合 | テーブルに紐づく有効な来店セッションを検索するため |
| orders         | visit_session_id       | 通常 | 来店セッションに紐づく注文一覧を取得するため       |
| orders         | store_id, created_at   | 複合 | 店舗別の注文一覧や期間検索を行うため               |
| order_items    | order_id               | 通常 | 注文詳細取得時に注文明細を取得するため             |
| payments       | store_id, paid_at      | 複合 | 店舗別・期間別の売上集計を行うため                 |

## 7. 排他制御方針

### 7.1 排他制御の基本方針

ホールスタッフAがテーブル状態を更新する
ホールスタッフBも同じテーブル状態を更新する

厨房スタッフAが注文明細を COOKING にする
厨房スタッフBが同じ注文明細を READY にする

ホールスタッフAが会計処理する
ホールスタッフBも同じ来店セッションを会計処理する

このような場合、後から実行された更新が先に実行された更新を上書きすると、業務上の不整合が発生する。
そのため、更新対象テーブルに version カラムを持たせ、更新時の条件に version を含めることで、上書き更新を防ぎ、更新競合を検知する楽観ロック方式を採用する。

### 7.2 楽観ロック対象テーブル

特に競合が発生しやすいのは restaurant_tables、visit_sessions、orders、order_items、payments である。
stores、users、menu_categories、menus などのマスタ系テーブルについても、管理画面から更新される可能性があるため version カラムを利用する。

| テーブル          | 対象理由                                                     |
| ----------------- | ------------------------------------------------------------ |
| stores            | 店舗情報が本部管理者などにより同時更新される可能性があるため |
| users             | ユーザー情報や権限・所属店舗が管理者により更新されるため     |
| restaurant_tables | テーブル状態が複数スタッフにより更新される可能性があるため   |
| visit_sessions    | 来店セッション開始・終了・期限切れ処理で更新されるため       |
| menu_categories   | メニューカテゴリ情報が店長・本部管理者により更新されるため   |
| menus             | 店長・本部管理者が販売状態や価格を更新する可能性があるため   |
| orders            | 注文ステータスがスタッフ操作で更新されるため                 |
| order_items       | 調理ステータスが厨房・ホール操作で更新されるため             |
| payments          | 会計処理が重複実行される可能性があるため                     |

### 7.3 楽観ロックの更新方式

更新時の検索条件に `WHERE id = ? AND version = ?` を追加し、更新件数が 0 件の場合は楽観ロックエラーとして処理する。
また、更新の際には `version = version + 1` として、更新後のバージョンをインクリメントする。

### 7.4 楽観ロックエラー時の扱い

更新SQLの実行結果が 0 件の場合は、楽観ロックエラーとして扱う。
楽観ロックエラーが発生した場合は HTTP 409 Conflict を返却し、ユーザーに再度操作を促す。

### 7.5 トランザクション制御との関係

楽観ロックは単一レコードの更新処理に対して有効である。
複数のテーブルをまたぐ更新処理は、楽観ロックだけでなくトランザクション制御を併用する必要がある。

以下は同一トランザクションで実行する。

| 処理             | 同一トランザクションに含める更新                                  |
| ---------------- | ----------------------------------------------------------------- |
| テーブル利用開始 | restaurant_tables.status 更新、visit_sessions 作成                |
| 注文登録         | orders 登録、order_items 登録、orders.total_amount 設定           |
| 会計完了         | payments 更新、visit_sessions 終了、restaurant_tables.status 更新 |

## 8. 論理削除方針

### 8.1 基本方針

本システムでは、業務上の参照整合性を保つため、マスタ系データは原則として物理削除せず、deleted_atに削除した日時を設定することで論理削除とする。

論理削除済みのデータは通常の一覧・検索結果には表示しない。
ただし、過去注文や売り上げ集計から参照される可能性があるため、DB上のレコードは保持する。

### 8.2 論理削除対象テーブル

| テーブル          | 方針                            | 理由                                               |
| ----------------- | ------------------------------- | -------------------------------------------------- |
| stores            | deleted_at に削除日時を設定する | 過去注文・売上データが店舗を参照するため           |
| users             | deleted_at に削除日時を設定する | 作成者・更新者として過去データから参照されるため   |
| restaurant_tables | deleted_at に削除日時を設定する | 過去の来店セッション・注文がテーブルを参照するため |
| menu_categories   | deleted_at に削除日時を設定する | 過去メニューや管理履歴との整合性を保つため         |
| menus             | deleted_at に削除日時を設定する | 過去の注文明細がメニューを参照するため             |

### 8.3 論理削除しないテーブル

| テーブル              | 方針                                                 | 理由                                 |
| --------------------- | ---------------------------------------------------- | ------------------------------------ |
| visit_sessions        | status で ACTIVE / CLOSED / EXPIRED を管理する       | 来店履歴として保持するため           |
| orders                | status で ORDERED / CANCELLED / COMPLETED を管理する | 注文履歴・売上確認に必要なため       |
| order_items           | cooking_status で状態を管理する                      | 調理履歴・注文詳細として保持するため |
| payments              | status で PENDING / PAID / CANCELLED を管理する      | 会計履歴・売上集計の正本となるため   |
| daily_sales_summaries | 原則削除せず、再集計時は更新する                     | バッチ集計結果として保持するため     |
| batch_job_histories   | 削除せず履歴として保持する                           | バッチ実行結果の監査に必要なため     |

### 8.4 論理削除時の扱い

論理削除を行う場合は、対象レコードの deleted_at に現在日時を設定する。
通常の一覧取得・詳細検索では、deleted_at IS NULL のレコードのみを取得する。

論理削除済みデータは新規登録や更新の対象外とする。
ただし、過去注文、会計履歴、売上集計など、履歴参照に必要な場合は、論理削除済みデータも参照可能とする。

論理削除時は version を更新し、楽観ロックの対象とする。

論理削除対象テーブルは想定件数が少ないマスタ系テーブルであるため、初期設計では deleted_at 用の通常インデックスは作成しない。
将来的にデータ件数増加により検索性能が問題となる場合は、deleted_at を含む複合インデックスまたは部分インデックスを検討する。

※ メニューの一時的な販売停止は deleted_at ではなく menus.status により管理する。
deleted_at は、メニューを管理対象から外す場合に使用する。

## 9. 監査項目方針

### 9.1 基本方針

本システムでは、データの作成日時・更新日時を追跡するため、全テーブルに created_at / updated_at を保持する。
スタッフ操作により作成・更新されるデータについては、created_by / updated_by に users.id を保持し、操作ユーザーを追跡できるようにする。

### 9.2 作成日時・更新日時

created_at はレコード作成日時を表し、作成時に CURRENT_TIMESTAMP を設定する。
updated_at はレコード更新日時を表し、作成時には created_at と同じ日時を設定し、更新時には現在日時に更新する。

更新処理では updated_at を現在日時に更新する。

### 9.3 作成者・更新者

created_by / updated_by は全テーブルに一律で付与するのではなく、スタッフ操作により作成・更新されるデータに限定して保持する。

created_by / updated_by は users.id を参照する。
初期データ投入、来店客操作、バッチ処理など、操作ユーザーを users.id として特定できない場合は NULL を許容する。

orders は来店客操作により作成される場合があるため、created_by は NULL を許容する。
order_items は注文登録時に来店客操作で作成されるため created_by は保持せず、調理ステータス更新時のスタッフを updated_by で管理する。

### 9.4 論理削除日時

論理削除対象テーブルでは、deleted_at に削除日時を保持する。
deleted_at が NULL の場合は有効データ、NULL でない場合は論理削除済みデータとして扱う。

### 9.5 来店客操作・バッチ処理時の扱い

来店客は users テーブルで管理しないため、来店客操作の追跡は visit_session_id により来店単位で行う。

visit_sessions は来店単位の業務エンティティとして管理し、来店客・テーブル・状態・有効期限を追跡する。
テーブル利用開始を行ったスタッフの追跡は本ポートフォリオでは必須対象外とし、created_by / updated_by は保持しない。

daily_sales_summaries はバッチにより作成・更新される集計結果であり、batch_job_histories によりバッチ実行履歴を管理するため created_by / updated_by は保持しない。
batch_job_histories はバッチ実行そのものの履歴であるため、ユーザーIDによる作成者・更新者は保持しない。

## 10. ステータス定義

### 10.1 ステータス管理方針

本システムでは、各業務データの状態を status カラムで管理する。
status は VARCHAR(30) とし、アプリケーション側では enum として扱う。

DB定義では VARCHAR として保持し、ステータス値の妥当性はアプリケーションの業務ロジックで検証する。
必要に応じて Flyway の CHECK 制約により、DBレベルでの制約追加も検討する。

### 10.2 restaurant_tables.status

| 値              | 意味     | 説明                                                  |
| --------------- | -------- | ----------------------------------------------------- |
| AVAILABLE       | 空席     | 来店客は注文不可。スタッフが利用開始できる            |
| OCCUPIED        | 利用中   | 有効な visit_session が存在する場合、来店客は注文可能 |
| PAYMENT_WAITING | 会計待ち | 追加注文不可。会計処理待ち                            |
| CLEANING        | 清掃中   | 注文不可。清掃完了後に AVAILABLE へ戻す               |

### 10.3 visit_sessions.status

| 値      | 意味     | 説明                                                       |
| ------- | -------- | ---------------------------------------------------------- |
| ACTIVE  | 有効     | 来店中。有効期限内かつテーブルが OCCUPIED の場合、注文可能 |
| CLOSED  | 終了     | 会計完了により終了済み。注文不可                           |
| EXPIRED | 期限切れ | 有効期限切れにより無効。注文不可                           |

### 10.4 menus.status

| 値        | 意味     | 説明                         |
| --------- | -------- | ---------------------------- |
| AVAILABLE | 販売中   | 注文可能なメニュー           |
| SUSPENDED | 販売停止 | 一時的に販売停止中。注文不可 |

### 10.5 orders.status

| 値        | 意味         | 説明                                                 |
| --------- | ------------ | ---------------------------------------------------- |
| ORDERED   | 注文受付済み | 来店客から注文が登録された状態                       |
| COMPLETED | 完了         | 注文全体の提供が完了した状態                         |
| CANCELLED | キャンセル   | 店舗スタッフが業務判断により注文をキャンセルした状態 |

### 10.6 order_items.cooking_status

| 値        | 意味         | 説明                                                     |
| --------- | ------------ | -------------------------------------------------------- |
| WAITING   | 未着手       | 厨房がまだ調理に着手していない状態                       |
| COOKING   | 調理中       | 調理中の状態                                             |
| READY     | 提供準備完了 | 調理完了し、提供可能な状態                               |
| SERVED    | 提供済み     | 来店客へ提供済みの状態                                   |
| CANCELLED | キャンセル   | 店舗スタッフが業務判断により注文明細をキャンセルした状態 |

### 10.7 payments.status

| 値        | 意味       | 説明                                 |
| --------- | ---------- | ------------------------------------ |
| PENDING   | 支払い待ち | 会計処理前、または支払い未完了の状態 |
| PAID      | 支払い完了 | 支払いが完了した状態                 |
| CANCELLED | キャンセル | 支払い処理がキャンセルされた状態     |

### 10.8 batch_job_histories.status

| 値        | 意味     | 説明                         |
| --------- | -------- | ---------------------------- |
| STARTED   | 実行中   | バッチ処理を開始した状態     |
| COMPLETED | 正常終了 | バッチ処理が正常終了した状態 |
| FAILED    | 異常終了 | バッチ処理が異常終了した状態 |