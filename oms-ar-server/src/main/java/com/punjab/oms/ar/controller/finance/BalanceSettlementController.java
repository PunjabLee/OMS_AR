package com.punjab.oms.ar.controller.finance;
import com.punjab.oms.ar.base.ApiReturn;import com.punjab.oms.ar.service.finance.BalanceSettlementService;
import com.punjab.oms.ar.domain.dto.finance.*;import com.punjab.oms.ar.domain.vo.finance.BalanceSettlementPreviewResponse;
import org.springframework.web.bind.annotation.*;import jakarta.validation.Valid;import lombok.RequiredArgsConstructor;import io.swagger.v3.oas.annotations.Operation;
/** 客户余额抵扣动作端点。 @author Punjab */
@RestController @RequestMapping("/api/finance/customer-balances/settlements") @RequiredArgsConstructor
public class BalanceSettlementController {
 private final BalanceSettlementService service;
 @PostMapping("/preview") @Operation(summary="预览客户余额抵扣",description="预览客户余额抵扣")
 public ApiReturn<BalanceSettlementPreviewResponse> preview(@Valid @RequestBody BalanceSettlementPreviewRequest request){return ApiReturn.of(service.preview(request));}
 @PostMapping("/confirm") @Operation(summary="确认客户余额抵扣",description="确认客户余额抵扣")
 public ApiReturn<Long> confirm(@Valid @RequestBody BalanceSettlementConfirmRequest request){return ApiReturn.of(service.confirm(request));}
}
