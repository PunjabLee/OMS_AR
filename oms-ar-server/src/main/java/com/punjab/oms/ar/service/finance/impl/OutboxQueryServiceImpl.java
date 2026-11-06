package com.punjab.oms.ar.service.finance.impl;
import com.punjab.oms.ar.service.finance.OutboxQueryService;import com.punjab.oms.ar.dao.finance.OutboxQueryDao;import com.punjab.oms.ar.base.*;import com.punjab.oms.ar.domain.query.finance.*;import com.punjab.oms.ar.domain.vo.finance.*;import com.punjab.oms.ar.domain.convert.finance.OutboxQueryConverter;
import com.fasterxml.jackson.databind.*;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.*;import lombok.RequiredArgsConstructor;import jakarta.validation.Validator;
/** 查询范围独立授权，分页采用同一一致快照。 @author Punjab */
@Service @RequiredArgsConstructor @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
public class OutboxQueryServiceImpl implements OutboxQueryService {
 private final OutboxQueryDao dao;private final OutboxQueryConverter converter;private final Validator validator;private final ObjectMapper json;
 /** 按主体和运行状态分页读取通知元数据，负载仅由详情查询读取。 */
 public PageResult<OutboxResponse> page(OutboxPageQuery query){validate(query);CurrentUser.require(query.getLegalEntityId(),"ar:outbox:read");var page=dao.page(converter.toPageQueryBO(query));return new PageResult<>(page.total(),page.page(),page.size(),page.records().stream().map(converter::toResponse).toList());}
 /** 校验通知读取权限及主体归属，解析已保存的事件负载供运营查询。 */
 public OutboxDetailResponse detail(OutboxDetailQuery query){validate(query);CurrentUser.require(query.legalEntityId(),"ar:outbox:read");var row=dao.detail(converter.toDetailQueryBO(query));if(row==null)throw new ServiceException("通知不存在或不可访问");
  try{return new OutboxDetailResponse(converter.toResponse(row),json.reader().with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree(row.getPayload()));}
  catch(java.io.IOException e){throw new ServiceException("通知原文解析失败");}
 }
 /** 验证请求对象及字段约束，拒绝无效输入。 */
 private void validate(Object query){if(query==null || !validator.validate(query).isEmpty())throw new ServiceException("通知查询参数无效");}
}
