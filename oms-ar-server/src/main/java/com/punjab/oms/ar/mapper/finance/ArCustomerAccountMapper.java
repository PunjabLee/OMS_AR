package com.punjab.oms.ar.mapper.finance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;import com.punjab.oms.ar.domain.entity.finance.ArCustomerAccount;import org.apache.ibatis.annotations.Mapper;
/** 客户信用余额映射。 @author Punjab */
@Mapper public interface ArCustomerAccountMapper extends BaseMapper<ArCustomerAccount> {
@org.apache.ibatis.annotations.Insert("INSERT INTO oms_ar_customer_account(customer_id,legal_entity_id,currency,cash_account_id,balance_amount,version,created_time,created_by,updated_time,updated_by,delete_status) VALUES(#{customerId},#{legalEntityId},#{currency},#{cashAccountId},0,0,#{createdTime},#{createdBy},#{updatedTime},#{updatedBy},false) ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id)")
 int reserve(ArCustomerAccount value);
}
