package com.punjab.oms.ar.domain.entity.finance;
import java.time.*;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
/** document持久化记录。 @author Punjab */
@TableName("oms_ar_document")
public class ArDocument extends ArBaseEntity {
    private String docNo;
    private Long customerId;
    private Long legalEntityId;
    private String currency;
    private String businessType;
    private String generationMode;
    private LocalDate businessDate;
    private LocalDate dueDate;
    private String status;
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
