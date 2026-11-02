package com.punjab.oms.ar.domain.entity.finance;
import lombok.Getter;import lombok.Setter;import java.math.BigDecimal;
/** SQL只读差异投影，无对应可写表。 @author Punjab */
@Getter @Setter public class ArReconciliationDifference {private String rule;private String objectType;private Long objectId;private BigDecimal storedAmount;private BigDecimal expectedAmount;}
