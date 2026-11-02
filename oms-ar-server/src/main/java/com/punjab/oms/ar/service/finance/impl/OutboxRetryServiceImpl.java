package com.punjab.oms.ar.service.finance.impl;
import com.punjab.oms.ar.service.finance.*;import com.punjab.oms.ar.dao.finance.OutboxRetryDao;import com.punjab.oms.ar.domain.dto.finance.*;import com.punjab.oms.ar.domain.query.finance.OutboxRetryPageQuery;import com.punjab.oms.ar.domain.vo.finance.OutboxRetryResponse;import com.punjab.oms.ar.domain.entity.finance.ArOutboxRetry;import com.punjab.oms.ar.domain.convert.finance.OutboxRetryConverter;import com.punjab.oms.ar.common.utils.FinanceValues;import com.punjab.oms.ar.base.*;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.*;import lombok.RequiredArgsConstructor;import jakarta.validation.Validator;import java.util.Set;
/** 重试操作、前后状态审计和幂等结果同事务提交，不追加财务通知事件。 @author Punjab */
@Service @RequiredArgsConstructor public class OutboxRetryServiceImpl implements OutboxRetryService {
 private final OutboxRetryDao dao;private final FinanceJournalService journal;private final FinanceValues values;private final OutboxRetryConverter converter;private final Validator validator;
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public Long retry(OutboxRetryRequest request){
  validate(request);var ticket=journal.begin(new JournalBeginRequest(request.legalEntityId(),"ar:outbox:retry","OUTBOX_RETRY",request.requestKey(),values.json(request)));
  if(ticket.resultReference()!=null)return ticket.resultReference();
  var before=dao.notification(request.outboxId(),request.legalEntityId(),true);if(before==null)throw new ServiceException("通知不存在或不可访问");
  if(!request.expectedVersion().equals(before.getVersion()))throw new ServiceException("通知版本已变化，请刷新后重试");
  if(!"FINANCE_EVENTS".equals(before.getDestination()) || !Set.of("RETRY","DEAD").contains(before.getStatus()))throw new ServiceException("仅失败待重试或已停止通知允许人工重试");
  if(!dao.reset(before.getId(),request.legalEntityId(),before.getVersion(),ticket.actor()))throw new ServiceException("通知版本已变化，请刷新后重试");
  var after=dao.notification(before.getId(),request.legalEntityId(),false);
  var audit=values.initialize(new ArOutboxRetry(),ticket.actor());audit.setLegalEntityId(request.legalEntityId());audit.setOutboxId(before.getId());audit.setOperationId(ticket.id());audit.setReason(request.reason());
  audit.setBeforeStatus(before.getStatus());audit.setBeforeAttemptCount(before.getAttemptCount());audit.setBeforeNextAttemptAt(before.getNextAttemptAt());audit.setBeforeLastError(before.getLastError());audit.setBeforeVersion(before.getVersion());
  audit.setAfterStatus(after.getStatus());audit.setAfterAttemptCount(after.getAttemptCount());audit.setAfterNextAttemptAt(after.getNextAttemptAt());audit.setAfterVersion(after.getVersion());
  if(!dao.create(audit))throw new ServiceException("通知重试审计保存失败");journal.complete(ticket,audit.getId());return audit.getId();
 }
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public PageResult<OutboxRetryResponse> history(OutboxRetryPageQuery query){validate(query);CurrentUser.require(query.getLegalEntityId(),"ar:outbox:read");if(dao.notification(query.getOutboxId(),query.getLegalEntityId(),false)==null)throw new ServiceException("通知不存在或不可访问");var page=dao.history(converter.toPageQueryBO(query));return new PageResult<>(page.total(),page.page(),page.size(),page.records().stream().map(converter::toResponse).toList());}
 private void validate(Object value){if(value==null || !validator.validate(value).isEmpty())throw new ServiceException("通知重试参数无效");}
}
