package com.punjab.oms.ar.dao.finance.impl;
import com.punjab.oms.ar.dao.finance.OutboxDao;import com.punjab.oms.ar.domain.entity.finance.*;import com.punjab.oms.ar.mapper.finance.*;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Repository;import lombok.RequiredArgsConstructor;import java.util.List;
/** 同财务聚合内按事件读取完整变化，禁止发送阶段重新构造。 @author Punjab */
@Repository @RequiredArgsConstructor
public class OutboxDaoImpl extends CrudRepository<ArOutboxMapper,ArOutbox> implements OutboxDao {
 private final ArEventMapper events;private final ArChangeMapper changes;
 public ArEvent event(Long id){return events.selectOne(new LambdaQueryWrapper<ArEvent>().eq(ArEvent::getId,id).eq(ArEvent::getDeleteStatus,false));}
 public List<ArChange> changes(Long id){return changes.selectList(new LambdaQueryWrapper<ArChange>().eq(ArChange::getEventId,id).eq(ArChange::getDeleteStatus,false).orderByAsc(ArChange::getId));}
 public boolean create(ArOutbox value){return baseMapper.insert(value)==1;}
}
