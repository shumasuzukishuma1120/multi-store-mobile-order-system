package com.portfolio.mobileorder.table.mapper;

import com.portfolio.mobileorder.table.model.RestaurantTable;
import com.portfolio.mobileorder.table.model.TableStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * restaurant_tables の MyBatis Mapper インターフェース。
 */
@Mapper
public interface RestaurantTableMapper {

    /**
     * 指定IDとのテーブルを取得する。
     * @param id テーブルID
     * @return テーブル情報 存在しない場合は空
     */
    Optional<RestaurantTable> findById(@Param("id") Long id);

    /**
     * QRコードトークンに対応するテーブルを取得する。
     * @param qrToken QRコードトークン
     * @return テーブル情報 存在しない場合は空
     */
    Optional<RestaurantTable> findByQrToken(@Param("qrToken") String qrToken);

    /**
     * 指定IDとversionが一致するテーブルの状態を更新する。
     * @param id テーブルID
     * @param status 変更後ステータス
     * @param version 更新前バージョン
     * @return 更新件数
     */
    int updateStatusByIdAndVersion(
            @Param("id") Long id,
            @Param("status") TableStatus status,
            @Param("version") Integer version
    );
}
