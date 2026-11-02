package com.punjab.oms.ar.dao.finance;
import com.punjab.oms.ar.domain.bo.finance.*;import com.punjab.oms.ar.domain.entity.finance.ArOutbox;import com.punjab.oms.ar.base.PageResult;
/** 通知运营查询，无运行状态修改。 @author Punjab */
public interface OutboxQueryDao {PageResult<ArOutbox> page(OutboxPageQueryBO query);ArOutbox detail(OutboxDetailQueryBO query);}
