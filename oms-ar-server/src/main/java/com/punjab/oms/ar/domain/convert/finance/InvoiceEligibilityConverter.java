package com.punjab.oms.ar.domain.convert.finance;
import org.mapstruct.*;import com.punjab.oms.ar.domain.query.finance.InvoiceEligibilityQuery;import com.punjab.oms.ar.domain.bo.finance.InvoiceEligibilityQueryBO;
/** 开票范围映射。 @author Punjab */
@Mapper(componentModel="spring",nullValuePropertyMappingStrategy=NullValuePropertyMappingStrategy.IGNORE)
public interface InvoiceEligibilityConverter {InvoiceEligibilityQueryBO toQueryBO(InvoiceEligibilityQuery query);}
