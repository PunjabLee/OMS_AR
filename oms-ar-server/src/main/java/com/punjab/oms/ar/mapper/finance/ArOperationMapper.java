package com.punjab.oms.ar.mapper.finance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.punjab.oms.ar.domain.entity.finance.ArOperation;
import org.apache.ibatis.annotations.Mapper;
/** operation_request映射。 @author Punjab */
@Mapper
public interface ArOperationMapper extends BaseMapper<ArOperation> {
@org.apache.ibatis.annotations.Insert("INSERT INTO oms_ar_operation_request(legal_entity_id,caller_id,operation_type,request_key,payload_hash,created_time,created_by,updated_time,updated_by,delete_status) VALUES(#{legalEntityId},#{callerId},#{operationType},#{requestKey},#{payloadHash},#{createdTime},#{createdBy},#{updatedTime},#{updatedBy},false) ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id)")
    int reserve(ArOperation operation);
}
