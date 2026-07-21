# TDDメモ

## 進め方

1. DTO案を決める
2. Mapperインターフェース案を決める
3. CustomerMenuServiceImplTest を先に書く
4. 失敗させる
5. Service実装
6. Controller MockMvcテスト
7. Controller実装

## まずやること

1. CustomerMenuService のメソッドシグネチャ案
2. Menu一覧レスポンスDTO案
3. Serviceで使うMapperメソッド案

### CustomerMenuService のメソッドシグネチャ案

``` java
CustomerMenuResponse getMenuByQrToken(String qrToken, );


```