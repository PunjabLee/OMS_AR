package com.punjab.oms.ar.service.finance.impl;
import com.punjab.oms.ar.service.finance.FinancePageService;import com.punjab.oms.ar.dao.finance.FinancePageDao;import com.punjab.oms.ar.base.*;import com.punjab.oms.ar.domain.query.finance.*;import com.punjab.oms.ar.domain.vo.finance.*;import com.punjab.oms.ar.domain.convert.finance.*;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.*;import lombok.RequiredArgsConstructor;import jakarta.validation.Validator;
/** 总数和记录采用同一一致快照，主体权限由服务端确认。 @author Punjab */
@Service @RequiredArgsConstructor @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public class FinancePageServiceImpl implements FinancePageService {
 private final FinancePageDao dao;private final FinancePageConverter converter;private final SettlementQueryConverter settlements;private final CustomerBalanceConverter balances;private final Validator validator;
 /** 按核销类型和状态等条件分页查询核销记录。 */
 public PageResult<SettlementResponse.Header> settlements(SettlementPageQuery q){validate(q);CurrentUser.require(q.getLegalEntityId(),"ar:read");var page=dao.settlements(converter.toPageQueryBO(q));return new PageResult<>(page.total(),page.page(),page.size(),page.records().stream().map(settlements::toResponse).toList());}
 /** 按核算主体及筛选条件分页查询信用账户。 */
 public PageResult<CustomerAccountResponse> accounts(AccountPageQuery q){validate(q);CurrentUser.require(q.getLegalEntityId(),"ar:read");var page=dao.accounts(converter.toPageQueryBO(q));return new PageResult<>(page.total(),page.page(),page.size(),page.records().stream().map(balances::toResponse).toList());}
 /** 按账户、来源行及状态条件分页查询余额批次。 */
 public PageResult<BalanceLotResponse> lots(LotPageQuery q){validate(q);CurrentUser.require(q.getLegalEntityId(),"ar:read");var page=dao.lots(converter.toPageQueryBO(q));return new PageResult<>(page.total(),page.page(),page.size(),page.records().stream().map(balances::toResponse).toList());}
 /** 验证请求对象及字段约束，拒绝无效输入。 */
 private void validate(Object q){if(q==null || !validator.validate(q).isEmpty())throw new ServiceException("财务分页参数无效");}
}
