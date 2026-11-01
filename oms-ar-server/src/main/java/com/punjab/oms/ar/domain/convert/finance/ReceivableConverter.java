package com.punjab.oms.ar.domain.convert.finance;
import org.mapstruct.*;
import com.punjab.oms.ar.domain.dto.finance.ReceivableCreateRequest;
import com.punjab.oms.ar.domain.entity.finance.*;
/** 请求到持久化模型；金额余额与审计字段由服务填充。 @author Punjab */
@Mapper(componentModel="spring",nullValuePropertyMappingStrategy=NullValuePropertyMappingStrategy.IGNORE,unmappedTargetPolicy=ReportingPolicy.IGNORE)
public interface ReceivableConverter {
 @Mapping(target="id",ignore=true) @Mapping(target="createdTime",ignore=true) @Mapping(target="createdBy",ignore=true)
 @Mapping(target="updatedTime",ignore=true) @Mapping(target="updatedBy",ignore=true) @Mapping(target="deleteStatus",ignore=true)
 ArDocument toEntity(ReceivableCreateRequest request);
 @Mapping(target="receivableAmount",source="amount")
 @Mapping(target="id",ignore=true) @Mapping(target="createdTime",ignore=true) @Mapping(target="createdBy",ignore=true)
 @Mapping(target="updatedTime",ignore=true) @Mapping(target="updatedBy",ignore=true) @Mapping(target="deleteStatus",ignore=true)
 ArLine toEntity(ReceivableCreateRequest.Line request);

 com.punjab.oms.ar.domain.bo.finance.ReceivableDetailQueryBO toDetailQueryBO(com.punjab.oms.ar.domain.query.finance.ReceivableDetailQuery query);
 com.punjab.oms.ar.domain.bo.finance.ReceivablePageQueryBO toPageQueryBO(com.punjab.oms.ar.domain.query.finance.ReceivablePageQuery query);
 com.punjab.oms.ar.domain.vo.finance.ReceivableResponse.ExchangeBasis toResponse(ArExchangeBasis entity);
 com.punjab.oms.ar.domain.vo.finance.ReceivableResponse.Document toResponse(ArDocument entity);
 com.punjab.oms.ar.domain.vo.finance.ReceivableResponse.Source toResponse(ArSource entity);
 com.punjab.oms.ar.domain.vo.finance.ReceivableResponse.Line toResponse(ArLine line,com.punjab.oms.ar.domain.vo.finance.ReceivableResponse.Source source,com.punjab.oms.ar.domain.vo.finance.ReceivableResponse.ExchangeBasis exchangeBasis);
}
