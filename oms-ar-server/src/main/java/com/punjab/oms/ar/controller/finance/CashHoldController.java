package com.punjab.oms.ar.controller.finance;
import com.punjab.oms.ar.base.ApiReturn;
import com.punjab.oms.ar.domain.dto.finance.CashHoldRequest.*;
import com.punjab.oms.ar.domain.vo.finance.CashHoldView;
import com.punjab.oms.ar.service.finance.CashHoldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/finance/cash-accounts")
public class CashHoldController {
 private final CashHoldService service;
 @Operation(summary="按金额冻结",description="按充值批次顺序冻结可用余额") @PostMapping("/freeze")
 public ApiReturn<Long> freeze(@Valid @RequestBody Freeze request){return ApiReturn.of(service.freeze(request));}
 @Operation(summary="解除金额冻结",description="按原批次部分或全部解冻") @PostMapping("/unfreeze")
 public ApiReturn<Long> unfreeze(@Valid @RequestBody Release request){return ApiReturn.of(service.unfreeze(request));}
 @Operation(summary="查询冻结详情",description="查询原冻结批次与未释放金额") @GetMapping("/holds/detail")
 public ApiReturn<CashHoldView> detail(@RequestParam Long legalEntityId,@RequestParam Long accountId,@RequestParam Long holdId){
  return ApiReturn.of(service.detail(legalEntityId,accountId,holdId));
 }
}
