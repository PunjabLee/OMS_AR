package com.punjab.oms.ar.domain.query.finance;
import com.punjab.oms.ar.base.QueryPage;
import jakarta.validation.constraints.*;
/** 分页范围。 @author Punjab */
public class ReceivablePageQuery extends QueryPage {
 @NotNull @Positive private Long legalEntityId;
 @Positive private Long customerId;
 public Long getLegalEntityId(){return legalEntityId;} public void setLegalEntityId(Long v){legalEntityId=v;}
 public Long getCustomerId(){return customerId;} public void setCustomerId(Long v){customerId=v;}
 public long getOffset(){return ((long)getPage()-1)*getSize();}
}
