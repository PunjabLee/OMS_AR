package com.punjab.oms.ar.domain.vo.finance;
import java.math.BigDecimal;
import java.util.List;
public record CashHoldView(Long id,Long accountId,String purpose,BigDecimal amount,BigDecimal releasedAmount,BigDecimal capturedAmount,BigDecimal remainingAmount,List<Slice> slices){
    public record Slice(Long lotId,BigDecimal amount,BigDecimal releasedAmount,BigDecimal capturedAmount){}
}
