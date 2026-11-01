package com.punjab.oms.ar.common.utils;
import com.punjab.oms.ar.domain.dto.finance.ReceivableCreateRequest.ExchangeBasis;import com.punjab.oms.ar.domain.bo.finance.ExchangeCalculationBO;import com.punjab.oms.ar.enums.ExchangeRateDirection;import com.punjab.oms.ar.base.ServiceException;
import org.springframework.stereotype.Component;import java.math.*;import java.util.Currency;
/** 旧新折算金额分别HALF_UP到结算币种精度，差额为新减旧。 @author Punjab */
@Component public class ExchangeDifferenceCalculator {
 public ExchangeCalculationBO calculate(ExchangeBasis basis,String settlementCurrency){
  try{
   if(basis==null || basis.rateDirection()==null || basis.originalCurrency().equals(settlementCurrency) || basis.originalAmount().signum()==0 || basis.oldRate().signum()<=0 || basis.newRate().signum()<=0)throw new IllegalArgumentException();
   int originalScale=Currency.getInstance(basis.originalCurrency()).getDefaultFractionDigits(),scale=Currency.getInstance(settlementCurrency).getDefaultFractionDigits();if(originalScale<0 || scale<0 || scale>8)throw new IllegalArgumentException();basis.originalAmount().setScale(originalScale,RoundingMode.UNNECESSARY);
   BigDecimal oldAmount=convert(basis.originalAmount(),basis.oldRate(),basis.rateDirection(),scale),newAmount=convert(basis.originalAmount(),basis.newRate(),basis.rateDirection(),scale);BigDecimal max=new BigDecimal("10000000000000000");if(oldAmount.abs().compareTo(max)>=0 || newAmount.abs().compareTo(max)>=0 || newAmount.subtract(oldAmount).abs().compareTo(max)>=0)throw new IllegalArgumentException();
   return new ExchangeCalculationBO(oldAmount,newAmount,newAmount.subtract(oldAmount));
  }catch(Exception e){throw new ServiceException("汇率依据、币种精度或折算金额无效");}
 }
 private BigDecimal convert(BigDecimal amount,BigDecimal rate,ExchangeRateDirection direction,int scale){return direction==ExchangeRateDirection.SETTLEMENT_PER_ORIGINAL?amount.multiply(rate).setScale(scale,RoundingMode.HALF_UP):amount.divide(rate,scale,RoundingMode.HALF_UP);}
}
