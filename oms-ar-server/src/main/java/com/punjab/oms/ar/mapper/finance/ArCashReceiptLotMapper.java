package com.punjab.oms.ar.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.punjab.oms.ar.domain.entity.finance.ArCashReceiptLot;
import org.apache.ibatis.annotations.Mapper;

/** 现金批次映射。 @author Punjab */
@Mapper public interface ArCashReceiptLotMapper extends BaseMapper<ArCashReceiptLot> {
    /** 按充值批次检查不可变冲正记录。 */
    @org.apache.ibatis.annotations.Select("SELECT EXISTS(SELECT 1 FROM oms_ar_cash_receipt_reversal WHERE lot_id=#{lotId})")
    boolean reversed(Long lotId);

    /** 保存一次整批冲正，数据库唯一键保护批次和操作幂等。 */
    @org.apache.ibatis.annotations.Insert("""
        INSERT INTO oms_ar_cash_receipt_reversal(lot_id,operation_id,amount,reason,created_time,created_by,updated_time,updated_by)
        VALUES(#{lotId},#{operationId},#{amount},#{reason},NOW(6),#{actor},NOW(6),#{actor})
        """)
    int reverse(Long lotId,Long operationId,java.math.BigDecimal amount,String reason,String actor);

    @org.apache.ibatis.annotations.Select("SELECT id FROM oms_ar_cash_receipt_reversal WHERE lot_id=#{lotId}")
    Long reversalId(Long lotId);
    @org.apache.ibatis.annotations.Select("""
        SELECT c.event_id FROM oms_ar_lifecycle_change c JOIN oms_ar_lifecycle_event e ON e.id=c.event_id
          WHERE c.object_type='RECEIPT' AND c.object_id=#{receiptId} AND e.event_type='RECEIPT_CREATED'
          AND c.delete_status=false AND e.delete_status=false
        """)
    Long originalEvent(Long receiptId);
}
