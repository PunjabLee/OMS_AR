package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.domain.query.finance.SettlementDetailQuery;import com.punjab.oms.ar.domain.vo.finance.SettlementResponse;
/** 核销详情查询边界。 @author Punjab */
public interface SettlementQueryService {SettlementResponse detail(SettlementDetailQuery query);}
