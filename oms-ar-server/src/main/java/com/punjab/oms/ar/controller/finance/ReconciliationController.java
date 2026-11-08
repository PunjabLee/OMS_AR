package com.punjab.oms.ar.controller.finance;
import com.punjab.oms.ar.service.finance.ReconciliationService;import com.punjab.oms.ar.base.*;import com.punjab.oms.ar.domain.query.finance.ReconciliationQuery;import com.punjab.oms.ar.domain.vo.finance.ReconciliationResponse;import org.springframework.web.bind.annotation.*;import lombok.RequiredArgsConstructor;import jakarta.validation.Valid;import io.swagger.v3.oas.annotations.Operation;
/** 只读对账报告。 @author Punjab */
@RestController @RequiredArgsConstructor public class ReconciliationController {private final ReconciliationService service;@PostMapping("/api/finance/reconciliation/page") @Operation(summary="分页查询金额对账差异",description="分页查询金额对账差异") public ApiReturn<PageResult<ReconciliationResponse>> page(@Valid @RequestBody ReconciliationQuery query){return ApiReturn.of(service.page(query));}
 @PostMapping("/api/finance/cash-accounts/reconciliation/page")
 @Operation(summary="查询现金对账差异",description="查询现金对账差异")
 public ApiReturn<PageResult<ReconciliationResponse>> cashPage(@Valid @RequestBody ReconciliationQuery query) {
     return ApiReturn.of(service.cashPage(query));
 }
}
