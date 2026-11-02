package com.punjab.oms.ar.dao.finance;
import com.punjab.oms.ar.domain.entity.finance.*;import com.punjab.oms.ar.domain.bo.finance.SettlementDetailQueryBO;import java.util.List;
/** 核销详情数据访问。 @author Punjab */
public interface SettlementQueryDao {ArSettlement detail(SettlementDetailQueryBO query);List<ArSettlementAllocation> allocations(Long id);ArBalanceSettlement balance(Long id);List<ArBalanceUsage> usages(Long id);}
