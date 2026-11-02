package com.punjab.oms.ar.domain.convert.finance;
import org.mapstruct.*;import com.punjab.oms.ar.domain.query.finance.ReconciliationQuery;import com.punjab.oms.ar.domain.bo.finance.ReconciliationQueryBO;import com.punjab.oms.ar.domain.entity.finance.ArReconciliationDifference;import com.punjab.oms.ar.domain.vo.finance.ReconciliationResponse;
/** 对账输入及差异输出转换。 @author Punjab */
@Mapper(componentModel="spring",nullValuePropertyMappingStrategy=NullValuePropertyMappingStrategy.IGNORE) public interface ReconciliationConverter {ReconciliationQueryBO toPageQueryBO(ReconciliationQuery query);ReconciliationResponse toResponse(ArReconciliationDifference entity);}
