package com.punjab.oms.ar.dao.finance.impl;
import com.punjab.oms.ar.dao.finance.OutboxRetryDao;import com.punjab.oms.ar.domain.entity.finance.*;import com.punjab.oms.ar.domain.bo.finance.OutboxRetryPageQueryBO;import com.punjab.oms.ar.mapper.finance.*;import com.punjab.oms.ar.base.PageResult;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;import org.springframework.stereotype.Repository;import lombok.RequiredArgsConstructor;
/** 同一范围计数/分页，DAO不管理事务。 @author Punjab */
@Repository @RequiredArgsConstructor public class OutboxRetryDaoImpl extends CrudRepository<ArOutboxRetryMapper,ArOutboxRetry> implements OutboxRetryDao {
 private final ArOutboxMapper notifications;
 public ArOutbox notification(Long id,Long scope,boolean lock){var q=new LambdaQueryWrapper<ArOutbox>().eq(ArOutbox::getId,id).eq(ArOutbox::getLegalEntityId,scope).eq(ArOutbox::getDeleteStatus,false);if(lock)q.last("FOR UPDATE");return notifications.selectOne(q);}
 public boolean reset(Long id,Long scope,Long version,String actor){return baseMapper.reset(id,scope,version,actor)==1;}
 public boolean create(ArOutboxRetry value){return baseMapper.insert(value)==1;}
 public PageResult<ArOutboxRetry> history(OutboxRetryPageQueryBO q){var filter=new LambdaQueryWrapper<ArOutboxRetry>().eq(ArOutboxRetry::getLegalEntityId,q.getLegalEntityId()).eq(ArOutboxRetry::getOutboxId,q.getOutboxId()).eq(ArOutboxRetry::getDeleteStatus,false);long total=baseMapper.selectCount(filter);filter.orderByDesc(ArOutboxRetry::getId).last("LIMIT "+q.getSize()+" OFFSET "+(((long)q.getPage()-1)*q.getSize()));return new PageResult<>(total,q.getPage(),q.getSize(),baseMapper.selectList(filter));}
}
