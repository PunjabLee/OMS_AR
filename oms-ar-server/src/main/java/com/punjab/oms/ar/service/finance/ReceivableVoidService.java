package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.domain.dto.finance.*;import com.punjab.oms.ar.domain.vo.finance.ReceivableVoidResponse;
/** 应收作废事务边界。 @author Punjab */
public interface ReceivableVoidService {ReceivableVoidResponse voidDocument(ReceivableVoidRequest request);ReceivableVoidResponse voidUpstream(UpstreamVoidRequest request);}
