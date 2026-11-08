package com.punjab.oms.ar.dao.finance;

import com.punjab.oms.ar.base.PageResult;
import com.punjab.oms.ar.domain.bo.finance.*;
import com.punjab.oms.ar.mapper.finance.CashHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** 批次及流水使用同一范围和分页条件。 @author Punjab */
@Repository @RequiredArgsConstructor
public class CashHistoryDao {
    private final CashHistoryMapper mapper;
    public PageResult<CashLotRow> lots(CashHistoryScope scope,int page) {
        return new PageResult<>(mapper.countLots(scope),page,scope.size(),mapper.lots(scope));
    }
    public PageResult<CashEntryRow> entries(CashHistoryScope scope,int page) {
        return new PageResult<>(mapper.countEntries(scope),page,scope.size(),mapper.entries(scope));
    }
}
