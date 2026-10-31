package com.punjab.oms.ar.controller.finance;
import com.punjab.oms.ar.service.finance.*;
import com.punjab.oms.ar.base.*;
import com.punjab.oms.ar.domain.dto.finance.ReceivableCreateRequest;
import com.punjab.oms.ar.domain.query.finance.*;
import com.punjab.oms.ar.domain.vo.finance.ReceivableResponse;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
/** 应收HTTP协议适配。 @author Punjab */
@RestController @RequestMapping("/api/finance/receivables") @RequiredArgsConstructor
public class ReceivableController {
 private final ReceivableService service;private final ReceivableQueryService queries;
 @Operation(summary="创建应收单",description="创建应收单")
 @PostMapping("/create") public ApiReturn<Long> create(@Valid @RequestBody ReceivableCreateRequest request){return ApiReturn.of(service.create(request));}
 @Operation(summary="查询应收详情",description="查询应收详情")
 @GetMapping("/detail") public ApiReturn<ReceivableResponse> detail(@Valid @ModelAttribute ReceivableDetailQuery query){return ApiReturn.of(queries.detail(query));}
 @Operation(summary="分页查询应收单",description="分页查询应收单")
 @PostMapping("/page") public ApiReturn<PageResult<ReceivableResponse.Document>> page(@Valid @RequestBody ReceivablePageQuery query){return ApiReturn.of(queries.page(query));}
}
