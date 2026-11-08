package com.punjab.oms.ar.service.finance;

import com.punjab.oms.ar.base.*;
import com.punjab.oms.ar.domain.query.finance.CashHistoryQuery;
import com.punjab.oms.ar.domain.vo.finance.*;

/** 现金批次及流水查询。 @author Punjab */
public interface CashHistoryService {
    PageResult<CashLotResponse> lots(CashHistoryQuery query);
    PageResult<CashEntryResponse> entries(CashHistoryQuery query);
}
