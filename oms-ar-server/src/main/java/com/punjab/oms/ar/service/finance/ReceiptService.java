package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.domain.dto.finance.ReceiptCreateRequest;
import com.punjab.oms.ar.domain.query.finance.ReceiptDetailQuery;
import com.punjab.oms.ar.domain.vo.finance.ReceiptResponse;
/** 收款入口及余额查询。 @author Punjab */
public interface ReceiptService {Long create(ReceiptCreateRequest request);ReceiptResponse detail(ReceiptDetailQuery query);}
