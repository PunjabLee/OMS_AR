package com.punjab.oms.ar.mapper.finance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;import com.punjab.oms.ar.domain.entity.finance.ArOutboxRetry;import org.apache.ibatis.annotations.*;
/** 重试审计和通知CAS，时间使用数据库时钟。 @author Punjab */
@Mapper public interface ArOutboxRetryMapper extends BaseMapper<ArOutboxRetry> {
 @Update("""
 UPDATE oms_ar_outbox SET status='PENDING',attempt_count=0,next_attempt_at=NOW(6),last_error=NULL,
 version=version+1,updated_time=NOW(6),updated_by=#{actor}
 WHERE id=#{id} AND legal_entity_id=#{scope} AND version=#{version} AND delete_status=false
 AND destination='FINANCE_EVENTS' AND status IN ('RETRY','DEAD')
 """) int reset(@Param("id")Long id,@Param("scope")Long scope,@Param("version")Long version,@Param("actor")String actor);
}
