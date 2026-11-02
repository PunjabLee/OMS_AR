package com.punjab.oms.ar.domain.query.finance;
import com.punjab.oms.ar.base.QueryPage;import jakarta.validation.constraints.*;import lombok.Getter;import lombok.Setter;
/** 单据生命周期分页范围。 @author Punjab */
@Getter @Setter public class LifecycleQuery extends QueryPage {@NotNull @Positive private Long legalEntityId;@NotNull @Positive private Long documentId;}
