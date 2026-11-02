package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.domain.dto.finance.OutboxRetryRequest;import com.punjab.oms.ar.domain.query.finance.OutboxRetryPageQuery;import com.punjab.oms.ar.domain.vo.finance.OutboxRetryResponse;import com.punjab.oms.ar.base.PageResult;
/** 人工重试及独立操作审计，不生成递归通知。 @author Punjab */
public interface OutboxRetryService {Long retry(OutboxRetryRequest request);PageResult<OutboxRetryResponse> history(OutboxRetryPageQuery query);}
