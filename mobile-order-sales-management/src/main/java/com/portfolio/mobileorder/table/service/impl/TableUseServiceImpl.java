package com.portfolio.mobileorder.table.service.impl;

import com.portfolio.mobileorder.common.exception.ConflictException;
import com.portfolio.mobileorder.common.exception.ForbiddenException;
import com.portfolio.mobileorder.common.exception.NotFoundException;
import com.portfolio.mobileorder.table.dto.StartTableUseRequest;
import com.portfolio.mobileorder.table.dto.StartTableUseResponse;
import com.portfolio.mobileorder.table.mapper.RestaurantTableMapper;
import com.portfolio.mobileorder.table.model.RestaurantTable;
import com.portfolio.mobileorder.table.model.TableStatus;
import com.portfolio.mobileorder.table.service.TableUserService;
import com.portfolio.mobileorder.visit.mapper.VisitSessionMapper;
import com.portfolio.mobileorder.visit.model.VisitSession;
import com.portfolio.mobileorder.visit.model.VisitSessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseBody;

@Service
@Transactional
@RequiredArgsConstructor
public class TableUseServiceImpl implements TableUserService {

    private final RestaurantTableMapper restaurantTableMapper;
    private final VisitSessionMapper visitSessionMapper;

    @Override
    public StartTableUseResponse startTableUse(Long storeId, Long tableId, StartTableUseRequest request) {

        RestaurantTable restaurantTable = restaurantTableMapper.findById(tableId)
                .orElseThrow(() -> new NotFoundException("TABLE_NOT_FOUND", "指定されたテーブルが存在しません。"));

        boolean existActiveByTableId = visitSessionMapper.existActiveByTableId(tableId);

        if (storeId.equals(restaurantTable.getStoreId())) {
            throw new ForbiddenException("FORBIDDEN", "指定されたテーブルにアクセスする権限がありません。");
        } else if (restaurantTable.getStatus() != TableStatus.AVAILABLE) {
            throw new ConflictException("TABLE_NOT_AVAILABLE", "指定されたテーブルは使用中です。");
        } else if (existActiveByTableId) {
            throw new ConflictException("ACTIVE_SESSION_EXISTS", "指定されたテーブルにはアクティブな来店セッションが存在します。");
        }

        //どうやって中身作る？
        VisitSession visitSession = new VisitSession();

        visitSession.setTableId(tableId);
        visitSession.setStatus(VisitSessionStatus.ACTIVE);
        visitSessionMapper.insert(visitSession);



        return null;
    }
}
