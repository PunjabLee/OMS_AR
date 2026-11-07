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
    @Operation(summary="开设现金账户",description="开设现金账户") @PostMapping("/open")
    public ApiReturn<Long> open(@Valid @RequestBody CashAccountOpenRequest request) {
        return ApiReturn.of(service.open(request));
    }
    @Operation(summary="查询现金余额",description="查询现金余额") @GetMapping("/detail")
    public ApiReturn<CashAccountResponse> detail(@Valid @ModelAttribute BalanceDetailQuery query) {
        return ApiReturn.of(service.detail(query));
    }
}
