package com.punjab.oms.ar.dao.finance.impl;
import com.punjab.oms.ar.dao.finance.ReceivableDao;
import com.punjab.oms.ar.domain.entity.finance.*;
import com.punjab.oms.ar.mapper.finance.*;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
/** 仅管理应收聚合对应的数据对象。 @author Punjab */
@Repository @RequiredArgsConstructor
public class ReceivableDaoImpl extends CrudRepository<ArDocumentMapper,ArDocument> implements ReceivableDao {
 private final ArSourceGuardMapper sourceGuards;
 private final ArGroupMapper groups;
 private final ArAdjustmentLinkMapper links;
 private final ArLineMapper lines;
 private final ArSourceMapper sources;
 private final ArExchangeBasisMapper exchangeBases;
 private final ArOperationMapper operations;
 private final ArEventMapper events;
 private final ArChangeMapper changes;
 public boolean initializeSourceGuard(ArSourceGuard value){return sourceGuards.initialize(value)==1;}
 public ArSourceGuard lockSourceGuard(Long scope,String key){return sourceGuards.selectOne(new LambdaQueryWrapper<ArSourceGuard>().eq(ArSourceGuard::getLegalEntityId,scope).eq(ArSourceGuard::getSourceKey,key).last("FOR UPDATE"));}
 public ArOperation reserve(ArOperation value){
  operations.reserve(value);
  return operations.selectOne(new LambdaQueryWrapper<ArOperation>()
    .eq(ArOperation::getLegalEntityId,value.getLegalEntityId()).eq(ArOperation::getCallerId,value.getCallerId())
    .eq(ArOperation::getOperationType,value.getOperationType()).eq(ArOperation::getRequestKey,value.getRequestKey())
    .eq(ArOperation::getDeleteStatus,false).last("FOR UPDATE"));
 }
 public boolean complete(ArOperation value){return operations.updateById(value)==1;}
 public ArGroup group(Long id){return groups.selectOne(new LambdaQueryWrapper<ArGroup>().eq(ArGroup::getId,id).eq(ArGroup::getDeleteStatus,false).last("FOR UPDATE"));}
 public ArLine line(Long id,boolean lock){var q=new LambdaQueryWrapper<ArLine>().eq(ArLine::getId,id).eq(ArLine::getDeleteStatus,false);if(lock)q.last("FOR UPDATE");return lines.selectOne(q);}
 public java.util.List<ArAdjustmentLink> adjustmentLinks(java.util.List<Long> ids){if(ids.isEmpty())return java.util.List.of();return links.selectList(new LambdaQueryWrapper<ArAdjustmentLink>().in(ArAdjustmentLink::getLineId,ids).eq(ArAdjustmentLink::getDeleteStatus,false));}
 public ArAdjustmentLink adjustmentLink(Long lineId){return links.selectOne(new LambdaQueryWrapper<ArAdjustmentLink>().eq(ArAdjustmentLink::getLineId,lineId).eq(ArAdjustmentLink::getDeleteStatus,false));}
 public boolean createAdjustmentLink(ArAdjustmentLink value){return links.insert(value)==1;}
 public boolean createDocument(ArDocument value){return baseMapper.insert(value)==1;}
 public boolean createGroup(ArGroup value){return groups.insert(value)==1;}
 public boolean createLine(ArLine value){return lines.insert(value)==1;}
 public boolean createExchangeBasis(ArExchangeBasis value){return exchangeBases.insert(value)==1;}
 public java.util.List<ArExchangeBasis> exchangeBases(java.util.List<Long> ids){if(ids.isEmpty())return java.util.List.of();return exchangeBases.selectList(new LambdaQueryWrapper<ArExchangeBasis>().in(ArExchangeBasis::getLineId,ids).eq(ArExchangeBasis::getDeleteStatus,false));}
 public boolean createSource(ArSource value){return sources.insert(value)==1;}
 public boolean createEvent(ArEvent value){return events.insert(value)==1;}
 public boolean createChange(ArChange value){return changes.insert(value)==1;}

 public ArDocument document(com.punjab.oms.ar.domain.bo.finance.ReceivableDetailQueryBO query){
  return baseMapper.selectOne(new LambdaQueryWrapper<ArDocument>().eq(ArDocument::getId,query.id()).eq(ArDocument::getLegalEntityId,query.legalEntityId()).eq(ArDocument::getDeleteStatus,false));
 }
 public java.util.List<ArLine> lines(Long documentId){return lines.selectList(new LambdaQueryWrapper<ArLine>().eq(ArLine::getDocumentId,documentId).eq(ArLine::getDeleteStatus,false).orderByAsc(ArLine::getLineNo));}
 public java.util.List<ArSource> sources(java.util.List<Long> ids){
  if(ids.isEmpty())return java.util.List.of();
  return sources.selectList(new LambdaQueryWrapper<ArSource>().in(ArSource::getLineId,ids).eq(ArSource::getDeleteStatus,false));
 }
 public com.punjab.oms.ar.base.PageResult<ArDocument> page(com.punjab.oms.ar.domain.bo.finance.ReceivablePageQueryBO query){
  return new com.punjab.oms.ar.base.PageResult<>(baseMapper.countActive(query),query.getPage(),query.getSize(),baseMapper.pageActive(query));
 }
}
