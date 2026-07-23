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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 顧客がテーブルにアクセスするための情報を検証するための実装クラス。
 */
@Component
@RequiredArgsConstructor
public class CustomerTableAccessValidatorImpl implements CustomerTableAccessValidator {

    private final RestaurantTableMapper restaurantTableMapper;
    private final VisitSessionMapper visitSessionMapper;

    /**
     * 顧客がテーブルにアクセスするための情報を検証するメソッド。
     *
     * @param qrToken QRコードトークン
     * @param visitToken 来店トークン
     * @return 顧客のテーブルアクセス情報
     */
    @Override
    public CustomerTableAccess validate(String qrToken, String visitToken) {

        RestaurantTable restaurantTable = restaurantTableMapper.findByQrToken(qrToken)
                .orElseThrow(() -> new NotFoundException(ErrorCodeConst.TABLE_NOT_FOUND, "指定されたテーブルが存在しません。"));

        if (restaurantTable.getStatus() != TableStatus.OCCUPIED) {
            throw new ConflictException(ErrorCodeConst.TABLE_NOT_OCCUPIED, "指定されたテーブルは使用中ではありません。");
        }

        VisitSession visitSession = visitSessionMapper.findByTableIdAndVisitToken(restaurantTable.getId(), visitToken)
                .orElseThrow(() -> new ForbiddenException(ErrorCodeConst.VISIT_SESSION_ACCESS_DENIED, "注文権限がありません。"));

        if (visitSession.getStatus() != VisitSessionStatus.ACTIVE) {
            throw new ConflictException(ErrorCodeConst.VISIT_SESSION_NOT_ACTIVE, "指定された来店セッションはアクティブではありません。");
        }

        LocalDateTime now = LocalDateTime.now();

        if (!visitSession.getExpiresAt().isAfter(now)) {
            throw new ConflictException(ErrorCodeConst.VISIT_SESSION_EXPIRED, "指定された来店セッションは期限切れです。");
        }

        return new CustomerTableAccess(
                restaurantTable.getStoreId(),
                restaurantTable.getId(),
                visitSession.getId()
        );
    }
}
