package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.base.PageResult;import com.punjab.oms.ar.domain.query.finance.ReconciliationQuery;import com.punjab.oms.ar.domain.vo.finance.ReconciliationResponse;
/** 只读金额对账服务。 @author Punjab */
public interface ReconciliationService {PageResult<ReconciliationResponse> page(ReconciliationQuery query); PageResult<ReconciliationResponse> cashPage(ReconciliationQuery query);
}
