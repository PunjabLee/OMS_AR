package com.punjab.oms.ar.domain.entity.finance;
/** 开票额度查询的只读联表投影，不用于写表。 @author Punjab */
public class ArInvoiceEligibilityLine extends ArLine {
 /** invoiceGroupId：计算共享开票额度所用的业务组ID；exchangeBasisId：汇率计算依据记录ID；adjustmentOriginalLineId：调整所指定的原应收行ID；relatedGroupId：负数调整归属的原业务组ID */
 private Long invoiceGroupId,exchangeBasisId,adjustmentOriginalLineId,relatedGroupId;
 public Long getRelatedGroupId(){return relatedGroupId;}public void setRelatedGroupId(Long v){relatedGroupId=v;}
    /** businessType：来源业务类别，例如出库或汇率差异；originalAmountType：原应收行的金额性质 */
    private String businessType,originalAmountType;
    /** 原应收行的税率 */
    private java.math.BigDecimal originalTaxRate;
 public Long getAdjustmentOriginalLineId(){return adjustmentOriginalLineId;}public void setAdjustmentOriginalLineId(Long v){adjustmentOriginalLineId=v;}
 public String getOriginalAmountType(){return originalAmountType;}public void setOriginalAmountType(String v){originalAmountType=v;}
 public java.math.BigDecimal getOriginalTaxRate(){return originalTaxRate;}public void setOriginalTaxRate(java.math.BigDecimal v){originalTaxRate=v;}
 public Long getInvoiceGroupId(){return invoiceGroupId;}public void setInvoiceGroupId(Long value){invoiceGroupId=value;}
 public Long getExchangeBasisId(){return exchangeBasisId;}public void setExchangeBasisId(Long value){exchangeBasisId=value;}
 public String getBusinessType(){return businessType;}public void setBusinessType(String value){businessType=value;}
}
