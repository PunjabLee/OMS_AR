package com.punjab.oms.ar.controller.finance;
import com.punjab.oms.ar.service.finance.FinancePageService;import com.punjab.oms.ar.base.*;import com.punjab.oms.ar.domain.query.finance.*;import com.punjab.oms.ar.domain.vo.finance.*;import org.springframework.web.bind.annotation.*;import lombok.RequiredArgsConstructor;import jakarta.validation.Valid;import io.swagger.v3.oas.annotations.Operation;
/** 核销、账户及批次分页协议。 @author Punjab */
@RestController @RequiredArgsConstructor public class FinancePageController {private final FinancePageService service;
 @PostMapping("/api/finance/settlements/page") @Operation(summary="分页查询核销单",description="分页查询核销单") public ApiReturn<PageResult<SettlementResponse.Header>> settlements(@Valid @RequestBody SettlementPageQuery query){return ApiReturn.of(service.settlements(query));}
 @PostMapping("/api/finance/customer-balances/page") @Operation(summary="分页查询客户账户",description="分页查询客户账户") public ApiReturn<PageResult<CustomerAccountResponse>> accounts(@Valid @RequestBody AccountPageQuery query){return ApiReturn.of(service.accounts(query));}
 @PostMapping("/api/finance/customer-balances/lots/page") @Operation(summary="分页查询余额批次",description="分页查询余额批次") public ApiReturn<PageResult<BalanceLotResponse>> lots(@Valid @RequestBody LotPageQuery query){return ApiReturn.of(service.lots(query));}
}
