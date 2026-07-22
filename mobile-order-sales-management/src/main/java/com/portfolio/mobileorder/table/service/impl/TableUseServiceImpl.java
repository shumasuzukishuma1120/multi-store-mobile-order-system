package com.portfolio.mobileorder.table.service.impl;

import com.portfolio.mobileorder.common.exception.ConflictException;
import com.portfolio.mobileorder.common.exception.ErrorCodeConst;
import com.portfolio.mobileorder.common.exception.ForbiddenException;
import com.portfolio.mobileorder.common.exception.NotFoundException;
import com.portfolio.mobileorder.table.dto.StartTableUseRequest;
import com.portfolio.mobileorder.table.dto.StartTableUseResponse;
import com.portfolio.mobileorder.table.mapper.RestaurantTableMapper;
import com.portfolio.mobileorder.table.model.RestaurantTable;
import com.portfolio.mobileorder.table.model.TableStatus;
import com.portfolio.mobileorder.table.service.TableUseService;
import com.portfolio.mobileorder.visit.mapper.VisitSessionMapper;
import com.portfolio.mobileorder.visit.model.VisitSession;
import com.portfolio.mobileorder.visit.model.VisitSessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * テーブル操作に関するサービス実装クラス。
 */
@Service
@RequiredArgsConstructor
public class TableUseServiceImpl implements TableUseService {

    private final RestaurantTableMapper restaurantTableMapper;
    private final VisitSessionMapper visitSessionMapper;

    /**
     * テーブル使用開始処理を行う。
     *
     * <p>対象テーブルが空席であること、ACTIVE な来店セッションが存在しないことを確認し、
     * テーブル状態を利用中に変更した上で来店セッションを作成する。</p>
     *
     * @param storeId 店舗ID
     * @param tableId テーブルID
     * @param request テーブル利用開始リクエスト
     * @return テーブル利用開始結果
     */
    @Override
    @Transactional
    public StartTableUseResponse startTableUse(Long storeId, Long tableId, StartTableUseRequest request) {

        RestaurantTable restaurantTable = restaurantTableMapper.findById(tableId)
                .orElseThrow(() -> new NotFoundException(ErrorCodeConst.TABLE_NOT_FOUND, "指定されたテーブルが存在しません。"));

        if (!storeId.equals(restaurantTable.getStoreId())) {
            throw new ForbiddenException(ErrorCodeConst.STORE_ACCESS_DENIED, "指定されたテーブルにアクセスする権限がありません。");
        }

        if (restaurantTable.getStatus() != TableStatus.AVAILABLE) {
            throw new ConflictException(ErrorCodeConst.TABLE_NOT_AVAILABLE, "指定されたテーブルは使用中です。");
        }

        boolean existsActiveByTableId = visitSessionMapper.existsActiveByTableId(tableId);

        if (existsActiveByTableId) {
            throw new ConflictException(ErrorCodeConst.ACTIVE_VISIT_SESSION_EXISTS, "指定されたテーブルにはアクティブな来店セッションが存在します。");
        }

        // 楽観ロックにより、画面表示時点のversionと一致する場合のみ更新する。
        int updateCount = restaurantTableMapper.updateStatusByIdAndVersion(
                restaurantTable.getId(), TableStatus.OCCUPIED, request.getVersion());

        //更新件数が0なら楽観ロック競合として409。
        if (updateCount == 0) {
            throw new ConflictException(
                    ErrorCodeConst.OPTIMISTIC_LOCK_CONFLICT, "テーブル情報がほかのユーザーにより更新されています。"
            );
        }

        // UPDATE により version が変わるため再取得する。
        RestaurantTable updatedRestaurantTable = restaurantTableMapper.findById(restaurantTable.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "更新後のテーブル情報が取得できません。tableId=" + restaurantTable.getId()
                ));

        String visitToken = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        VisitSession visitSession = new VisitSession();
        visitSession.setStoreId(storeId);
        visitSession.setTableId(tableId);
        visitSession.setVisitToken(visitToken);
        visitSession.setStatus(VisitSessionStatus.ACTIVE);
        visitSession.setStartedAt(now);
        visitSession.setEndedAt(null);
        visitSession.setExpiresAt(now.plusHours(6));
        visitSessionMapper.insert(visitSession);

        Long visitSessionId = visitSession.getId();

        StartTableUseResponse.CurrentVisitSession currentVisitSession
                = new StartTableUseResponse.CurrentVisitSession(
                        visitSessionId,
                visitSession.getVisitToken(),
                visitSession.getStatus(),
                visitSession.getStartedAt(),
                visitSession.getExpiresAt()
        );

        return new StartTableUseResponse(
                updatedRestaurantTable.getId(),
                updatedRestaurantTable.getStoreId(),
                updatedRestaurantTable.getTableNumber(),
                updatedRestaurantTable.getStatus(),
                updatedRestaurantTable.getVersion(),
                currentVisitSession
        );
    }
}
