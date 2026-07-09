## ローカル開発環境

本プロジェクトでは、ビルドツールとして Gradle を使用する。
Gradle Wrapper をリポジトリに含めることで、開発者ごとの Gradle インストール状況に依存せず、同一バージョンの Gradle でビルド、テスト、アプリケーション起動を行えるようにする。

ローカル開発環境では、Docker Compose を使用して PostgreSQL を起動する。
Spring Boot API は、初期開発ではローカルJVM上で起動し、必要に応じて Docker コンテナ上でも起動できる構成とする。

DBマイグレーションには Flyway を使用し、アプリケーション起動時に `src/main/resources/db/migration` 配下のSQLを適用する。