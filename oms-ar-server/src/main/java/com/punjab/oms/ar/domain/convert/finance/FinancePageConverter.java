package com.punjab.oms.ar.domain.convert.finance;
import org.mapstruct.*;import com.punjab.oms.ar.domain.query.finance.*;import com.punjab.oms.ar.domain.bo.finance.*;
/** 分页Web筛选到持久层范围。 @author Punjab */
@Mapper(componentModel="spring",nullValuePropertyMappingStrategy=NullValuePropertyMappingStrategy.IGNORE) public interface FinancePageConverter {
 SettlementPageQueryBO toPageQueryBO(SettlementPageQuery query);
 AccountPageQueryBO toPageQueryBO(AccountPageQuery query);
 LotPageQueryBO toPageQueryBO(LotPageQuery query);
}
