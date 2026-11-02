package com.punjab.oms.ar.service.finance.impl;
import com.punjab.oms.ar.service.finance.FinancePageService;import com.punjab.oms.ar.dao.finance.FinancePageDao;import com.punjab.oms.ar.base.*;import com.punjab.oms.ar.domain.query.finance.*;import com.punjab.oms.ar.domain.vo.finance.*;import com.punjab.oms.ar.domain.convert.finance.*;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.*;import lombok.RequiredArgsConstructor;import jakarta.validation.Validator;
/** 总数和记录采用同一一致快照，主体权限由服务端确认。 @author Punjab */
@Service @RequiredArgsConstructor @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public class FinancePageServiceImpl implements FinancePageService {
 private final FinancePageDao dao;private final FinancePageConverter converter;private final SettlementQueryConverter settlements;private final CustomerBalanceConverter balances;private final Validator validator;
 public PageResult<SettlementResponse.Header> settlements(SettlementPageQuery q){validate(q);CurrentUser.require(q.getLegalEntityId(),"ar:read");var page=dao.settlements(converter.toPageQueryBO(q));return new PageResult<>(page.total(),page.page(),page.size(),page.records().stream().map(settlements::toResponse).toList());}
 public PageResult<CustomerAccountResponse> accounts(AccountPageQuery q){validate(q);CurrentUser.require(q.getLegalEntityId(),"ar:read");var page=dao.accounts(converter.toPageQueryBO(q));return new PageResult<>(page.total(),page.page(),page.size(),page.records().stream().map(balances::toResponse).toList());}
 public PageResult<BalanceLotResponse> lots(LotPageQuery q){validate(q);CurrentUser.require(q.getLegalEntityId(),"ar:read");var page=dao.lots(converter.toPageQueryBO(q));return new PageResult<>(page.total(),page.page(),page.size(),page.records().stream().map(balances::toResponse).toList());}
 private void validate(Object q){if(q==null || !validator.validate(q).isEmpty())throw new ServiceException("财务分页参数无效");}
}
