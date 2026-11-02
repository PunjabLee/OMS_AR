package com.punjab.oms.ar.domain.convert.finance;
import org.mapstruct.*;import com.punjab.oms.ar.domain.query.finance.*;import com.punjab.oms.ar.domain.bo.finance.*;import com.punjab.oms.ar.domain.vo.finance.OutboxResponse;import com.punjab.oms.ar.domain.entity.finance.ArOutbox;
/** 运营模型显式排除敏感租约。 @author Punjab */
@Mapper(componentModel="spring",nullValuePropertyMappingStrategy=NullValuePropertyMappingStrategy.IGNORE)
public interface OutboxQueryConverter {OutboxPageQueryBO toPageQueryBO(OutboxPageQuery query);OutboxDetailQueryBO toDetailQueryBO(OutboxDetailQuery query);OutboxResponse toResponse(ArOutbox value);}
