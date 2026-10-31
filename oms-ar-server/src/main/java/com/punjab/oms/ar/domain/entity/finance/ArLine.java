package com.punjab.oms.ar.domain.entity.finance;
import java.time.*;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
/** line持久化记录。 @author Punjab */
@TableName("oms_ar_line")
public class ArLine extends ArBaseEntity {
    private Long documentId;
    private Long groupId;
    private Long customerId;
    private Long legalEntityId;
    private String currency;
    private Integer lineNo;
    private String amountType;
    private BigDecimal receivableAmount;
    private BigDecimal settledAmount;
    private BigDecimal transferredAmount;
    private BigDecimal openAmount;
    private BigDecimal invoicedAmount;
    private BigDecimal taxRate;
    private BigDecimal taxBaseAmount;
    private String taxMethod;
    private String status;
    private Long version;
    public Long getDocumentId(){return documentId;}
    public void setDocumentId(Long value){this.documentId=value;}
    public Long getGroupId(){return groupId;}
    public void setGroupId(Long value){this.groupId=value;}
    public Long getCustomerId(){return customerId;}
    public void setCustomerId(Long value){this.customerId=value;}
    public Long getLegalEntityId(){return legalEntityId;}
    public void setLegalEntityId(Long value){this.legalEntityId=value;}
    public String getCurrency(){return currency;}
    public void setCurrency(String value){this.currency=value;}
    public Integer getLineNo(){return lineNo;}
    public void setLineNo(Integer value){this.lineNo=value;}
    public String getAmountType(){return amountType;}
    public void setAmountType(String value){this.amountType=value;}
    public BigDecimal getReceivableAmount(){return receivableAmount;}
    public void setReceivableAmount(BigDecimal value){this.receivableAmount=value;}
    public BigDecimal getSettledAmount(){return settledAmount;}
    public void setSettledAmount(BigDecimal value){this.settledAmount=value;}
    public BigDecimal getTransferredAmount(){return transferredAmount;}
    public void setTransferredAmount(BigDecimal value){this.transferredAmount=value;}
    public BigDecimal getOpenAmount(){return openAmount;}
    public void setOpenAmount(BigDecimal value){this.openAmount=value;}
    public BigDecimal getInvoicedAmount(){return invoicedAmount;}
    public void setInvoicedAmount(BigDecimal value){this.invoicedAmount=value;}
    public BigDecimal getTaxRate(){return taxRate;}
    public void setTaxRate(BigDecimal value){this.taxRate=value;}
    public BigDecimal getTaxBaseAmount(){return taxBaseAmount;}
    public void setTaxBaseAmount(BigDecimal value){this.taxBaseAmount=value;}
    public String getTaxMethod(){return taxMethod;}
    public void setTaxMethod(String value){this.taxMethod=value;}
    public String getStatus(){return status;}
    public void setStatus(String value){this.status=value;}
    public Long getVersion(){return version;}
    public void setVersion(Long value){this.version=value;}
}
