package com.punjab.oms.ar.dao.finance.impl;
import com.punjab.oms.ar.dao.finance.InvoiceEligibilityDao;import com.punjab.oms.ar.domain.entity.finance.*;import com.punjab.oms.ar.domain.bo.finance.InvoiceEligibilityQueryBO;import com.punjab.oms.ar.mapper.finance.*;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;import org.springframework.stereotype.Repository;import lombok.RequiredArgsConstructor;import java.util.List;
/** 只读联表，查询参数不直接使用Web Query。 @author Punjab */
@Repository @RequiredArgsConstructor
public class InvoiceEligibilityDaoImpl extends CrudRepository<InvoiceEligibilityMapper,ArLine> implements InvoiceEligibilityDao {
 private final ArGroupMapper groups;
 public List<ArGroup> groups(InvoiceEligibilityQueryBO q){return groups.selectList(new LambdaQueryWrapper<ArGroup>().in(ArGroup::getId,q.groupIds()).eq(ArGroup::getLegalEntityId,q.legalEntityId()).eq(ArGroup::getCustomerId,q.customerId()).eq(ArGroup::getCurrency,q.currency()).eq(ArGroup::getDeleteStatus,false).orderByAsc(ArGroup::getId));}
 public List<ArInvoiceEligibilityLine> lines(InvoiceEligibilityQueryBO q){return baseMapper.lines(q);}
}
