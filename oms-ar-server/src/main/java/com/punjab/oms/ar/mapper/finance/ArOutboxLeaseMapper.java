package com.punjab.oms.ar.mapper.finance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;import com.punjab.oms.ar.domain.entity.finance.ArOutbox;import org.apache.ibatis.annotations.*;
/** 租约CAS与数据库时钟；固定SQL不拼入请求文本。 @author Punjab */
@Mapper public interface ArOutboxLeaseMapper extends BaseMapper<ArOutbox>{
 @Select("""
 SELECT * FROM oms_ar_outbox WHERE legal_entity_id=#{scope} AND destination='FINANCE_EVENTS' AND delete_status=false
 AND ((status IN ('PENDING','RETRY') AND next_attempt_at<=NOW(6)) OR (status='PROCESSING' AND lease_until<=NOW(6)))
 ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED
 """) ArOutbox candidate(@Param("scope")Long scope);
 @Update("""
 UPDATE oms_ar_outbox SET status='PROCESSING',attempt_count=attempt_count+1,lease_token=#{token},
 lease_until=DATE_ADD(NOW(6),INTERVAL #{seconds} SECOND),version=version+1,updated_time=NOW(6),updated_by='system:outbox'
 WHERE id=#{id} AND version=#{version} AND delete_status=false
 """) int acquire(@Param("id")Long id,@Param("version")Long version,@Param("token")String token,@Param("seconds")int seconds);
 @Update("""
 UPDATE oms_ar_outbox SET last_error=CASE WHEN status='PROCESSING' THEN 'LEASE_EXPIRED' ELSE 'ATTEMPTS_EXHAUSTED' END,
 status='DEAD',lease_token=NULL,lease_until=NULL,version=version+1,updated_time=NOW(6),updated_by='system:outbox'
 WHERE id=#{id} AND version=#{version} AND delete_status=false
 """) int exhausted(@Param("id")Long id,@Param("version")Long version);
 @Update("""
 UPDATE oms_ar_outbox SET status=CASE WHEN #{success} THEN 'SENT' WHEN attempt_count>=#{maxAttempts} THEN 'DEAD' ELSE 'RETRY' END,
 sent_time=CASE WHEN #{success} THEN NOW(6) ELSE NULL END,last_error=CASE WHEN #{success} THEN NULL ELSE #{error} END,
 next_attempt_at=CASE WHEN #{success} THEN next_attempt_at ELSE DATE_ADD(NOW(6),INTERVAL #{retrySeconds} SECOND) END,
 lease_token=NULL,lease_until=NULL,version=version+1,updated_time=NOW(6),updated_by='system:outbox'
 WHERE id=#{id} AND legal_entity_id=#{scope} AND destination='FINANCE_EVENTS' AND delete_status=false
 AND status='PROCESSING' AND lease_token=#{token} AND lease_until>NOW(6)
 """) int finish(@Param("id")Long id,@Param("scope")Long scope,@Param("token")String token,@Param("success")boolean success,@Param("error")String error,@Param("maxAttempts")int maxAttempts,@Param("retrySeconds")int retrySeconds);
}
