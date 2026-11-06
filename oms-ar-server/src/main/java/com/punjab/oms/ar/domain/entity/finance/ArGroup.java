package com.punjab.oms.ar.domain.entity.finance;
import java.time.*;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
/** business_group持久化记录。 @author Punjab */
@TableName("oms_ar_business_group")
public class ArGroup extends ArBaseEntity {
    /** 业务组编号 */
    private String groupNo;
    /** 应收所属客户ID */
    private Long customerId;
    /** 核算主体ID，也是权限范围 */
    private Long legalEntityId;
    /** 结算币种代码 */
    private String currency;
    /** 业务发生日期 */
    private LocalDate businessDate;
    /** 用于并发校验的记录版本 */
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
