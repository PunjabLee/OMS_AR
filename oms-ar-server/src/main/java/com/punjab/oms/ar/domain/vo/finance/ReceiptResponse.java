package com.punjab.oms.ar.domain.vo.finance;
import java.math.BigDecimal;
import java.time.LocalDate;
/** 收款余额，不与客户信用余额混用。 @author Punjab */
public record ReceiptResponse(Long id,Long customerId,Long legalEntityId,String currency,BigDecimal amount,BigDecimal allocatedAmount,BigDecimal availableAmount,Long version,LocalDate receivedDate,String sourceSystem,String sourceId,String sourceNo){}
