package com.punjab.oms.ar.domain.entity.finance;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
/** 显式审计基础字段。 @author Punjab */
public abstract class ArBaseEntity {
    @TableId(type=IdType.AUTO)
    private Long id;
    private LocalDateTime createdTime;
    private String createdBy;
    private LocalDateTime updatedTime;
    private String updatedBy;
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
