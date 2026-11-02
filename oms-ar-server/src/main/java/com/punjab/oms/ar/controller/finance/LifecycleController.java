package com.punjab.oms.ar.controller.finance;
import com.punjab.oms.ar.service.finance.LifecycleService;import com.punjab.oms.ar.domain.query.finance.LifecycleQuery;import com.punjab.oms.ar.domain.vo.finance.LifecycleResponse;import com.punjab.oms.ar.base.*;import org.springframework.web.bind.annotation.*;import lombok.RequiredArgsConstructor;import jakarta.validation.Valid;import io.swagger.v3.oas.annotations.Operation;
/** 应收生命周期查询协议。 @author Punjab */
@RestController @RequestMapping("/api/finance/receivables/lifecycle") @RequiredArgsConstructor public class LifecycleController {
 private final LifecycleService service;
 @PostMapping("/page") @Operation(summary="分页查询应收生命周期",description="分页查询应收生命周期") public ApiReturn<PageResult<LifecycleResponse>> page(@Valid @RequestBody LifecycleQuery query){return ApiReturn.of(service.page(query));}
}
