package com.punjab.oms.ar.domain.entity.finance;
import java.time.*;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
/** lifecycle_change持久化记录。 @author Punjab */
@TableName("oms_ar_lifecycle_change")
public class ArChange extends ArBaseEntity {
    /** 事件标识 */
    private Long eventId;
    /** 被记录或诊断的对象类型 */
    private String objectType;
    /** 被记录或诊断的对象ID */
    private Long objectId;
    /** 变更前的对象快照，初始化时为空 */
    private String beforeSnapshot;
    /** 本次变更的字段及增量 */
    private String deltaSnapshot;
    /** 变更后的对象快照 */
    private String afterSnapshot;
    /** 变更前的版本号 */
    private Long beforeVersion;
    /** 变更后的版本号 */
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
