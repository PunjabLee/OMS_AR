package com.punjab.oms.ar.domain.entity.finance;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
/** 显式审计基础字段。 @author Punjab */
public abstract class ArBaseEntity {
    /** 当前对象ID */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 记录创建时间 */
    private LocalDateTime createdTime;
    /** 创建记录的操作人 */
    private String createdBy;
    /** 记录最后更新时间 */
    private LocalDateTime updatedTime;
    /** 最近更新记录的操作人 */
    private String updatedBy;
    /** 记录的逻辑删除标志 */
    private Boolean deleteStatus;
    public Long getId(){return id;}
    public void setId(Long value){this.id=value;}
    public LocalDateTime getCreatedTime(){return createdTime;}
    public void setCreatedTime(LocalDateTime value){this.createdTime=value;}
    public String getCreatedBy(){return createdBy;}
    public void setCreatedBy(String value){this.createdBy=value;}
    public LocalDateTime getUpdatedTime(){return updatedTime;}
    public void setUpdatedTime(LocalDateTime value){this.updatedTime=value;}
    public String getUpdatedBy(){return updatedBy;}
    public void setUpdatedBy(String value){this.updatedBy=value;}
    public Boolean getDeleteStatus(){return deleteStatus;}
    public void setDeleteStatus(Boolean value){this.deleteStatus=value;}
}
