package com.punjab.oms.ar.controller.finance;
import com.punjab.oms.ar.base.ApiReturn;import com.punjab.oms.ar.service.finance.InvoiceService;import com.punjab.oms.ar.domain.dto.finance.*;import com.punjab.oms.ar.domain.query.finance.InvoiceDetailQuery;import com.punjab.oms.ar.domain.vo.finance.*;
import org.springframework.web.bind.annotation.*;import jakarta.validation.Valid;import lombok.RequiredArgsConstructor;import io.swagger.v3.oas.annotations.Operation;
/** 登记本系统发票关联，不调用外部税务平台。 @author Punjab */
@RestController @RequestMapping("/api/finance/invoice-links") @RequiredArgsConstructor
public class InvoiceController {
 private final InvoiceService service;
 @PostMapping("/preview") @Operation(summary="预览发票应收关联",description="预览发票应收关联")
 public ApiReturn<InvoicePreviewResponse> preview(@Valid @RequestBody InvoicePreviewRequest request){return ApiReturn.of(service.preview(request));}
 @PostMapping("/register") @Operation(summary="登记发票应收关联",description="登记发票应收关联")
 public ApiReturn<Long> register(@Valid @RequestBody InvoiceRegisterRequest request){return ApiReturn.of(service.register(request));}
 @PostMapping("/reverse") @Operation(summary="撤销发票应收关联",description="撤销发票应收关联")
 public ApiReturn<Long> reverse(@Valid @RequestBody InvoiceReverseRequest request){return ApiReturn.of(service.reverse(request));}
 @GetMapping("/detail") @Operation(summary="查询发票应收关联",description="查询发票应收关联")
 public ApiReturn<InvoiceResponse> detail(@Valid InvoiceDetailQuery query){return ApiReturn.of(service.detail(query));}
}
