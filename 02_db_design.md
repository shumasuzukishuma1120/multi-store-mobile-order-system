# DB設計

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

| カラム名   | 型           | PK  | FK  | NOT NULL | UNIQUE | DEFAULT           | 説明                   |
| ---------- | ------------ | --- | --- | -------- | ------ | ----------------- | ---------------------- |
| id         | BIGINT       | Y   | N   | Y        | Y      | IDENTITY          | 店舗ID                 |
| name       | VARCHAR(100) | N   | N   | Y        | Y      |                   | 店舗名                 |
| created_at | TIMESTAMP    | N   | N   | Y        | N      | CURRENT_TIMESTAMP | 作成日時               |
| updated_at | TIMESTAMP    | N   | N   | Y        | N      | CURRENT_TIMESTAMP | 更新日時               |
| delete_at  | TIMESTAMP    | N   | N   | N        | N      | NULL              | 論理削除日時           |
| version    | INTEGER      | N   | N   | Y        | N      | 0                 | 楽観ロック用バージョン |

### 3.2 users

| カラム名   | 型           | PK  | FK  | NOT NULL | UNIQUE | DEFAULT           | 説明                   |
| ---------- | ------------ | --- | --- | -------- | ------ | ----------------- | ---------------------- |
| id         | BIGINT       | Y   | N   | Y        | Y      | IDENTITY          | ユーザーID             |
| name       | VARCHAR(100) | N   | N   | Y        | Y      |                   | ユーザー名             |
| store_id   | BIGINT       | N   | Y   | Y        | N      |                   | 所属店舗ID             |
| created_at | TIMESTAMP    | N   | N   | Y        | N      | CURRENT_TIMESTAMP | 作成日時               |
| updated_at | TIMESTAMP    | N   | N   | Y        | N      | CURRENT_TIMESTAMP | 更新日時               |
| delete_at  | TIMESTAMP    | N   | N   | N        | N      | NULL              | 論理削除日時           |
| version    | INTEGER      | N   | N   | Y        | N      | 0                 | 楽観ロック用バージョン |

### 3.3 restaurant-tables

| カラム名   | 型          | PK  | FK  | NOT NULL | UNIQUE | DEFAULT           | 説明                   |
| ---------- | ----------- | --- | --- | -------- | ------ | ----------------- | ---------------------- |
| id         | BIGINT      | Y   | N   | Y        | Y      | IDENTITY          | テーブルID             |
| status     | VARCHAR(30) | N   | N   | Y        | N      | 'AVAILABLE'       | テーブルステータス     |
| store_id   | BIGINT      | N   | Y   | Y        | N      |                   | 所属店舗ID             |
| created_at | TIMESTAMP   | N   | N   | Y        | N      | CURRENT_TIMESTAMP | 作成日時               |
| updated_at | TIMESTAMP   | N   | N   | Y        | N      | CURRENT_TIMESTAMP | 更新日時               |
| delete_at  | TIMESTAMP   | N   | N   | N        | N      | NULL              | 論理削除日時           |
| version    | INTEGER     | N   | N   | Y        | N      | 0                 | 楽観ロック用バージョン |

### 3.4 visit_sessions

| カラム名   | 型          | PK  | FK  | NOT NULL | UNIQUE | DEFAULT           | 説明                     |
| ---------- | ----------- | --- | --- | -------- | ------ | ----------------- | ------------------------ |
| id         | BIGINT      | Y   | N   | Y        | Y      | IDENTITY          | 来店セッションID         |
| status     | VARCHAR(30) | N   | N   | Y        | N      | 'ACTIVE'          | 来店セッションステータス |
| store_id   | BIGINT      | N   | Y   | Y        | N      |                   | 所属店舗ID               |
| created_at | TIMESTAMP   | N   | N   | Y        | N      | CURRENT_TIMESTAMP | 作成日時                 |
| updated_at | TIMESTAMP   | N   | N   | Y        | N      | CURRENT_TIMESTAMP | 更新日時                 |
| version    | INTEGER     | N   | N   | Y        | N      | 0                 | 楽観ロック用バージョン   |

### 3.5 menu_categories

| カラム名   | 型           | PK  | FK  | NOT NULL | UNIQUE | DEFAULT           | 説明                   |
| ---------- | ------------ | --- | --- | -------- | ------ | ----------------- | ---------------------- |
| id         | BIGINT       | Y   | N   | Y        | Y      | IDENTITY          | メニューカテゴリID     |
| name       | VARCHAR(100) | N   | N   | Y        | Y      |                   | メニューカテゴリ名     |
| store_id   | BIGINT       | N   | Y   | Y        | N      |                   | 所属店舗ID             |
| created_at | TIMESTAMP    | N   | N   | Y        | N      | CURRENT_TIMESTAMP | 作成日時               |
| updated_at | TIMESTAMP    | N   | N   | Y        | N      | CURRENT_TIMESTAMP | 更新日時               |
| delete_at  | TIMESTAMP    | N   | N   | N        | N      | NULL              | 論理削除日時           |
| version    | INTEGER      | N   | N   | Y        | N      | 0                 | 楽観ロック用バージョン |

### 3.6 menus

| カラム名         | 型            | PK  | FK  | NOT NULL | UNIQUE | DEFAULT           | 説明                   |
| ---------------- | ------------- | --- | --- | -------- | ------ | ----------------- | ---------------------- |
| id               | BIGINT        | Y   | N   | Y        | Y      | IDENTITY          | メニューID             |
| name             | VARCHAR(100)  | N   | N   | Y        | Y      |                   | メニュー名             |
| menu_category_id | BIGINT        | N   | Y   | Y        | N      |                   | メニューカテゴリID     |
| price            | DECIMAL(10,2) | N   | N   | Y        | N      |                   | 価格                   |
| created_at       | TIMESTAMP     | N   | N   | Y        | N      | CURRENT_TIMESTAMP | 作成日時               |
| updated_at       | TIMESTAMP     | N   | N   | Y        | N      | CURRENT_TIMESTAMP | 更新日時               |
| delete_at        | TIMESTAMP     | N   | N   | N        | N      | NULL              | 論理削除日時           |
| version          | INTEGER       | N   | N   | Y        | N      | 0                 | 楽観ロック用バージョン |

### 3.7 orders

| カラム名            | 型          | PK  | FK  | NOT NULL | UNIQUE | DEFAULT           | 説明                   |
| ------------------- | ----------- | --- | --- | -------- | ------ | ----------------- | ---------------------- |
| id                  | BIGINT      | Y   | N   | Y        | Y      | IDENTITY          | 注文ID                 |
| visit_session_id    | BIGINT      | N   | Y   | Y        | N      |                   | 来店セッションID       |
| restaurant_table_id | BIGINT      | N   | Y   | Y        | N      |                   | レストランテーブルID   |
| status              | VARCHAR(30) | N   | N   | Y        | N      | 'ORDERED'         | 注文ステータス         |
| created_at          | TIMESTAMP   | N   | N   | Y        | N      | CURRENT_TIMESTAMP | 作成日時               |
| updated_at          | TIMESTAMP   | N   | N   | Y        | N      | CURRENT_TIMESTAMP | 更新日時               |
| delete_at           | TIMESTAMP   | N   | N   | N        | N      | NULL              | 論理削除日時           |
| version             | INTEGER     | N   | N   | Y        | N      | 0                 | 楽観ロック用バージョン |

### 3.8 order_items

### 3.9 payments

### 3.10 daily_sales_summaries

### 3.11 batch_job_histories

## 4. リレーション設計

## 5. 制約設計

## 6. インデックス設計

## 7. 排他制御方針

## 8. 論理削除方針

## 9. 監査項目方針

詳細なテーブル定義は `docs/db/README.md` を参照。