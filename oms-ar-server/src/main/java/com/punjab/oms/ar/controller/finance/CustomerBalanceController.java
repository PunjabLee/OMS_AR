package com.punjab.oms.ar.controller.finance;
import com.punjab.oms.ar.base.ApiReturn;import com.punjab.oms.ar.service.finance.CustomerBalanceService;import com.punjab.oms.ar.domain.dto.finance.*;
import org.springframework.web.bind.annotation.*;import jakarta.validation.Valid;import lombok.RequiredArgsConstructor;import io.swagger.v3.oas.annotations.Operation;
/** 客户信用余额动作入口。 @author Punjab */
@RestController @RequestMapping("/api/finance/customer-balances") @RequiredArgsConstructor
public class CustomerBalanceController {
 private final CustomerBalanceService service;
 @PostMapping("/transfer") @Operation(summary="负数应收转入客户余额",description="负数应收转入客户余额")
 public ApiReturn<Long> transfer(@Valid @RequestBody BalanceTransferRequest request){return ApiReturn.of(service.transfer(request));}
 @PostMapping("/reverse-transfer") @Operation(summary="撤销客户余额转入",description="撤销客户余额转入")
 public ApiReturn<Long> reverseTransfer(@Valid @RequestBody BalanceTransferReverseRequest request){return ApiReturn.of(service.reverseTransfer(request));}
 @GetMapping("/detail") @Operation(summary="查询客户信用余额",description="查询客户信用余额")
 public ApiReturn<com.punjab.oms.ar.domain.vo.finance.CustomerAccountResponse> detail(@Valid com.punjab.oms.ar.domain.query.finance.BalanceDetailQuery query){return ApiReturn.of(service.detail(query));}
 @GetMapping("/lot-detail") @Operation(summary="查询余额来源批次",description="查询余额来源批次")
 public ApiReturn<com.punjab.oms.ar.domain.vo.finance.BalanceLotResponse> lotDetail(@Valid com.punjab.oms.ar.domain.query.finance.BalanceDetailQuery query){return ApiReturn.of(service.lotDetail(query));}
}
