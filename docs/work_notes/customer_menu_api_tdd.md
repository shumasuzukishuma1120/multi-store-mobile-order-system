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
CustomerCategoryResponse getCategoriesForCustomer(String qrToken, String visitToken);
```

VisitTokenはCustomerTableAccessValidator的なものを作ってService内で呼び出して検証する

### Menu一覧レスポンスDTO案

``` java
CustomerCategoryResponse {
    List<Category> categories;
}

CustomerCategoryResponse.Category {
    Long categoryId;
    String categoryName;
    String imageUrl;
}
```

### Serviceで使うMapperメソッド案

``` java
RestaurantTableMapper.findByQrToken(String qrToken) : Optional<RestaurantTable>
CustomerCategoryMapper.findAvailableCategoriesByStoreId(Long storeId) : List<CustomerCategory>
VisitSessionMapper.findByTableIdAndVisitToken(Long tableId, String visitToken) : Optional<VisitSession>
```

### 共通Validator

``` java
CustomerTableAccess validate(String qrToken, String visitToken);
```

***共通チェック用DTO***

``` java
CustomerTableAccess {
    Long storeId;
    Long tableId;
    Long visitSessionId;
}
```

### クラス案

customer/
  controller/
    CustomerCategoryController
  service/
    CustomerCategoryService
  service/impl/
    CustomerCategoryServiceImpl
  dto/
    CustomerCategoryResponse
  model/
    CustomerTableAccess
  validation/
    CustomerTableAccessValidator
    impl/
      CustomerTableAccessValidatorImpl

menu/
  model/
    MenuCategory
  mapper/
    MenuCategoryMapper

### メソッドシグネチャ案

``` java
public interface CustomerCategoryService {
    CustomerCategoryResponse getCategoriesForCustomer(String qrToken, String visitToken);
}
```

##### Validator

``` java
public interface CustomerTableAccessValidator {
    CustomerTableAccess validate(String qrToken, String visitToken);
}
```

##### Mapper

``` java

RestaurantTableMapper.findByQrToken(String qrToken): Optional<RestaurantTable>

VisitSessionMapper.findByTableIdAndVisitToken(Long tableId, String visitToken): Optional<VisitSession>

MenuCategoryMapper.findAvailableCategoriesByStoreId(Long storeId): List<MenuCategory>

```

##### DTO

``` java
CustomerCategoryResponse {
    List<Category> categories;
}

Category {
    Long categoryId;
    String categoryName;
    String imageUrl;
}
```

##### Model

``` java
CustomerTableAccess {
    Long storeId;
    Long tableId;
    Long visitSessionId;
}

MenuCategory {
    Long id;
    Long storeId;
    String name;
    String imageUrl;
    LocalDateTime createdAt;
    Long createdBy;
    LocalDateTime updatedAt;
    Long updatedBy;
    LocalDateTime deletedAt;
    Integer version;
}
```

##### 異常系テストケース

1. qrTokenが紐づくテーブルが存在しない場合、404(TABLE_NOT_FOUND)エラーを返す
2. visitTokenが対象テーブルに紐づかない場合、403(VISIT_SESSION_ACCESS_DENIED)エラーを返す
3. テーブル状態がOCCUPIEDではない場合、409(TABLE_NOT_OCCUPIED)エラーを返す
4. 来店セッションがACTIVEではない場合、409(VISIT_SESSION_NOT_ACTIVE)エラーを返す
5. 来店セッションが期限切れの場合、409(VISIT_SESSION_EXPIRED)エラーを返す

##### Validatorの処理の順番

1. qrToken でテーブル取得
2. テーブルが存在しなければ TABLE_NOT_FOUND
3. テーブルステータスが OCCUPIED でなければ TABLE_NOT_OCCUPIED
4. tableId + visitToken で visitSession 取得
5. なければ VISIT_SESSION_ACCESS_DENIED
6. visitSession.status が ACTIVE でなければ VISIT_SESSION_NOT_ACTIVE
7. expiresAt が現在以前なら VISIT_SESSION_EXPIRED
8. CustomerTableAccess を返す