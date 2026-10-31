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
 private final ArGroupMapper groups;
 private final ArLineMapper lines;
 private final ArSourceMapper sources;
 private final ArOperationMapper operations;
 private final ArEventMapper events;
 private final ArChangeMapper changes;
 public ArOperation reserve(ArOperation value){
  operations.reserve(value);
  return operations.selectOne(new LambdaQueryWrapper<ArOperation>()
    .eq(ArOperation::getLegalEntityId,value.getLegalEntityId()).eq(ArOperation::getCallerId,value.getCallerId())
    .eq(ArOperation::getOperationType,value.getOperationType()).eq(ArOperation::getRequestKey,value.getRequestKey())
    .eq(ArOperation::getDeleteStatus,false).last("FOR UPDATE"));
 }
 public boolean complete(ArOperation value){return operations.updateById(value)==1;}
 public ArGroup group(Long id){return groups.selectOne(new LambdaQueryWrapper<ArGroup>().eq(ArGroup::getId,id).eq(ArGroup::getDeleteStatus,false).last("FOR UPDATE"));}
 public boolean createDocument(ArDocument value){return baseMapper.insert(value)==1;}
 public boolean createGroup(ArGroup value){return groups.insert(value)==1;}
 public boolean createLine(ArLine value){return lines.insert(value)==1;}
 public boolean createSource(ArSource value){return sources.insert(value)==1;}
 public boolean createEvent(ArEvent value){return events.insert(value)==1;}
 public boolean createChange(ArChange value){return changes.insert(value)==1;}
}
