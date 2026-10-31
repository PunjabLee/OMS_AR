package com.punjab.oms.ar.mapper.finance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.punjab.oms.ar.domain.entity.finance.ArDocument;
import org.apache.ibatis.annotations.Mapper;
/** document映射。 @author Punjab */
@Mapper
public interface ArDocumentMapper extends BaseMapper<ArDocument> {


 @org.apache.ibatis.annotations.Select("<script>SELECT * FROM oms_ar_document WHERE delete_status=false AND legal_entity_id=#{legalEntityId} <if test='customerId != null'>AND customer_id=#{customerId}</if> ORDER BY created_time DESC,id DESC LIMIT #{size} OFFSET #{offset}</script>")
 java.util.List<ArDocument> pageActive(com.punjab.oms.ar.domain.bo.finance.ReceivablePageQueryBO query);
 @org.apache.ibatis.annotations.Select("<script>SELECT COUNT(*) FROM oms_ar_document WHERE delete_status=false AND legal_entity_id=#{legalEntityId} <if test='customerId != null'>AND customer_id=#{customerId}</if></script>")
 long countActive(com.punjab.oms.ar.domain.bo.finance.ReceivablePageQueryBO query);
}
