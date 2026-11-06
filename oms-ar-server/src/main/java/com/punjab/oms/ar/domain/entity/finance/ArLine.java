package com.punjab.oms.ar.domain.entity.finance;
import java.time.*;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
/** line持久化记录。 @author Punjab */
@TableName("oms_ar_line")
public class ArLine extends ArBaseEntity {
    /** 应收单ID */
    private Long documentId;
    /** 业务组ID */
    private Long groupId;
    /** 应收所属客户ID */
    private Long customerId;
    /** 核算主体ID，也是权限范围 */
    private Long legalEntityId;
    /** 结算币种代码 */
    private String currency;
    /** 应收单内的明细行序号 */
    private Integer lineNo;
    /** 金额性质，区分价款和销项税 */
    private String amountType;
    /** 带符号的原始应收金额 */
    private BigDecimal receivableAmount;
    /** 已核销金额，沿用原应收方向 */
    private BigDecimal settledAmount;
    /** 从负数应收转入信用账户的带符号金额 */
    private BigDecimal transferredAmount;
    /** 当前未核销、未转出的应收余额 */
    private BigDecimal openAmount;
    /** 本行已登记的带符号开票金额 */
    private BigDecimal invoicedAmount;
    /** 税率，以小数表示，空值与零税率区分 */
    private BigDecimal taxRate;
    /** 上游确认的计税依据金额 */
    private BigDecimal taxBaseAmount;
    /** 计税方式 */
    private String taxMethod;
    /** 当前业务记录的状态 */
    private String status;
    /** 用于并发校验的记录版本 */
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
