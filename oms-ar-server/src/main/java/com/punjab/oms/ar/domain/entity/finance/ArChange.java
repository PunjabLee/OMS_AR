package com.punjab.oms.ar.domain.entity.finance;
import java.time.*;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
/** lifecycle_change持久化记录。 @author Punjab */
@TableName("oms_ar_lifecycle_change")
public class ArChange extends ArBaseEntity {
    private Long eventId;
    private String objectType;
    private Long objectId;
    private String beforeSnapshot;
    private String deltaSnapshot;
    private String afterSnapshot;
    private Long beforeVersion;
    private Long afterVersion;
    public Long getEventId(){return eventId;}
    public void setEventId(Long value){this.eventId=value;}
    public String getObjectType(){return objectType;}
    public void setObjectType(String value){this.objectType=value;}
    public Long getObjectId(){return objectId;}
    public void setObjectId(Long value){this.objectId=value;}
    public String getBeforeSnapshot(){return beforeSnapshot;}
    public void setBeforeSnapshot(String value){this.beforeSnapshot=value;}
    public String getDeltaSnapshot(){return deltaSnapshot;}
    public void setDeltaSnapshot(String value){this.deltaSnapshot=value;}
    public String getAfterSnapshot(){return afterSnapshot;}
    public void setAfterSnapshot(String value){this.afterSnapshot=value;}
    public Long getBeforeVersion(){return beforeVersion;}
    public void setBeforeVersion(Long value){this.beforeVersion=value;}
    public Long getAfterVersion(){return afterVersion;}
    public void setAfterVersion(Long value){this.afterVersion=value;}
}
