package com.punjab.oms.ar.mapper.finance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;import com.punjab.oms.ar.domain.entity.finance.ArSourceGuard;import org.apache.ibatis.annotations.*;
/** 来源锁映射，重复插入不更改既有取消状态及审计字段。 @author Punjab */
@Mapper public interface ArSourceGuardMapper extends BaseMapper<ArSourceGuard> {
 @Insert("INSERT IGNORE INTO oms_ar_source_guard(legal_entity_id,source_key,source_system,source_type,source_id,source_line_id,blocked,version,created_time,created_by,updated_time,updated_by,delete_status) VALUES(#{legalEntityId},#{sourceKey},#{sourceSystem},#{sourceType},#{sourceId},#{sourceLineId},false,0,#{createdTime},#{createdBy},#{updatedTime},#{updatedBy},false)")
 int initialize(ArSourceGuard value);
}
