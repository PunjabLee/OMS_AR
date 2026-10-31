package com.punjab.oms.ar.service.finance.impl;

import com.punjab.oms.ar.base.ServiceException;
import com.punjab.oms.ar.service.finance.AllocationService;
import com.punjab.oms.ar.domain.dto.finance.AllocationPreviewRequest;
import com.punjab.oms.ar.domain.vo.finance.AllocationPreviewResponse;
import com.punjab.oms.ar.domain.vo.finance.AllocationPreviewResponse.Allocation;
import com.punjab.oms.ar.enums.AllocationStrategy;
import com.punjab.oms.ar.enums.AmountType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import org.springframework.stereotype.Service;

/** 无持久化副作用的核销分配实现。 @author Punjab */
@Service
public class AllocationServiceImpl implements AllocationService {
    private static final BigDecimal LIMIT = new BigDecimal("10000000000000000");

    @Override
    public AllocationPreviewResponse preview(AllocationPreviewRequest request) {
        if (request == null || request.currencyScale() < 0 || request.currencyScale() > 8
                || request.groups() == null) {
            throw new ServiceException("核销请求或币种精度无效");
        }
        if (request.strategy() != AllocationStrategy.GROUP_TAX_FIRST) {
            throw new ServiceException("核销策略尚未启用");
        }
        validateMoney(request.availableAmount(), request.currencyScale());
        var groups = new HashSet<Long>();
        var lines = new HashSet<Long>();
        // 必须先校验全部目标，即使资金已分配完，也不得忽略后续非法行。
        for (var group : request.groups()) {
            if (group == null || group.groupId() == null || group.groupId() <= 0
                    || !groups.add(group.groupId()) || group.lines() == null) {
                throw new ServiceException("业务组无效或重复");
            }
            for (var line : group.lines()) {
                if (line == null || line.lineId() == null || line.lineId() <= 0
                        || !lines.add(line.lineId()) || line.amountType() == null) {
                    throw new ServiceException("应收行无效或重复");
                }
                validateMoney(line.openAmount(), request.currencyScale());
            }
        }
        var result = new ArrayList<Allocation>();
        var remaining = request.availableAmount();
        for (var group : request.groups()) {
            for (var type : new AmountType[] {AmountType.OUTPUT_TAX, AmountType.PRINCIPAL}) {
                for (var line : group.lines()) {
                    if (line.amountType() != type || remaining.signum() == 0) continue;
                    var amount = remaining.min(line.openAmount());
                    if (amount.signum() > 0) {
                        result.add(new Allocation(group.groupId(), line.lineId(), type, amount));
                        remaining = remaining.subtract(amount);
                    }
                }
            }
        }
        return new AllocationPreviewResponse(result, remaining);
    }

    private void validateMoney(BigDecimal amount, int scale) {
        if (amount == null || amount.signum() < 0 || amount.compareTo(LIMIT) >= 0) {
            throw new ServiceException("核销金额或目标余额无效");
        }
        try {
            amount.setScale(scale, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ServiceException("金额超出币种精度");
        }
    }
}
