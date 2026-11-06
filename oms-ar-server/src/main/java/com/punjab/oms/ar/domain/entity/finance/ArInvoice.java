package com.punjab.oms.ar.domain.entity.finance;
import com.baomidou.mybatisplus.annotation.TableName;import java.math.BigDecimal;import java.time.LocalDate;
/** 发票关联持久化记录。 @author Punjab */
@TableName("oms_ar_invoice")
public class ArInvoice extends ArBaseEntity {
 /** 来源系统标识 */
 private String sourceSystem;
 public String getSourceSystem(){return sourceSystem;}public void setSourceSystem(String v){sourceSystem=v;}
 /** 来源单据ID */
 private String sourceId;
 public String getSourceId(){return sourceId;}public void setSourceId(String v){sourceId=v;}
 /** 外部发票编号 */
 private String invoiceNo;
 public String getInvoiceNo(){return invoiceNo;}public void setInvoiceNo(String v){invoiceNo=v;}
 /** 结算币种代码 */
 private String currency;
 public String getCurrency(){return currency;}public void setCurrency(String v){currency=v;}
 /** 发票方向，蓝字或红字 */
 private String invoiceKind;
 public String getInvoiceKind(){return invoiceKind;}public void setInvoiceKind(String v){invoiceKind=v;}
 /** 当前业务记录的状态 */
 private String status;
 public String getStatus(){return status;}public void setStatus(String v){status=v;}
 /** 外部发票日期 */
 private LocalDate invoiceDate;
 public LocalDate getInvoiceDate(){return invoiceDate;}public void setInvoiceDate(LocalDate v){invoiceDate=v;}
 /** 应收所属客户ID */
 private Long customerId;
 public Long getCustomerId(){return customerId;}public void setCustomerId(Long v){customerId=v;}
 /** 核算主体ID，也是权限范围 */
 private Long legalEntityId;
 public Long getLegalEntityId(){return legalEntityId;}public void setLegalEntityId(Long v){legalEntityId=v;}
 /** 红字引用的原蓝字关联ID */
 private Long originalInvoiceId;
 public Long getOriginalInvoiceId(){return originalInvoiceId;}public void setOriginalInvoiceId(Long v){originalInvoiceId=v;}
 /** 用于并发校验的记录版本 */
 private Long version;
 public Long getVersion(){return version;}public void setVersion(Long v){version=v;}
 /** 带符号的发票分配合计 */
 private BigDecimal totalAmount;
 public BigDecimal getTotalAmount(){return totalAmount;}public void setTotalAmount(BigDecimal v){totalAmount=v;}
}
