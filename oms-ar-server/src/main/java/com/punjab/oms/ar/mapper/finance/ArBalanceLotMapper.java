package com.punjab.oms.ar.mapper.finance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;import com.punjab.oms.ar.domain.entity.finance.ArBalanceLot;import org.apache.ibatis.annotations.Mapper;
/** 客户信用余额映射。 @author Punjab */
@Mapper public interface ArBalanceLotMapper extends BaseMapper<ArBalanceLot> {
 @org.apache.ibatis.annotations.Select("<script>SELECT * FROM oms_ar_balance_lot WHERE account_id=#{accountId} AND status='ACTIVE' AND delete_status=false AND amount>used_amount ORDER BY id <if test='lock'>FOR UPDATE</if></script>")
 java.util.List<ArBalanceLot> available(@org.apache.ibatis.annotations.Param("accountId") Long accountId,@org.apache.ibatis.annotations.Param("lock") boolean lock);
}
