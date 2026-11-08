package com.punjab.oms.ar.service.finance.impl;
import com.punjab.oms.ar.service.finance.ReconciliationService;import com.punjab.oms.ar.base.*;import com.punjab.oms.ar.domain.query.finance.ReconciliationQuery;import com.punjab.oms.ar.domain.vo.finance.ReconciliationResponse;import com.punjab.oms.ar.domain.convert.finance.ReconciliationConverter;import com.punjab.oms.ar.dao.finance.ReconciliationDao;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.*;import lombok.RequiredArgsConstructor;import jakarta.validation.Validator;
/** 同一快照核对结算范围内余额和有效分配。 @author Punjab */
@Service @RequiredArgsConstructor public class ReconciliationServiceImpl implements ReconciliationService {
 private final ReconciliationDao dao;private final ReconciliationConverter converter;private final Validator validator;
 /** 在同一读取快照按主体、客户和币种重算差异，仅返回当前页异常对象。 */
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public PageResult<ReconciliationResponse> page(ReconciliationQuery query){if(query==null || !validator.validate(query).isEmpty())throw new ServiceException("对账范围或分页参数无效");CurrentUser.require(query.getLegalEntityId(),"ar:read");var result=dao.page(converter.toPageQueryBO(query));return new PageResult<>(result.total(),result.page(),result.size(),result.records().stream().map(converter::toResponse).toList());}
 /** 在同一快照计算现金投影与业务凭证差异。 */
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public PageResult<ReconciliationResponse> cashPage(ReconciliationQuery query) {
     if(query==null || !validator.validate(query).isEmpty()) throw new ServiceException("现金对账范围或分页参数无效");
     CurrentUser.require(query.getLegalEntityId(),"ar:cash:read");
     var result=dao.cashPage(converter.toPageQueryBO(query));
     return new PageResult<>(result.total(),result.page(),result.size(),result.records().stream().map(converter::toResponse).toList());
 }
}
