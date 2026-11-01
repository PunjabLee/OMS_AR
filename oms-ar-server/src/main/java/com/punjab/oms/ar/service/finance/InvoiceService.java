package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.domain.dto.finance.*;import com.punjab.oms.ar.domain.query.finance.InvoiceDetailQuery;import com.punjab.oms.ar.domain.vo.finance.*;
/** 发票关联生命周期。 @author Punjab */
public interface InvoiceService {InvoicePreviewResponse preview(InvoicePreviewRequest request);Long register(InvoiceRegisterRequest request);Long reverse(InvoiceReverseRequest request);InvoiceResponse detail(InvoiceDetailQuery query);}
