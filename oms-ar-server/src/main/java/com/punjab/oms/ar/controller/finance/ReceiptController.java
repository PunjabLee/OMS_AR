package com.punjab.oms.ar.controller.finance;
import com.punjab.oms.ar.base.ApiReturn;import com.punjab.oms.ar.service.finance.ReceiptService;
import com.punjab.oms.ar.domain.dto.finance.ReceiptCreateRequest;import com.punjab.oms.ar.domain.query.finance.ReceiptDetailQuery;import com.punjab.oms.ar.domain.vo.finance.ReceiptResponse;
import org.springframework.web.bind.annotation.*;import jakarta.validation.Valid;import lombok.RequiredArgsConstructor;import io.swagger.v3.oas.annotations.Operation;
/** 收款适配端点。 @author Punjab */
@RestController @RequestMapping("/api/finance/receipts") @RequiredArgsConstructor
public class ReceiptController {
 private final ReceiptService service;
 @PostMapping("/create") @Operation(summary="登记收款",description="登记收款")
 public ApiReturn<Long> create(@Valid @RequestBody ReceiptCreateRequest request){return ApiReturn.of(service.create(request));}
 @GetMapping("/detail") @Operation(summary="查询收款余额",description="查询收款余额")
 public ApiReturn<ReceiptResponse> detail(@Valid @ModelAttribute ReceiptDetailQuery query){return ApiReturn.of(service.detail(query));}
 @PostMapping("/reverse-deposit") @Operation(summary="整批冲正充值",description="整批冲正充值")
 public ApiReturn<Long> reverseDeposit(@Valid @RequestBody com.punjab.oms.ar.domain.dto.finance.CashDepositReverseRequest request) {
     return ApiReturn.of(service.reverseDeposit(request));
 }
}
