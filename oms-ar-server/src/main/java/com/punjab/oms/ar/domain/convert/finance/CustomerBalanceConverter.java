package com.punjab.oms.ar.domain.convert.finance;
import org.mapstruct.*;import com.punjab.oms.ar.domain.query.finance.BalanceDetailQuery;import com.punjab.oms.ar.domain.bo.finance.BalanceDetailQueryBO;import com.punjab.oms.ar.domain.entity.finance.*;import com.punjab.oms.ar.domain.vo.finance.*;
/** 余额查询模型映射。 @author Punjab */
@Mapper(componentModel="spring",nullValuePropertyMappingStrategy=NullValuePropertyMappingStrategy.IGNORE)
public interface CustomerBalanceConverter {
 BalanceDetailQueryBO toDetailQueryBO(BalanceDetailQuery query);
 CustomerAccountResponse toResponse(ArCustomerAccount entity);
 BalanceLotResponse toResponse(ArBalanceLot entity);
}
