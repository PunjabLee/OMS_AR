package com.punjab.oms.ar.domain.convert.finance;
import org.mapstruct.*;import com.punjab.oms.ar.domain.query.finance.InvoiceDetailQuery;import com.punjab.oms.ar.domain.bo.finance.InvoiceDetailQueryBO;import com.punjab.oms.ar.domain.entity.finance.*;import com.punjab.oms.ar.domain.vo.finance.InvoiceResponse;import java.math.BigDecimal;
/** 发票详情模型边界。 @author Punjab */
@Mapper(componentModel="spring",nullValuePropertyMappingStrategy=NullValuePropertyMappingStrategy.IGNORE)
public interface InvoiceConverter {InvoiceDetailQueryBO toDetailQueryBO(InvoiceDetailQuery query);InvoiceResponse.Header toResponse(ArInvoice invoice);InvoiceResponse.Allocation toResponse(ArInvoiceAllocation allocation,BigDecimal redAvailableAmount);}
