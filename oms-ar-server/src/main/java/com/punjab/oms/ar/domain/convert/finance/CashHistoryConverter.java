package com.punjab.oms.ar.domain.convert.finance;

import com.punjab.oms.ar.domain.bo.finance.*;
import com.punjab.oms.ar.domain.query.finance.CashHistoryQuery;
import com.punjab.oms.ar.domain.vo.finance.*;
import org.mapstruct.Mapper;

/** 转换业务查询范围、批次及流水响应。 @author Punjab */
@Mapper(componentModel="spring")
public interface CashHistoryConverter {
    CashLotResponse toResponse(CashLotRow row);
    CashEntryResponse toResponse(CashEntryRow row);
    default CashHistoryScope toBO(CashHistoryQuery q) {
        return new CashHistoryScope(q.getLegalEntityId(),q.getAccountId(),((long)q.getPage()-1)*q.getSize(),q.getSize());
    }
}
