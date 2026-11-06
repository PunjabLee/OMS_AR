package com.punjab.oms.ar.domain.bo.finance;
import jakarta.validation.constraints.*;
/** 详情范围。 @author Punjab */
public record ReceivableDetailQueryBO(
    /** 当前对象ID */
    Long id,
    /** 核算主体ID，也是权限范围 */
    Long legalEntityId){}
