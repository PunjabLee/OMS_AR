package com.punjab.oms.ar.domain.convert.finance;

import org.mapstruct.*;
import com.punjab.oms.ar.domain.entity.finance.ArCashAccount;
import com.punjab.oms.ar.domain.dto.finance.CashAccountOpenRequest;
import com.punjab.oms.ar.domain.vo.finance.CashAccountResponse;

/** 现金账户开户与余额响应映射。 @author Punjab */
@Mapper(componentModel="spring",unmappedTargetPolicy=ReportingPolicy.IGNORE)
public interface CashAccountConverter {
    ArCashAccount toEntity(CashAccountOpenRequest request);
    CashAccountResponse toResponse(ArCashAccount row);
}
