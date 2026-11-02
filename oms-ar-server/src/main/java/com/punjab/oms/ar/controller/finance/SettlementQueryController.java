package com.punjab.oms.ar.controller.finance;
import com.punjab.oms.ar.service.finance.SettlementQueryService;import com.punjab.oms.ar.domain.query.finance.SettlementDetailQuery;import com.punjab.oms.ar.domain.vo.finance.SettlementResponse;import com.punjab.oms.ar.base.ApiReturn;import org.springframework.web.bind.annotation.*;import lombok.RequiredArgsConstructor;import jakarta.validation.Valid;import io.swagger.v3.oas.annotations.Operation;
/** 核销只读协议适配。 @author Punjab */
@RestController @RequestMapping("/api/finance/settlements") @RequiredArgsConstructor public class SettlementQueryController {
 private final SettlementQueryService service;
 @GetMapping("/detail") @Operation(summary="查询核销单详情",description="查询核销单详情") public ApiReturn<SettlementResponse> detail(@Valid @ModelAttribute SettlementDetailQuery query){return ApiReturn.of(service.detail(query));}
}
