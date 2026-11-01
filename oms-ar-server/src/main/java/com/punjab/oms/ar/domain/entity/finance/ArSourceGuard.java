package com.punjab.oms.ar.domain.entity.finance;
import com.baomidou.mybatisplus.annotation.TableName;import lombok.Getter;import lombok.Setter;
/** 稳定来源身份锁，与事件编号无关，取消后保留禁止重建标记。 @author Punjab */
@Getter @Setter @TableName("oms_ar_source_guard")
public class ArSourceGuard extends ArBaseEntity {
 private Long legalEntityId;private String sourceKey;private String sourceSystem;private String sourceType;private String sourceId;private String sourceLineId;private Boolean blocked;private Long version;
}
