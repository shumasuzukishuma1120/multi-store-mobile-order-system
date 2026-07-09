# multi-store-mobile-order-system

## 概要
複数店舗向けモバイルオーダー・売上管理 REST API システムです。
来店客はテーブル上の固定QRコードから注文画面へアクセスし、メニュー表示、注文登録、注文履歴確認を行います。
店舗スタッフ・店長・本部管理者は共通の業務管理画面を利用し、ログインユーザーのロールに応じて表示メニューと操作可能機能を切り替えます。

クライアントは以下の2系統です。

| クライアント | 利用者 | 主な機能 |
| --- | --- | --- |
| 来店客向け注文画面 | 来店客 | QR注文、メニュー表示、注文登録、注文履歴確認 |
| 業務管理画面 | 店舗スタッフ、店長、本部管理者 | テーブル管理、注文管理、会計処理、メニュー管理、売上管理、店舗管理、バッチ管理 |

画面上で機能を非表示にするだけではなく、API側でも必ずロール認可と店舗単位アクセス制御を行います。

## 使用技術
- Java
- Spring Boot
- Spring Security
- OAuth2.0 / OpenID Connect
- Amazon Cognito
- JWT
- MyBatis
- PostgreSQL
- Flyway
- Spring Batch
- Docker Compose
- AWS ECS / RDS / S3 / CloudWatch / ALB

## 主な機能
- QR注文
- 来店セッション管理
- テーブルステータス管理
- 注文管理
- 会計処理
- メニュー管理
- 売上管理
- 店舗管理
- 日次売上集計バッチ
- Amazon Cognito + JWT による認証・認可

## ロール別機能

| ロール | 業務管理画面で利用できる主な機能 |
| --- | --- |
| STORE_STAFF | テーブル管理、注文確認、調理ステータス更新、会計処理 |
| STORE_MANAGER | STORE_STAFF機能、自店舗売上参照、メニュー登録・更新 |
| ADMIN | 全店舗売上、店舗管理、スタッフ管理、バッチ管理 |

## 起動方法
※後で記載

## 設計書
- [基本設計](./01_basic_design.md)
- [DB設計](./02_db_design.md)
- [API設計](./03_api_design.md)
- [セキュリティ設計](./04_security_design.md)
