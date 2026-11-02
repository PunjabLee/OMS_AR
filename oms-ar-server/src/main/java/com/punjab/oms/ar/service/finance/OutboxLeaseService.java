package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.domain.dto.finance.*;import com.punjab.oms.ar.domain.vo.finance.OutboxDeliveryResponse;
/** 内部短事务领取与确认端口，无HTTP映射。 @author Punjab */
public interface OutboxLeaseService {OutboxDeliveryResponse claim(OutboxClaimRequest request);boolean finish(OutboxFinishRequest request);}
