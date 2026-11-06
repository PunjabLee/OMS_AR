package com.punjab.oms.ar.domain.entity.finance;
import com.baomidou.mybatisplus.annotation.TableName;
/** 负数应收的不可变业务关联，独立于来源单据是否存在。 @author Punjab */
@TableName("oms_ar_adjustment_link")
public class ArAdjustmentLink extends ArBaseEntity {
 /** lineId：业务明细行标识；relatedGroupId：负数调整归属的原业务组ID；originalLineId：调整所指定的原正数应收行ID */
 private Long lineId,relatedGroupId,originalLineId;
 public Long getLineId(){return lineId;} public void setLineId(Long v){lineId=v;}
 public Long getRelatedGroupId(){return relatedGroupId;} public void setRelatedGroupId(Long v){relatedGroupId=v;}
 public Long getOriginalLineId(){return originalLineId;} public void setOriginalLineId(Long v){originalLineId=v;}
}
