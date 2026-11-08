package com.punjab.oms.ar.controller.finance;

import com.punjab.oms.ar.domain.dto.finance.*;
import com.punjab.oms.ar.domain.query.finance.BalanceDetailQuery;
import com.punjab.oms.ar.domain.vo.finance.CashAccountResponse;
import com.punjab.oms.ar.base.ApiReturn;
import com.punjab.oms.ar.service.finance.CashAccountService;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/** 客户现金账户接口。 @author Punjab */
@RestController @RequiredArgsConstructor @RequestMapping("/api/finance/cash-accounts")
public class CashAccountController {
    private final CashAccountService service;
    private final com.punjab.oms.ar.service.finance.CashSettlementService settlements;

    @Operation(summary="预览现金账户核销",description="预览现金账户核销") @PostMapping("/settlements/preview")
    public ApiReturn<com.punjab.oms.ar.domain.vo.finance.CashSettlementView> preview(@Valid @RequestBody BalanceSettlementPreviewRequest request) {
        return ApiReturn.of(settlements.preview(request));
    }
    @Operation(summary="确认现金账户核销",description="确认现金账户核销") @PostMapping("/settlements/confirm")
    public ApiReturn<com.punjab.oms.ar.domain.vo.finance.CashSettlementView.Posted> confirm(@Valid @RequestBody BalanceSettlementConfirmRequest request) {
        return ApiReturn.of(settlements.confirm(request));
    }
    @Operation(summary="关闭现金账户",description="关闭现金账户") @PostMapping("/close")
    public ApiReturn<Long> close(@Valid @RequestBody CashStatusRequest request) {
        return ApiReturn.of(service.close(request));
    }
    @Operation(summary="恢复现金账户",description="恢复现金账户") @PostMapping("/reopen")
    public ApiReturn<Long> reopen(@Valid @RequestBody CashStatusRequest request) {
        return ApiReturn.of(service.reopen(request));
    }
    @Operation(summary="整户止付",description="整户止付") @PostMapping("/stop")
    public ApiReturn<Long> stop(@Valid @RequestBody CashStatusRequest request) {
        return ApiReturn.of(service.stop(request));
    }
    @Operation(summary="解除整户止付",description="解除整户止付") @PostMapping("/resume")
    public ApiReturn<Long> resume(@Valid @RequestBody CashStatusRequest request) {
        return ApiReturn.of(service.resume(request));
    }
    @Operation(summary="开设现金账户",description="开设现金账户") @PostMapping("/open")
    public ApiReturn<Long> open(@Valid @RequestBody CashAccountOpenRequest request) {
        return ApiReturn.of(service.open(request));
    }
    @Operation(summary="查询现金余额",description="查询现金余额") @GetMapping("/detail")
    public ApiReturn<CashAccountResponse> detail(@Valid @ModelAttribute BalanceDetailQuery query) {
        return ApiReturn.of(service.detail(query));
    }
}
