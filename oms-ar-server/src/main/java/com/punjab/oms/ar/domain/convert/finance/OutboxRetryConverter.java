package com.punjab.oms.ar.domain.convert.finance;
import org.mapstruct.*;import com.punjab.oms.ar.domain.query.finance.OutboxRetryPageQuery;import com.punjab.oms.ar.domain.bo.finance.OutboxRetryPageQueryBO;import com.punjab.oms.ar.domain.entity.finance.ArOutboxRetry;import com.punjab.oms.ar.domain.vo.finance.OutboxRetryResponse;
/** 重试审计查询转换。 @author Punjab */
@Mapper(componentModel="spring",nullValuePropertyMappingStrategy=NullValuePropertyMappingStrategy.IGNORE)
public interface OutboxRetryConverter {OutboxRetryPageQueryBO toPageQueryBO(OutboxRetryPageQuery query);OutboxRetryResponse toResponse(ArOutboxRetry value);}
