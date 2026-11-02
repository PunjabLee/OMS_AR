package com.punjab.oms.ar.domain.bo.finance;
import com.punjab.oms.ar.base.QueryPage;import lombok.Getter;import lombok.Setter;
/** 通知持久层查询条件。 @author Punjab */
@Getter @Setter public class OutboxPageQueryBO extends QueryPage {private Long legalEntityId;private Long eventId;private String status;private String destination;}
