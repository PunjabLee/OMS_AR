package com.punjab.oms.ar.dao.finance.impl;
import com.punjab.oms.ar.dao.finance.OutboxQueryDao;import com.punjab.oms.ar.domain.bo.finance.*;import com.punjab.oms.ar.domain.entity.finance.ArOutbox;import com.punjab.oms.ar.mapper.finance.ArOutboxMapper;import com.punjab.oms.ar.base.PageResult;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;import org.springframework.stereotype.Repository;
/** 列表不读取payload/lease_token；总数和记录使用相同筛选。 @author Punjab */
@Repository public class OutboxQueryDaoImpl extends CrudRepository<ArOutboxMapper,ArOutbox> implements OutboxQueryDao {
 public PageResult<ArOutbox> page(OutboxPageQueryBO q){
  var filter=new LambdaQueryWrapper<ArOutbox>().eq(ArOutbox::getLegalEntityId,q.getLegalEntityId()).eq(ArOutbox::getDeleteStatus,false)
    .eq(q.getEventId()!=null,ArOutbox::getEventId,q.getEventId()).eq(q.getStatus()!=null,ArOutbox::getStatus,q.getStatus()).eq(q.getDestination()!=null,ArOutbox::getDestination,q.getDestination());
  long total=baseMapper.selectCount(filter);metadata(filter);filter.orderByDesc(ArOutbox::getId).last("LIMIT "+q.getSize()+" OFFSET "+(((long)q.getPage()-1)*q.getSize()));
  return new PageResult<>(total,q.getPage(),q.getSize(),baseMapper.selectList(filter));
 }
 public ArOutbox detail(OutboxDetailQueryBO q){return baseMapper.selectOne(new LambdaQueryWrapper<ArOutbox>().eq(ArOutbox::getId,q.id()).eq(ArOutbox::getLegalEntityId,q.legalEntityId()).eq(ArOutbox::getDeleteStatus,false));}
 private void metadata(LambdaQueryWrapper<ArOutbox> q){q.select(ArOutbox::getId,ArOutbox::getEventId,ArOutbox::getLegalEntityId,ArOutbox::getDestination,ArOutbox::getStatus,ArOutbox::getAttemptCount,ArOutbox::getNextAttemptAt,ArOutbox::getLeaseUntil,ArOutbox::getSentTime,ArOutbox::getLastError,ArOutbox::getVersion,ArOutbox::getCreatedTime,ArOutbox::getCreatedBy,ArOutbox::getUpdatedTime,ArOutbox::getUpdatedBy);}
}
