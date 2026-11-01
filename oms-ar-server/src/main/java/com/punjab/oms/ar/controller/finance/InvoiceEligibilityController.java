package com.punjab.oms.ar.controller.finance;
import com.punjab.oms.ar.base.ApiReturn;import com.punjab.oms.ar.service.finance.InvoiceEligibilityService;import com.punjab.oms.ar.domain.query.finance.InvoiceEligibilityQuery;import com.punjab.oms.ar.domain.vo.finance.InvoiceEligibilityResponse;
import org.springframework.web.bind.annotation.*;import jakarta.validation.Valid;import lombok.RequiredArgsConstructor;import io.swagger.v3.oas.annotations.Operation;
/** 只读开票额度，不表示登记或占用开票金额。 @author Punjab */
@RestController @RequestMapping("/api/finance/invoice-links") @RequiredArgsConstructor
public class InvoiceEligibilityController {
 private final InvoiceEligibilityService service;
 @PostMapping("/eligibility") @Operation(summary="查询应收开票额度",description="查询应收开票额度")
 public ApiReturn<InvoiceEligibilityResponse> query(@Valid @RequestBody InvoiceEligibilityQuery query){return ApiReturn.of(service.query(query));}
}
