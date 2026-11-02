package com.punjab.oms.ar.domain.convert.finance;
import org.mapstruct.*;import com.punjab.oms.ar.domain.query.finance.LifecycleQuery;import com.punjab.oms.ar.domain.bo.finance.LifecycleQueryBO;
/** 生命周期查询边界转换。 @author Punjab */
@Mapper(componentModel="spring",nullValuePropertyMappingStrategy=NullValuePropertyMappingStrategy.IGNORE) public interface LifecycleConverter {LifecycleQueryBO toPageQueryBO(LifecycleQuery query);}
