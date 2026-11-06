package com.punjab.oms.ar.service.finance.impl;
import com.punjab.oms.ar.service.finance.OutboxLeaseService;import com.punjab.oms.ar.dao.finance.OutboxLeaseDao;import com.punjab.oms.ar.domain.dto.finance.*;import com.punjab.oms.ar.domain.vo.finance.OutboxDeliveryResponse;import com.punjab.oms.ar.domain.convert.finance.OutboxDeliveryConverter;import com.punjab.oms.ar.base.ServiceException;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.*;import lombok.RequiredArgsConstructor;import jakarta.validation.Validator;
/** 仅后台派发器调用：领取与确认各自提交短事务，网络不进入该服务。 @author Punjab */
@Service @RequiredArgsConstructor @Transactional(propagation=Propagation.REQUIRES_NEW,isolation=Isolation.READ_COMMITTED,rollbackFor=Exception.class)
public class OutboxLeaseServiceImpl implements OutboxLeaseService {
 private final OutboxLeaseDao dao;private final OutboxDeliveryConverter converter;private final Validator validator;
 /** 锁定可领取通知，校验剩余尝试次数并设置新的限时租约。 */
 public OutboxDeliveryResponse claim(OutboxClaimRequest request){
  validate(request);var row=dao.candidate(request.legalEntityId());if(row==null)return null;
  if(row.getAttemptCount()>=request.maxAttempts()){if(!dao.exhausted(row.getId(),row.getVersion()))throw new ServiceException("通知租约状态已变化");return null;}
  if(request.leaseToken().equals(row.getLeaseToken()))throw new ServiceException("通知重领必须使用新租约");
  if(!dao.acquire(row.getId(),row.getVersion(),request.leaseToken(),request.leaseSeconds()))throw new ServiceException("通知租约状态已变化");
  row.setAttemptCount(row.getAttemptCount()+1);return converter.toResponse(row);
 }
 /** 按租约凭证及有效期提交投递结果，失败时按次数安排重试或停止派发。 */
 public boolean finish(OutboxFinishRequest request){
  validate(request);if((request.success() && request.errorCode()!=null) || (!request.success() && request.errorCode()==null))throw new ServiceException("通知结果分类无效");
  return dao.finish(request.id(),request.legalEntityId(),request.leaseToken(),request.success(),request.errorCode(),request.maxAttempts(),request.retrySeconds());
 }
 /** 验证请求对象及字段约束，拒绝无效输入。 */
 private void validate(Object value){if(value==null || !validator.validate(value).isEmpty())throw new ServiceException("通知租约参数无效");}
}
