package com.portfolio.mobileorder.table.controller;

import com.portfolio.mobileorder.table.dto.StartTableUseRequest;
import com.portfolio.mobileorder.table.dto.StartTableUseResponse;
import com.portfolio.mobileorder.table.service.TableUseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * テーブル操作に関するコントローラクラス。
 */
@RestController
@RequestMapping("/stores/{storeId}/tables/{tableId}/visit-sessions")
@RequiredArgsConstructor
public class TableUseController {

    private final TableUseService tableUseService;

    /**
     * テーブル利用開始API。
     *
     * @param storeId 店舗ID
     * @param tableId テーブルID
     * @param request テーブル利用開始リクエスト
     * @return テーブル利用開始レスポンス
     */
    @PostMapping
    public ResponseEntity<StartTableUseResponse> startTableUse(
            @PathVariable Long storeId,
            @PathVariable Long tableId,
            @Valid @RequestBody StartTableUseRequest request
    ) {
        StartTableUseResponse response = tableUseService.startTableUse(storeId, tableId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
