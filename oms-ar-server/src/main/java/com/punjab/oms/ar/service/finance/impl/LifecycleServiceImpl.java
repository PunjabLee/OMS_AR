package com.punjab.oms.ar.service.finance.impl;
import com.punjab.oms.ar.service.finance.LifecycleService;import com.punjab.oms.ar.base.*;import com.punjab.oms.ar.domain.query.finance.LifecycleQuery;import com.punjab.oms.ar.domain.vo.finance.LifecycleResponse;import com.punjab.oms.ar.domain.convert.finance.LifecycleConverter;import com.punjab.oms.ar.dao.finance.LifecycleDao;import com.punjab.oms.ar.domain.entity.finance.*;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.*;import lombok.RequiredArgsConstructor;import jakarta.validation.Validator;import com.fasterxml.jackson.databind.*;import java.util.*;import java.util.stream.Collectors;
/** 一致快照中的事件分页及当前单据变化。 @author Punjab */
@Service @RequiredArgsConstructor public class LifecycleServiceImpl implements LifecycleService {
 private final LifecycleDao dao;private final LifecycleConverter converter;private final Validator validator;private final ObjectMapper json;
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public PageResult<LifecycleResponse> page(LifecycleQuery query){
  if(query==null || !validator.validate(query).isEmpty())throw new ServiceException("生命周期查询参数无效");CurrentUser.require(query.getLegalEntityId(),"ar:read");var q=converter.toPageQueryBO(query);if(!dao.accessible(q))throw new ServiceException("应收单不存在或不可访问");
  long total=dao.count(q);var events=dao.page(q);var changes=dao.changes(q,events.stream().map(ArEvent::getId).toList()).stream().collect(Collectors.groupingBy(ArChange::getEventId));
  var result=events.stream().map(e->new LifecycleResponse(e.getId(),e.getEventType(),e.getOccurredAt(),e.getRecordedAt(),e.getActor(),e.getReason(),e.getTriggerType(),e.getTriggerId(),e.getCorrelationId(),e.getReverseOfId(),e.getSchemaVersion(),changes.getOrDefault(e.getId(),List.of()).stream().map(c->new LifecycleResponse.Change(c.getId(),c.getObjectType(),c.getObjectId(),parse(c.getBeforeSnapshot()),parse(c.getDeltaSnapshot()),parse(c.getAfterSnapshot()),c.getBeforeVersion(),c.getAfterVersion())).toList())).toList();return new PageResult<>(total,query.getPage(),query.getSize(),result);
 }
 private JsonNode parse(String snapshot){if(snapshot==null)return null;try{return json.readTree(snapshot);}catch(Exception e){throw new ServiceException("生命周期快照格式异常");}}
}
