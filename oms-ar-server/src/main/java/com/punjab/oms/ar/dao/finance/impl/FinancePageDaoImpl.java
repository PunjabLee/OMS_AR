package com.punjab.oms.ar.dao.finance.impl;
import com.punjab.oms.ar.dao.finance.FinancePageDao;import com.punjab.oms.ar.base.PageResult;import com.punjab.oms.ar.domain.entity.finance.*;import com.punjab.oms.ar.domain.bo.finance.*;import com.punjab.oms.ar.mapper.finance.*;import com.baomidou.mybatisplus.extension.repository.CrudRepository;import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;import org.springframework.stereotype.Repository;import lombok.RequiredArgsConstructor;
/** 同一过滤对象计数及取页；分页数字仅来自已校验整数，固定ID倒序。 @author Punjab */
@Repository @RequiredArgsConstructor public class FinancePageDaoImpl extends CrudRepository<ArSettlementMapper,ArSettlement> implements FinancePageDao {
 private final ArCustomerAccountMapper accounts;private final ArBalanceLotMapper lots;
 public PageResult<ArSettlement> settlements(SettlementPageQueryBO q){var filter=new LambdaQueryWrapper<ArSettlement>().eq(ArSettlement::getDeleteStatus,false).eq(ArSettlement::getLegalEntityId,q.getLegalEntityId());
 filter.eq(q.getCustomerId()!=null,ArSettlement::getCustomerId,q.getCustomerId());
 filter.eq(q.getCurrency()!=null,ArSettlement::getCurrency,q.getCurrency());
 filter.eq(q.getStatus()!=null,ArSettlement::getStatus,q.getStatus());
 filter.eq(q.getSettlementType()!=null,ArSettlement::getSettlementType,q.getSettlementType());
 filter.eq(q.getReceiptId()!=null,ArSettlement::getReceiptId,q.getReceiptId());
 long total=baseMapper.selectCount(filter);filter.orderByDesc(ArSettlement::getId).last("LIMIT "+q.getSize()+" OFFSET "+(((long)q.getPage()-1)*q.getSize()));return new PageResult<>(total,q.getPage(),q.getSize(),baseMapper.selectList(filter));}
 public PageResult<ArCustomerAccount> accounts(AccountPageQueryBO q){var filter=new LambdaQueryWrapper<ArCustomerAccount>().eq(ArCustomerAccount::getDeleteStatus,false).eq(ArCustomerAccount::getLegalEntityId,q.getLegalEntityId());
 filter.eq(q.getCustomerId()!=null,ArCustomerAccount::getCustomerId,q.getCustomerId());
 filter.eq(q.getCurrency()!=null,ArCustomerAccount::getCurrency,q.getCurrency());
 long total=accounts.selectCount(filter);filter.orderByDesc(ArCustomerAccount::getId).last("LIMIT "+q.getSize()+" OFFSET "+(((long)q.getPage()-1)*q.getSize()));return new PageResult<>(total,q.getPage(),q.getSize(),accounts.selectList(filter));}
 public PageResult<ArBalanceLot> lots(LotPageQueryBO q){var filter=new LambdaQueryWrapper<ArBalanceLot>().eq(ArBalanceLot::getDeleteStatus,false).eq(ArBalanceLot::getLegalEntityId,q.getLegalEntityId());
 filter.eq(q.getCustomerId()!=null,ArBalanceLot::getCustomerId,q.getCustomerId());
 filter.eq(q.getCurrency()!=null,ArBalanceLot::getCurrency,q.getCurrency());
 filter.eq(q.getStatus()!=null,ArBalanceLot::getStatus,q.getStatus());
 filter.eq(q.getAccountId()!=null,ArBalanceLot::getAccountId,q.getAccountId());
 filter.eq(q.getSourceLineId()!=null,ArBalanceLot::getSourceLineId,q.getSourceLineId());
 long total=lots.selectCount(filter);filter.orderByDesc(ArBalanceLot::getId).last("LIMIT "+q.getSize()+" OFFSET "+(((long)q.getPage()-1)*q.getSize()));var rows=lots.selectList(filter);
 if(!rows.isEmpty()){
  var ids=rows.stream().map(ArBalanceLot::getAccountId).distinct().toList();
  var owners=new java.util.HashMap<Long,Long>();
  accounts.selectByIds(ids).forEach(a->owners.put(a.getId(),a.getCashAccountId()));
  rows.forEach(l->l.setCashAccountId(owners.get(l.getAccountId())));
 }
 return new PageResult<>(total,q.getPage(),q.getSize(),rows);}
}
