package com.punjab.oms.ar.controller.finance;
import com.punjab.oms.ar.base.ApiReturn;import com.punjab.oms.ar.service.finance.SettlementService;
import com.punjab.oms.ar.domain.dto.finance.*;import com.punjab.oms.ar.domain.vo.finance.SettlementPreviewResponse;
import org.springframework.web.bind.annotation.*;import jakarta.validation.Valid;import lombok.RequiredArgsConstructor;import io.swagger.v3.oas.annotations.Operation;
/** 收款核销动作端点。 @author Punjab */
@RestController @RequestMapping("/api/finance/settlements") @RequiredArgsConstructor
public class SettlementController {
 private final SettlementService service;
 @PostMapping("/preview") @Operation(summary="预览收款核销",description="预览收款核销")
 public ApiReturn<SettlementPreviewResponse> preview(@Valid @RequestBody SettlementPreviewRequest request){return ApiReturn.of(service.preview(request));}
 @PostMapping("/confirm") @Operation(summary="确认收款核销",description="确认收款核销")
 public ApiReturn<Long> confirm(@Valid @RequestBody SettlementConfirmRequest request){return ApiReturn.of(service.confirm(request));}
 @PostMapping("/reverse") @Operation(summary="反核销",description="反核销")
 public ApiReturn<Long> reverse(@Valid @RequestBody SettlementReverseRequest request){return ApiReturn.of(service.reverse(request));}
}
