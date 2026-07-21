package com.portfolio.mobileorder.table.service;

import com.portfolio.mobileorder.table.dto.StartTableUseRequest;
import com.portfolio.mobileorder.table.dto.StartTableUseResponse;
import org.springframework.transaction.annotation.Transactional;

/**
 * テーブル操作に関するサービスインターフェース。
 */
public interface TableUseService {

    /**
     * テーブル利用開始処理を行う。
     *
     * @param storeId 店舗ID
     * @param tableId テーブルID
     * @param request テーブル利用開始リクエスト
     * @return テーブル利用開始レスポンス
     */
    @Transactional
    public StartTableUseResponse startTableUse(
            Long storeId,
            Long tableId,
            StartTableUseRequest request
    );
}
