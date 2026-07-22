package com.portfolio.mobileorder.visit.mapper;

import com.portfolio.mobileorder.visit.model.VisitSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

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
    boolean existsActiveByTableId(@Param("tableId") Long tableId);

    /**
     * 指定テーブルIDと来店トークンに対応する来店セッションを取得する。
     * @param tableId テーブルID
     * @param visitToken 来店トークン
     * @return 来店セッション情報 存在しない場合は空
     */
    Optional<VisitSession> findByTableIdAndVisitToken(@Param("tableId") Long tableId, @Param("visitToken") String visitToken);

    /**
     * 新しい来店セッションを挿入する。
     * @param visitSession 登録する来店セッション
     * @return 登録件数
     */
    int insert(VisitSession visitSession);


}
