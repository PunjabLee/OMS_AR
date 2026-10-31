package com.punjab.oms.ar.domain.entity.finance;
import java.time.*;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
/** business_group持久化记录。 @author Punjab */
@TableName("oms_ar_business_group")
public class ArGroup extends ArBaseEntity {
    private String groupNo;
    private Long customerId;
    private Long legalEntityId;
    private String currency;
    private LocalDate businessDate;
    private Long version;
    public String getGroupNo(){return groupNo;}
    public void setGroupNo(String value){this.groupNo=value;}
    public Long getCustomerId(){return customerId;}
    public void setCustomerId(Long value){this.customerId=value;}
    public Long getLegalEntityId(){return legalEntityId;}
    public void setLegalEntityId(Long value){this.legalEntityId=value;}
    public String getCurrency(){return currency;}
    public void setCurrency(String value){this.currency=value;}
    public LocalDate getBusinessDate(){return businessDate;}
    public void setBusinessDate(LocalDate value){this.businessDate=value;}
    public Long getVersion(){return version;}
    public void setVersion(Long value){this.version=value;}
}
