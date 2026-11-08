package com.punjab.oms.ar.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.punjab.oms.ar.domain.entity.finance.ArCashAccount;
import org.apache.ibatis.annotations.Mapper;

/** 现金账户映射。 @author Punjab */
@Mapper public interface ArCashAccountMapper extends BaseMapper<ArCashAccount> {
    /** 账户锁下检查尚有余额的应收、信用和在途提现。 */
    @org.apache.ibatis.annotations.Select("""
        SELECT EXISTS(SELECT 1 FROM oms_ar_cash_group_binding b JOIN oms_ar_line l ON l.group_id=b.group_id
                       WHERE b.cash_account_id=#{id} AND b.delete_status=false AND l.status='ACTIVE' AND l.delete_status=false AND l.open_amount<>0)
            OR EXISTS(SELECT 1 FROM oms_ar_customer_account WHERE cash_account_id=#{id} AND delete_status=false AND balance_amount<>0)
            OR EXISTS(SELECT 1 FROM oms_ar_withdrawal WHERE cash_account_id=#{id} AND delete_status=false AND status IN ('PENDING','APPROVED','PROCESSING','UNKNOWN'))
        """)
    boolean hasOpenDependencies(Long id);
}
