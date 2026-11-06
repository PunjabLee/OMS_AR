package com.punjab.oms.ar.domain.entity.finance;
import com.baomidou.mybatisplus.annotation.TableName;import lombok.Getter;import lombok.Setter;
/** 稳定来源身份锁，与事件编号无关，取消后保留禁止重建标记。 @author Punjab */
@Getter @Setter @TableName("oms_ar_source_guard")
public class ArSourceGuard extends ArBaseEntity {
 /** 核算主体ID，也是权限范围 */
 private Long legalEntityId;
    /** 稳定来源身份的摘要键 */
    private String sourceKey;
    /** 来源系统标识 */
    private String sourceSystem;
    /** 来源单据类型 */
    private String sourceType;
    /** 来源单据ID */
    private String sourceId;
    /** 可选的来源行标识 */
    private String sourceLineId;
    /** 来源是否已禁止再次生成应收 */
    private Boolean blocked;
    /** 用于并发校验的记录版本 */
    private Long version;
}
