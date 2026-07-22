package com.portfolio.mobileorder.customer.validation.impl;

import com.portfolio.mobileorder.common.exception.ConflictException;
import com.portfolio.mobileorder.common.exception.ErrorCodeConst;
import com.portfolio.mobileorder.common.exception.ForbiddenException;
import com.portfolio.mobileorder.common.exception.NotFoundException;
import com.portfolio.mobileorder.customer.model.CustomerTableAccess;
import com.portfolio.mobileorder.customer.validation.CustomerTableAccessValidator;
import com.portfolio.mobileorder.table.mapper.RestaurantTableMapper;
import com.portfolio.mobileorder.table.model.RestaurantTable;
import com.portfolio.mobileorder.table.model.TableStatus;
import com.portfolio.mobileorder.visit.mapper.VisitSessionMapper;
import com.portfolio.mobileorder.visit.model.VisitSession;
import com.portfolio.mobileorder.visit.model.VisitSessionStatus;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 顧客がテーブルにアクセスするための情報を検証するための実装クラス。
 */
@RequiredArgsConstructor
public class CustomerTableAccessValidatorImpl implements CustomerTableAccessValidator {

    private final RestaurantTableMapper restaurantTableMapper;
    private final VisitSessionMapper visitSessionMapper;

    @Override
    /**
     * 顧客がテーブルにアクセスするための情報を検証するメソッド。
     * @param qrToken QRコードトークン
     * @param visitToken 訪問トークン
     * @return 顧客のテーブルアクセス情報
     */
    public CustomerTableAccess validate(String qrToken, String visitToken) {

        RestaurantTable restaurantTable = restaurantTableMapper.findByQrToken(qrToken)
                .orElseThrow(() -> new NotFoundException("TABLE_NOT_FOUND", "指定されたテーブルが存在しません。"));

        if (restaurantTable.getStatus() != TableStatus.OCCUPIED) {
            throw new ConflictException(ErrorCodeConst.TABLE_NOT_OCCUPIED, "指定されたテーブルは使用中ではありません。");
        }

        VisitSession visitSession = visitSessionMapper.findByTableIdAndVisitToken(restaurantTable.getId(), visitToken)
                .orElseThrow(() -> new ForbiddenException(ErrorCodeConst.VISIT_SESSION_ACCESS_DENIED, "指定された訪問トークンは無効です。"));

        if (visitSession.getStatus() != VisitSessionStatus.ACTIVE) {
            throw new ConflictException(ErrorCodeConst.VISIT_SESSION_NOT_ACTIVE, "指定された来店セッションはアクティブではありません。");
        }

        LocalDateTime now = LocalDateTime.now();

        if (!visitSession.getExpiresAt().isAfter(now)) {
            throw new ConflictException(ErrorCodeConst.VISIT_SESSION_EXPIRED, "指定された来店セッションは期限切れです。");
        }

        CustomerTableAccess customerTableAccess = new CustomerTableAccess(
                restaurantTable.getId(),
                restaurantTable.getStoreId(),
                visitSession.getId()
        );

        return customerTableAccess;
    }
}
