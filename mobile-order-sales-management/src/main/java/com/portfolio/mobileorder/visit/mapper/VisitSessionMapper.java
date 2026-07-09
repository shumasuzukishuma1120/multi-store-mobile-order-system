package com.portfolio.mobileorder.visit.mapper;

import com.portfolio.mobileorder.visit.model.VisitSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

/**
 * visit_sessions の MyBatis Mapper インターフェース。
 */
@Mapper
public interface VisitSessionMapper {
    /**
     * 指定テーブルIDに対してアクティブな来店セッションが存在するかを確認する。
     * @param tableId テーブルID
     * @return アクティブな来店セッションが存在する場合は true
     */
    boolean existActiveByTableId(@Param("id") Long tableId);

    /**
     * 新しい来店セッションを挿入する。
     * @param visitSession 登録する来店セッション
     * @return 登録件数
     */
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(VisitSession visitSession);
}
