package com.portfolio.mobileorder.table.service;

import com.portfolio.mobileorder.table.dto.StartTableUseRequest;
import com.portfolio.mobileorder.table.dto.StartTableUseResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * テーブル
 */
public interface TableUserService {

    @Transactional
    public StartTableUseResponse startTableUse(
            Long storeId,
            Long tableId,
            StartTableUseRequest request
    );
}
