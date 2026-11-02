package com.punjab.oms.ar.domain.convert.finance;
import org.mapstruct.*;import com.punjab.oms.ar.domain.entity.finance.*;import com.punjab.oms.ar.domain.query.finance.SettlementDetailQuery;import com.punjab.oms.ar.domain.bo.finance.SettlementDetailQueryBO;import com.punjab.oms.ar.domain.vo.finance.SettlementResponse;
/** 核销只读模型转换。 @author Punjab */
@Mapper(componentModel="spring",nullValuePropertyMappingStrategy=NullValuePropertyMappingStrategy.IGNORE) public interface SettlementQueryConverter {
 SettlementDetailQueryBO toDetailQueryBO(SettlementDetailQuery query);SettlementResponse.Header toResponse(ArSettlement entity);SettlementResponse.Allocation toResponse(ArSettlementAllocation entity);SettlementResponse.Usage toResponse(ArBalanceUsage entity);
}
