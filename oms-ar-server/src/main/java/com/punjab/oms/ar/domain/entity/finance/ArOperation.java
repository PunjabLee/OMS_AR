package com.punjab.oms.ar.domain.entity.finance;
import java.time.*;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
/** operation_request持久化记录。 @author Punjab */
@TableName("oms_ar_operation_request")
public class ArOperation extends ArBaseEntity {
    /** 核算主体ID，也是权限范围 */
    private Long legalEntityId;
    /** 幂等请求所属调用方 */
    private String callerId;
    /** 幂等操作的动作类型 */
    private String operationType;
    /** 调用方提供的幂等请求键 */
    private String requestKey;
    /** 用于识别同键异内容请求的摘要 */
    private String payloadHash;
    /** 操作已完成时可重放的结果引用 */
    private Long resultReference;
    public Long getLegalEntityId(){return legalEntityId;}
    public void setLegalEntityId(Long value){this.legalEntityId=value;}
    public String getCallerId(){return callerId;}
    public void setCallerId(String value){this.callerId=value;}
    public String getOperationType(){return operationType;}
    public void setOperationType(String value){this.operationType=value;}
    public String getRequestKey(){return requestKey;}
    public void setRequestKey(String value){this.requestKey=value;}
    public String getPayloadHash(){return payloadHash;}
    public void setPayloadHash(String value){this.payloadHash=value;}
    public Long getResultReference(){return resultReference;}
    public void setResultReference(Long value){this.resultReference=value;}
}
