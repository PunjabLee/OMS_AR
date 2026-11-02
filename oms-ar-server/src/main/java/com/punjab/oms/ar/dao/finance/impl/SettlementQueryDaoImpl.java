package com.punjab.oms.ar.dao.finance.impl;
import com.punjab.oms.ar.dao.finance.SettlementQueryDao;import com.punjab.oms.ar.domain.entity.finance.*;import com.punjab.oms.ar.domain.bo.finance.SettlementDetailQueryBO;import com.punjab.oms.ar.mapper.finance.*;import com.baomidou.mybatisplus.extension.repository.CrudRepository;import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;import org.springframework.stereotype.Repository;import lombok.RequiredArgsConstructor;import java.util.List;
/** 详情保留REVERSED记录，不从当前余额重算历史分配。 @author Punjab */
@Repository @RequiredArgsConstructor public class SettlementQueryDaoImpl extends CrudRepository<ArSettlementMapper,ArSettlement> implements SettlementQueryDao {
 private final ArSettlementAllocationMapper allocations;private final ArBalanceSettlementMapper balances;private final ArBalanceUsageMapper usages;
 public ArSettlement detail(SettlementDetailQueryBO q){return baseMapper.selectOne(new LambdaQueryWrapper<ArSettlement>().eq(ArSettlement::getId,q.id()).eq(ArSettlement::getLegalEntityId,q.legalEntityId()).eq(ArSettlement::getDeleteStatus,false));}
 public List<ArSettlementAllocation> allocations(Long id){return allocations.selectList(new LambdaQueryWrapper<ArSettlementAllocation>().eq(ArSettlementAllocation::getSettlementId,id).eq(ArSettlementAllocation::getDeleteStatus,false).orderByAsc(ArSettlementAllocation::getSequenceNo));}
 public ArBalanceSettlement balance(Long id){return balances.selectOne(new LambdaQueryWrapper<ArBalanceSettlement>().eq(ArBalanceSettlement::getSettlementId,id).eq(ArBalanceSettlement::getDeleteStatus,false));}
 public List<ArBalanceUsage> usages(Long id){return usages.selectList(new LambdaQueryWrapper<ArBalanceUsage>().eq(ArBalanceUsage::getSettlementId,id).eq(ArBalanceUsage::getDeleteStatus,false).orderByAsc(ArBalanceUsage::getSequenceNo).orderByAsc(ArBalanceUsage::getId));}
}
