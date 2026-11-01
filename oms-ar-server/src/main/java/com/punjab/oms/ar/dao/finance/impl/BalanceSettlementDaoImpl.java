package com.punjab.oms.ar.dao.finance.impl;
import com.punjab.oms.ar.dao.finance.BalanceSettlementDao;import com.punjab.oms.ar.domain.entity.finance.*;import com.punjab.oms.ar.mapper.finance.*;import java.util.List;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;import org.springframework.stereotype.Repository;import lombok.RequiredArgsConstructor;
/** 不含业务事务；账户锁由服务在批次锁之前取得。 @author Punjab */
@Repository @RequiredArgsConstructor
public class BalanceSettlementDaoImpl extends CrudRepository<ArBalanceSettlementMapper,ArBalanceSettlement> implements BalanceSettlementDao {
 private final ArBalanceUsageMapper usages;private final ArBalanceLotMapper lots;
 public boolean create(ArBalanceSettlement v){return baseMapper.insert(v)==1;}public boolean createUsage(ArBalanceUsage v){return usages.insert(v)==1;}
 public ArBalanceSettlement source(Long id){return baseMapper.selectOne(new LambdaQueryWrapper<ArBalanceSettlement>().eq(ArBalanceSettlement::getSettlementId,id).eq(ArBalanceSettlement::getDeleteStatus,false));}
 public List<ArBalanceUsage> usages(Long id){return usages.selectList(new LambdaQueryWrapper<ArBalanceUsage>().eq(ArBalanceUsage::getSettlementId,id).eq(ArBalanceUsage::getDeleteStatus,false).orderByAsc(ArBalanceUsage::getSequenceNo));}
 public List<ArBalanceLot> availableLots(Long accountId,boolean lock){return lots.available(accountId,lock);}
 public List<ArBalanceLot> lots(List<Long> ids,boolean lock){if(ids.isEmpty())return List.of();var q=new LambdaQueryWrapper<ArBalanceLot>().in(ArBalanceLot::getId,ids).eq(ArBalanceLot::getDeleteStatus,false).orderByAsc(ArBalanceLot::getId);if(lock)q.last("FOR UPDATE");return lots.selectList(q);}
}
