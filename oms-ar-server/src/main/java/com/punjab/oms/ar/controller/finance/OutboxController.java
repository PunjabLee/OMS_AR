package com.punjab.oms.ar.controller.finance;
import com.punjab.oms.ar.service.finance.OutboxQueryService;import com.punjab.oms.ar.domain.query.finance.*;import com.punjab.oms.ar.domain.vo.finance.*;import com.punjab.oms.ar.base.*;
import org.springframework.web.bind.annotation.*;import lombok.RequiredArgsConstructor;import jakarta.validation.Valid;import io.swagger.v3.oas.annotations.Operation;
/** 主体授权的通知运营接口。 @author Punjab */
@RestController @RequestMapping("/api/finance/notifications") @RequiredArgsConstructor public class OutboxController {
 private final OutboxQueryService queries;
 @PostMapping("/page") @Operation(summary="通知分页",description="按主体及状态查询，不返回事件原文或租约令牌") public ApiReturn<PageResult<OutboxResponse>> page(@Valid @RequestBody OutboxPageQuery query){return ApiReturn.of(queries.page(query));}
 @GetMapping("/detail") @Operation(summary="通知详情",description="需通知运营权限，包含原始事件快照") public ApiReturn<OutboxDetailResponse> detail(@Valid @ModelAttribute OutboxDetailQuery query){return ApiReturn.of(queries.detail(query));}
}
