package com.punjab.oms.ar.common.utils;
import java.math.*;import java.time.LocalDateTime;import java.util.Currency;
import com.punjab.oms.ar.base.ServiceException;
import com.punjab.oms.ar.domain.entity.finance.ArBaseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
/** 服务层金额、显式审计和快照工具。 @author Punjab */
@Component @RequiredArgsConstructor
public class FinanceValues {
 private final ObjectMapper json;
 public static boolean sameTaxRate(BigDecimal left,BigDecimal right){return left==null?right==null:right!=null && left.compareTo(right)==0;}
 public String json(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new ServiceException("快照序列化失败");}}
 public void positiveMoney(String currency,BigDecimal amount){
  try{int scale=Currency.getInstance(currency).getDefaultFractionDigits();if(scale<0||scale>8||amount==null||amount.signum()<=0||amount.abs().compareTo(new BigDecimal("10000000000000000"))>=0)throw new IllegalArgumentException();amount.setScale(scale,RoundingMode.UNNECESSARY);}
  catch(Exception e){throw new ServiceException("金额或币种精度无效");}
 }
 public <T extends ArBaseEntity>T initialize(T value,String actor){var now=LocalDateTime.now();value.setCreatedTime(now);value.setUpdatedTime(now);value.setCreatedBy(actor);value.setUpdatedBy(actor);value.setDeleteStatus(false);return value;}
}
