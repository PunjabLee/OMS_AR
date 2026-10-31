package com.punjab.oms.ar.domain.entity.finance;
import java.time.*;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
/** line_source持久化记录。 @author Punjab */
@TableName("oms_ar_line_source")
public class ArSource extends ArBaseEntity {
    private Long lineId;
    private String sourceSystem;
    private String sourceType;
    private String sourceNo;
    private String sourceId;
    private String sourceLineId;
    private String sourceEventId;
    private String sourceOutputKey;
    private Long originalLineId;
    public Long getLineId(){return lineId;}
    public void setLineId(Long value){this.lineId=value;}
    public String getSourceSystem(){return sourceSystem;}
    public void setSourceSystem(String value){this.sourceSystem=value;}
    public String getSourceType(){return sourceType;}
    public void setSourceType(String value){this.sourceType=value;}
    public String getSourceNo(){return sourceNo;}
    public void setSourceNo(String value){this.sourceNo=value;}
    public String getSourceId(){return sourceId;}
    public void setSourceId(String value){this.sourceId=value;}
    public String getSourceLineId(){return sourceLineId;}
    public void setSourceLineId(String value){this.sourceLineId=value;}
    public String getSourceEventId(){return sourceEventId;}
    public void setSourceEventId(String value){this.sourceEventId=value;}
    public String getSourceOutputKey(){return sourceOutputKey;}
    public void setSourceOutputKey(String value){this.sourceOutputKey=value;}
    public Long getOriginalLineId(){return originalLineId;}
    public void setOriginalLineId(Long value){this.originalLineId=value;}
}
