package com.punjab.oms.ar.controller.finance;

import com.punjab.oms.ar.base.ApiReturn;
import com.punjab.oms.ar.domain.dto.finance.WithdrawalRequests.*;
import com.punjab.oms.ar.domain.vo.finance.WithdrawalResponse;
import com.punjab.oms.ar.domain.query.finance.BalanceDetailQuery;
import com.punjab.oms.ar.service.finance.WithdrawalService;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;

/** 提现及收款档案。 @author Punjab */
@RestController @RequiredArgsConstructor @RequestMapping("/api/finance/withdrawals")
public class WithdrawalController {
    private final WithdrawalService service;
    @PostMapping("/payout-accounts/bind") @Operation(summary="登记受托收款档案",description="登记受托收款档案")
    public ApiReturn<Long> bind(@Valid @RequestBody Bind request) { return ApiReturn.of(service.bind(request)); }
    @PostMapping("/payout-accounts/disable") @Operation(summary="停用受托收款档案",description="停用受托收款档案")
    public ApiReturn<Long> disable(@Valid @RequestBody Action request) { return ApiReturn.of(service.disable(request)); }
    @PostMapping("/apply") @Operation(summary="申请提现",description="申请提现")
    public ApiReturn<Long> apply(@Valid @RequestBody Apply request) { return ApiReturn.of(service.apply(request)); }
    @PostMapping("/approve") @Operation(summary="批准提现",description="批准提现")
    public ApiReturn<Long> approve(@Valid @RequestBody Action request) { return ApiReturn.of(service.approve(request)); }
    @PostMapping("/reject") @Operation(summary="拒绝提现",description="拒绝提现")
    public ApiReturn<Long> reject(@Valid @RequestBody Action request) { return ApiReturn.of(service.reject(request)); }
    @PostMapping("/cancel") @Operation(summary="撤销提现",description="撤销提现")
    public ApiReturn<Long> cancel(@Valid @RequestBody Action request) { return ApiReturn.of(service.cancel(request)); }
    @PostMapping("/claim") @Operation(summary="认领线下付款",description="认领线下付款")
    public ApiReturn<Long> claim(@Valid @RequestBody Action request) { return ApiReturn.of(service.claim(request)); }
    @PostMapping("/result") @Operation(summary="登记付款结果",description="登记付款结果")
    public ApiReturn<Long> result(@Valid @RequestBody Result request) { return ApiReturn.of(service.result(request)); }
    @GetMapping("/detail") @Operation(summary="提现详情",description="提现详情")
    public ApiReturn<WithdrawalResponse> detail(@Valid @ModelAttribute BalanceDetailQuery query) {
        return ApiReturn.of(service.detail(query.legalEntityId(),query.id()));
    }
}
