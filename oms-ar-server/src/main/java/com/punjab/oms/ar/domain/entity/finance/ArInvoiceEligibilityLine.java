package com.punjab.oms.ar.domain.entity.finance;
/** 开票额度查询的只读联表投影，不用于写表。 @author Punjab */
public class ArInvoiceEligibilityLine extends ArLine {
 private Long invoiceGroupId,exchangeBasisId,adjustmentOriginalLineId;private String businessType,originalAmountType;private java.math.BigDecimal originalTaxRate;
 public Long getAdjustmentOriginalLineId(){return adjustmentOriginalLineId;}public void setAdjustmentOriginalLineId(Long v){adjustmentOriginalLineId=v;}
 public String getOriginalAmountType(){return originalAmountType;}public void setOriginalAmountType(String v){originalAmountType=v;}
 public java.math.BigDecimal getOriginalTaxRate(){return originalTaxRate;}public void setOriginalTaxRate(java.math.BigDecimal v){originalTaxRate=v;}
 public Long getInvoiceGroupId(){return invoiceGroupId;}public void setInvoiceGroupId(Long value){invoiceGroupId=value;}
 public Long getExchangeBasisId(){return exchangeBasisId;}public void setExchangeBasisId(Long value){exchangeBasisId=value;}
 public String getBusinessType(){return businessType;}public void setBusinessType(String value){businessType=value;}
}
