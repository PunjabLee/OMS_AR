package com.punjab.oms.ar.dao.finance.impl;
import com.punjab.oms.ar.dao.finance.InvoiceDao;import com.punjab.oms.ar.domain.entity.finance.*;import com.punjab.oms.ar.domain.bo.finance.InvoiceDetailQueryBO;import com.punjab.oms.ar.mapper.finance.*;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;import org.springframework.stereotype.Repository;import lombok.RequiredArgsConstructor;import java.util.List;
/** 原蓝字作为红字串行化锁；业务组作为开票共享额度串行化锁。 @author Punjab */
@Repository @RequiredArgsConstructor
public class InvoiceDaoImpl extends CrudRepository<ArInvoiceMapper,ArInvoice> implements InvoiceDao {
 private final ArInvoiceAllocationMapper allocations;private final InvoiceEligibilityMapper eligibility;private final ArGroupMapper groups;private final ArLineMapper lines;private final ArEventMapper events;
 public ArInvoice invoice(Long id,Long scope,boolean lock){var q=new LambdaQueryWrapper<ArInvoice>().eq(ArInvoice::getId,id).eq(ArInvoice::getLegalEntityId,scope).eq(ArInvoice::getDeleteStatus,false);if(lock)q.last("FOR UPDATE");return baseMapper.selectOne(q);}
 public ArInvoice detail(InvoiceDetailQueryBO q){return invoice(q.id(),q.legalEntityId(),false);}
 public List<ArInvoiceAllocation> allocations(Long id){return allocations.selectList(new LambdaQueryWrapper<ArInvoiceAllocation>().eq(ArInvoiceAllocation::getInvoiceId,id).eq(ArInvoiceAllocation::getDeleteStatus,false).orderByAsc(ArInvoiceAllocation::getId));}
 public List<ArInvoiceAllocation> activeRedAllocations(Long id){return allocations.activeRedAllocations(id);}
 public List<ArInvoiceEligibilityLine> sources(List<Long> ids,Long scope){return ids.isEmpty()?List.of():eligibility.selectedLines(ids,scope);}
 public List<ArGroup> lockGroups(List<Long> ids,Long scope){return groups.selectList(new LambdaQueryWrapper<ArGroup>().in(ArGroup::getId,ids).eq(ArGroup::getLegalEntityId,scope).eq(ArGroup::getDeleteStatus,false).orderByAsc(ArGroup::getId).last("FOR UPDATE"));}
 public boolean create(ArInvoice v){return baseMapper.insert(v)==1;}public boolean allocate(ArInvoiceAllocation v){return allocations.insert(v)==1;}
 public boolean update(ArInvoice v,Long expected){return baseMapper.update(null,new LambdaUpdateWrapper<ArInvoice>().eq(ArInvoice::getId,v.getId()).eq(ArInvoice::getVersion,expected).eq(ArInvoice::getDeleteStatus,false).set(ArInvoice::getStatus,v.getStatus()).set(ArInvoice::getVersion,v.getVersion()).set(ArInvoice::getUpdatedTime,v.getUpdatedTime()).set(ArInvoice::getUpdatedBy,v.getUpdatedBy()))==1;}
 public boolean updateLine(ArLine v,Long expected){return lines.update(null,new LambdaUpdateWrapper<ArLine>().eq(ArLine::getId,v.getId()).eq(ArLine::getVersion,expected).eq(ArLine::getStatus,"ACTIVE").eq(ArLine::getDeleteStatus,false).set(ArLine::getInvoicedAmount,v.getInvoicedAmount()).set(ArLine::getVersion,v.getVersion()).set(ArLine::getUpdatedTime,v.getUpdatedTime()).set(ArLine::getUpdatedBy,v.getUpdatedBy()))==1;}
 public long activeRedCount(Long id){return baseMapper.selectCount(new LambdaQueryWrapper<ArInvoice>().eq(ArInvoice::getOriginalInvoiceId,id).eq(ArInvoice::getStatus,"ACTIVE").eq(ArInvoice::getDeleteStatus,false));}
 public Long postedEvent(Long id){var event=events.selectOne(new LambdaQueryWrapper<ArEvent>().eq(ArEvent::getEventType,"INVOICE_REGISTERED").eq(ArEvent::getTriggerType,"INVOICE").eq(ArEvent::getTriggerId,id.toString()).eq(ArEvent::getDeleteStatus,false));return event==null?null:event.getId();}
}
