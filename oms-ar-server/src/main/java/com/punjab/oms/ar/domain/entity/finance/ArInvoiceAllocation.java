package com.punjab.oms.ar.domain.entity.finance;
import com.baomidou.mybatisplus.annotation.TableName;import java.math.BigDecimal;import java.time.LocalDate;
/** 发票关联持久化记录。 @author Punjab */
@TableName("oms_ar_invoice_allocation")
public class ArInvoiceAllocation extends ArBaseEntity {
 /** 发票关联记录ID */
 private Long invoiceId;
 public Long getInvoiceId(){return invoiceId;}public void setInvoiceId(Long v){invoiceId=v;}
 /** 分配所关联的应收行ID */
 private Long receivableLineId;
 public Long getReceivableLineId(){return receivableLineId;}public void setReceivableLineId(Long v){receivableLineId=v;}
 /** 红字引用的原蓝字关联ID */
 private Long originalInvoiceId;
 public Long getOriginalInvoiceId(){return originalInvoiceId;}public void setOriginalInvoiceId(Long v){originalInvoiceId=v;}
 /** 红字分配引用的原蓝字分配ID */
 private Long originalAllocationId;
 public Long getOriginalAllocationId(){return originalAllocationId;}public void setOriginalAllocationId(Long v){originalAllocationId=v;}
 /** 发票方向，蓝字或红字 */
 private String invoiceKind;
 public String getInvoiceKind(){return invoiceKind;}public void setInvoiceKind(String v){invoiceKind=v;}
 /** 外部发票行标识 */
 private String invoiceLineId;
 public String getInvoiceLineId(){return invoiceLineId;}public void setInvoiceLineId(String v){invoiceLineId=v;}
 /** 金额性质，区分价款和销项税 */
 private String amountType;
 public String getAmountType(){return amountType;}public void setAmountType(String v){amountType=v;}
 /** 本记录的业务金额，方向由业务类型决定 */
 private BigDecimal amount;
 public BigDecimal getAmount(){return amount;}public void setAmount(BigDecimal v){amount=v;}
 /** 税率，以小数表示，空值与零税率区分 */
 private BigDecimal taxRate;
 public BigDecimal getTaxRate(){return taxRate;}public void setTaxRate(BigDecimal v){taxRate=v;}
}
