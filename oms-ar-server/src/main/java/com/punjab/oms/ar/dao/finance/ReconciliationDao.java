package com.punjab.oms.ar.dao.finance;
import com.punjab.oms.ar.domain.bo.finance.ReconciliationQueryBO;import com.punjab.oms.ar.domain.entity.finance.ArReconciliationDifference;import com.punjab.oms.ar.base.PageResult;
/** 对账结果持久层接口。 @author Punjab */
public interface ReconciliationDao {PageResult<ArReconciliationDifference> page(ReconciliationQueryBO query);}
