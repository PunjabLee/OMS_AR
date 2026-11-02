package com.punjab.oms.ar.domain.bo.finance;
import com.punjab.oms.ar.base.QueryPage;import lombok.Getter;import lombok.Setter;
/** 财务分页筛选范围。 @author Punjab */
@Getter @Setter public class LotPageQueryBO extends QueryPage {
private Long legalEntityId;
private Long customerId;
private String currency;
private String status;
private Long accountId;
private Long sourceLineId;
}
