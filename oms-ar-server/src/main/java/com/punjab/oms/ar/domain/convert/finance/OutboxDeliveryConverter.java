package com.punjab.oms.ar.domain.convert.finance;
import org.mapstruct.*;import com.punjab.oms.ar.domain.entity.finance.ArOutbox;import com.punjab.oms.ar.domain.vo.finance.OutboxDeliveryResponse;
/** 内部投递响应明确排除租约与凭据。 @author Punjab */
@Mapper(componentModel="spring",nullValuePropertyMappingStrategy=NullValuePropertyMappingStrategy.IGNORE)
public interface OutboxDeliveryConverter {OutboxDeliveryResponse toResponse(ArOutbox value);}
