package com.punjab.oms.ar.controller.finance;

import com.punjab.oms.ar.base.*;
import com.punjab.oms.ar.domain.query.finance.CashHistoryQuery;
import com.punjab.oms.ar.domain.vo.finance.*;
import com.punjab.oms.ar.service.finance.CashHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

/** 现金来源及账户流水接口。 @author Punjab */
@RestController @RequiredArgsConstructor @RequestMapping("/api/finance/cash-accounts")
public class CashHistoryController {
    private final CashHistoryService history;
    @Operation(summary="现金来源批次",description="现金来源批次") @PostMapping("/lots/page")
    public ApiReturn<PageResult<CashLotResponse>> lots(@Valid @RequestBody CashHistoryQuery q) {
        return ApiReturn.of(history.lots(q));
    }
    @Operation(summary="现金账户流水",description="现金账户流水") @PostMapping("/ledger/page")
    public ApiReturn<PageResult<CashEntryResponse>> entries(@Valid @RequestBody CashHistoryQuery q) {
        return ApiReturn.of(history.entries(q));
    }
}
