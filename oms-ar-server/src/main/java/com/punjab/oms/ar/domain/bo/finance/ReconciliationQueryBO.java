package com.punjab.oms.ar.domain.bo.finance;
import com.punjab.oms.ar.base.QueryPage;import lombok.Getter;import lombok.Setter;
/** 对账持久层范围。 @author Punjab */
@Getter @Setter public class ReconciliationQueryBO extends QueryPage {private Long legalEntityId;private Long customerId;private String currency;public long getOffset(){return ((long)getPage()-1)*getSize();}}
