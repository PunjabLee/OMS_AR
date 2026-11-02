package com.punjab.oms.ar.dao.finance.impl;
import com.punjab.oms.ar.dao.finance.OutboxLeaseDao;import com.punjab.oms.ar.mapper.finance.ArOutboxLeaseMapper;import com.punjab.oms.ar.domain.entity.finance.ArOutbox;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;import org.springframework.stereotype.Repository;
/** 固定SQL处理SKIP LOCKED、CAS与NOW(6)，避免应用节点时钟差异。 @author Punjab */
@Repository public class OutboxLeaseDaoImpl extends CrudRepository<ArOutboxLeaseMapper,ArOutbox> implements OutboxLeaseDao {
 public ArOutbox candidate(Long scope){return baseMapper.candidate(scope);}
 public boolean acquire(Long id,Long version,String token,int seconds){return baseMapper.acquire(id,version,token,seconds)==1;}
 public boolean exhausted(Long id,Long version){return baseMapper.exhausted(id,version)==1;}
 public boolean finish(Long id,Long scope,String token,boolean success,String error,int maxAttempts,int retrySeconds){return baseMapper.finish(id,scope,token,success,error,maxAttempts,retrySeconds)==1;}
}
