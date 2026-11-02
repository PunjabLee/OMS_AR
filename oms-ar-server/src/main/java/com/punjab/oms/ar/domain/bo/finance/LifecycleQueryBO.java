package com.punjab.oms.ar.domain.bo.finance;
import com.punjab.oms.ar.base.QueryPage;import lombok.Getter;import lombok.Setter;
/** 生命周期分页持久层范围。 @author Punjab */
@Getter @Setter public class LifecycleQueryBO extends QueryPage {private Long legalEntityId;private Long documentId;public long getOffset(){return ((long)getPage()-1)*getSize();}}
