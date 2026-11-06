package com.punjab.oms.ar.domain.entity.finance;
import java.time.*;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
/** document持久化记录。 @author Punjab */
@TableName("oms_ar_document")
public class ArDocument extends ArBaseEntity {
    /** 财务或上游作废原因 */
    private String voidReason;
    /** 执行整单作废的操作人 */
    private String voidedBy;
    /** 整单作废完成时间 */
    private LocalDateTime voidedTime;
    public String getVoidReason(){return voidReason;}
    public void setVoidReason(String v){voidReason=v;}
    public String getVoidedBy(){return voidedBy;}
    public void setVoidedBy(String v){voidedBy=v;}
    public LocalDateTime getVoidedTime(){return voidedTime;}
    public void setVoidedTime(LocalDateTime v){voidedTime=v;}
    /** 应收单编号 */
    private String docNo;
    /** 应收所属客户ID */
    private Long customerId;
    /** 核算主体ID，也是权限范围 */
    private Long legalEntityId;
    /** 结算币种代码 */
    private String currency;
    /** 来源业务类别，例如出库或汇率差异 */
    private String businessType;
    /** 生成方式，区分直接录入与上游生成 */
    private String generationMode;
    /** 业务发生日期 */
    private LocalDate businessDate;
    /** 应收到期日期 */
    private LocalDate dueDate;
    /** 当前业务记录的状态 */
    private String status;
    /** 用于并发校验的记录版本 */
    private Long version;
    public String getDocNo(){return docNo;}
    public void setDocNo(String value){this.docNo=value;}
    public Long getCustomerId(){return customerId;}
    public void setCustomerId(Long value){this.customerId=value;}
    public Long getLegalEntityId(){return legalEntityId;}
    public void setLegalEntityId(Long value){this.legalEntityId=value;}
    public String getCurrency(){return currency;}
    public void setCurrency(String value){this.currency=value;}
    public String getBusinessType(){return businessType;}
    public void setBusinessType(String value){this.businessType=value;}
    public String getGenerationMode(){return generationMode;}
    public void setGenerationMode(String value){this.generationMode=value;}
    public LocalDate getBusinessDate(){return businessDate;}
    public void setBusinessDate(LocalDate value){this.businessDate=value;}
    public LocalDate getDueDate(){return dueDate;}
    public void setDueDate(LocalDate value){this.dueDate=value;}
    public String getStatus(){return status;}
    public void setStatus(String value){this.status=value;}
    public Long getVersion(){return version;}
    public void setVersion(Long value){this.version=value;}
}
