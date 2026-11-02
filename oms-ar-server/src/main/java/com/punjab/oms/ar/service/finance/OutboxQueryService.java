package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.base.PageResult;import com.punjab.oms.ar.domain.query.finance.*;import com.punjab.oms.ar.domain.vo.finance.*;
/** 通知运营查询，与派发内部端口分离。 @author Punjab */
public interface OutboxQueryService {PageResult<OutboxResponse> page(OutboxPageQuery query);OutboxDetailResponse detail(OutboxDetailQuery query);}
