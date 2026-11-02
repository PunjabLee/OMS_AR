package com.punjab.oms.ar.dao.finance;
import com.punjab.oms.ar.domain.entity.finance.ArOutbox;
/** 租约持久化端口，不自行开启事务。 @author Punjab */
public interface OutboxLeaseDao {
 ArOutbox candidate(Long scope);boolean acquire(Long id,Long version,String token,int seconds);boolean exhausted(Long id,Long version);
 boolean finish(Long id,Long scope,String token,boolean success,String error,int maxAttempts,int retrySeconds);
}
