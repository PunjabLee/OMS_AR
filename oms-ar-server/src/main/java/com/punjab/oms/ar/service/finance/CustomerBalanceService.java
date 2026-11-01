package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.domain.dto.finance.*;
/** 最小客户信用余额服务。 @author Punjab */
public interface CustomerBalanceService {
 com.punjab.oms.ar.domain.vo.finance.CustomerAccountResponse detail(com.punjab.oms.ar.domain.query.finance.BalanceDetailQuery query);
 com.punjab.oms.ar.domain.vo.finance.BalanceLotResponse lotDetail(com.punjab.oms.ar.domain.query.finance.BalanceDetailQuery query);
Long transfer(BalanceTransferRequest request);Long reverseTransfer(BalanceTransferReverseRequest request);}
