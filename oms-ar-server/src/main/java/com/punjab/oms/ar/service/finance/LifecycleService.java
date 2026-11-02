package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.base.PageResult;import com.punjab.oms.ar.domain.query.finance.LifecycleQuery;import com.punjab.oms.ar.domain.vo.finance.LifecycleResponse;
/** 应收生命周期查询边界。 @author Punjab */
public interface LifecycleService {PageResult<LifecycleResponse> page(LifecycleQuery query);}
