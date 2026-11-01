package com.punjab.oms.ar.controller.finance;
import com.punjab.oms.ar.base.ApiReturn;import com.punjab.oms.ar.service.finance.OffsetService;
import com.punjab.oms.ar.domain.dto.finance.*;import com.punjab.oms.ar.domain.vo.finance.OffsetPreviewResponse;
import org.springframework.web.bind.annotation.*;import jakarta.validation.Valid;import lombok.RequiredArgsConstructor;import io.swagger.v3.oas.annotations.Operation;
/** 正负应收抵销动作端点。 @author Punjab */
@RestController @RequestMapping("/api/finance/offsets") @RequiredArgsConstructor
public class OffsetController {
 private final OffsetService service;
 @PostMapping("/preview") @Operation(summary="预览正负应收抵销",description="预览正负应收抵销")
 public ApiReturn<OffsetPreviewResponse> preview(@Valid @RequestBody OffsetPreviewRequest request){return ApiReturn.of(service.preview(request));}
 @PostMapping("/confirm") @Operation(summary="确认正负应收抵销",description="确认正负应收抵销")
 public ApiReturn<Long> confirm(@Valid @RequestBody OffsetConfirmRequest request){return ApiReturn.of(service.confirm(request));}
}
