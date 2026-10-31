package com.punjab.oms.ar.domain.convert.finance;
import org.mapstruct.*;
import com.punjab.oms.ar.domain.dto.finance.ReceiptCreateRequest;
import com.punjab.oms.ar.domain.query.finance.ReceiptDetailQuery;
import com.punjab.oms.ar.domain.bo.finance.ReceiptDetailQueryBO;
import com.punjab.oms.ar.domain.entity.finance.ArReceipt;
import com.punjab.oms.ar.domain.vo.finance.ReceiptResponse;
/** 收款模型转换。 @author Punjab */
@Mapper(componentModel="spring",nullValuePropertyMappingStrategy=NullValuePropertyMappingStrategy.IGNORE,unmappedTargetPolicy=ReportingPolicy.IGNORE)
public interface ReceiptConverter {
 @Mapping(target="id",ignore=true) @Mapping(target="createdTime",ignore=true) @Mapping(target="createdBy",ignore=true)
 @Mapping(target="updatedTime",ignore=true) @Mapping(target="updatedBy",ignore=true) @Mapping(target="deleteStatus",ignore=true)
 ArReceipt toEntity(ReceiptCreateRequest request);
 ReceiptDetailQueryBO toDetailQueryBO(ReceiptDetailQuery query);
 @Mapping(target="availableAmount",expression="java(entity.getAmount().subtract(entity.getAllocatedAmount()))")
 ReceiptResponse toResponse(ArReceipt entity);
}
