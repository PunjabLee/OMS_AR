package com.punjab.oms.ar.dao.finance;
import com.punjab.oms.ar.domain.entity.finance.*;import com.punjab.oms.ar.domain.bo.finance.OutboxRetryPageQueryBO;import com.punjab.oms.ar.base.PageResult;
/** 通知及其重试审计同属财务通知聚合。 @author Punjab */
public interface OutboxRetryDao {ArOutbox notification(Long id,Long scope,boolean lock);boolean reset(Long id,Long scope,Long version,String actor);boolean create(ArOutboxRetry value);PageResult<ArOutboxRetry> history(OutboxRetryPageQueryBO query);}
