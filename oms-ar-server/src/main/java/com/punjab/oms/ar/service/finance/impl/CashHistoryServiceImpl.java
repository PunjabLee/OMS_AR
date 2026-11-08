package com.punjab.oms.ar.service.finance.impl;

import com.punjab.oms.ar.base.*;
import com.punjab.oms.ar.domain.query.finance.CashHistoryQuery;
import com.punjab.oms.ar.domain.vo.finance.*;
import com.punjab.oms.ar.dao.finance.*;
import com.punjab.oms.ar.domain.convert.finance.CashHistoryConverter;
import com.punjab.oms.ar.service.finance.CashHistoryService;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** 以同一读取快照校验账户归属并分页。 @author Punjab */
@Service @RequiredArgsConstructor @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
public class CashHistoryServiceImpl implements CashHistoryService {
    private final CashHistoryDao history;
    private final CashAccountDao accounts;
    private final CashHistoryConverter converter;
    private final Validator validator;
    /** 校验读取权限、主体及指定账户。 */
    private void validate(CashHistoryQuery q) {
        if(q==null || !validator.validate(q).isEmpty()) throw new ServiceException("现金账户查询参数无效");
        CurrentUser.require(q.getLegalEntityId(),"ar:cash:read");
        if(accounts.find(q.getAccountId(),q.getLegalEntityId())==null) throw new ServiceException("现金账户不存在或不可访问");
    }
    /** 读取原收款、占用及当前可用额。 */
    public PageResult<CashLotResponse> lots(CashHistoryQuery q) {
        validate(q);
        var page=history.lots(converter.toBO(q),q.getPage());
        return new PageResult<>(page.total(),page.page(),page.size(),page.records().stream().map(converter::toResponse).toList());
    }
    /** 读取账户事件和账面、冻结金额的前后值。 */
    public PageResult<CashEntryResponse> entries(CashHistoryQuery q) {
        validate(q);
        var page=history.entries(converter.toBO(q),q.getPage());
        return new PageResult<>(page.total(),page.page(),page.size(),page.records().stream().map(converter::toResponse).toList());
    }
}
