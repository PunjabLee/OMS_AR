package com.punjab.oms.ar.domain.entity.finance;
import java.time.*;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
/** operation_request持久化记录。 @author Punjab */
@TableName("oms_ar_operation_request")
public class ArOperation extends ArBaseEntity {
    private Long legalEntityId;
    private String callerId;
    private String operationType;
    private String requestKey;
    private String payloadHash;
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
