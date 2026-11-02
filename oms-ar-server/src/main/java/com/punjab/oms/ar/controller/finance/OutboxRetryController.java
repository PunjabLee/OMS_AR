package com.punjab.oms.ar.controller.finance;
import com.punjab.oms.ar.service.finance.OutboxRetryService;import com.punjab.oms.ar.domain.dto.finance.OutboxRetryRequest;import com.punjab.oms.ar.domain.query.finance.OutboxRetryPageQuery;import com.punjab.oms.ar.domain.vo.finance.OutboxRetryResponse;import com.punjab.oms.ar.base.*;
import org.springframework.web.bind.annotation.*;import lombok.RequiredArgsConstructor;import jakarta.validation.Valid;import io.swagger.v3.oas.annotations.Operation;
/** 重试写权限和运营读权限分别校验。 @author Punjab */
@RestController @RequestMapping("/api/finance/notifications") @RequiredArgsConstructor public class OutboxRetryController {
 private final OutboxRetryService service;
 @PostMapping("/retry") @Operation(summary="人工重试通知",description="仅RETRY或DEAD，原因必填，按版本并发保护，返回审计ID") public ApiReturn<Long> retry(@Valid @RequestBody OutboxRetryRequest request){return ApiReturn.of(service.retry(request));}
 @PostMapping("/retries/page") @Operation(summary="人工重试记录",description="查询指定主体通知的不可变重试前后状态") public ApiReturn<PageResult<OutboxRetryResponse>> history(@Valid @RequestBody OutboxRetryPageQuery query){return ApiReturn.of(service.history(query));}
}
