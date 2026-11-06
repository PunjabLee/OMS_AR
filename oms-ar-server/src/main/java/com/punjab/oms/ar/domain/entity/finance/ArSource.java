package com.punjab.oms.ar.domain.entity.finance;
import java.time.*;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
/** line_source持久化记录。 @author Punjab */
@TableName("oms_ar_line_source")
public class ArSource extends ArBaseEntity {
    /** 业务明细行标识 */
    private Long lineId;
    /** 来源系统标识 */
    private String sourceSystem;
    /** 来源单据类型 */
    private String sourceType;
    /** 来源单据编号 */
    private String sourceNo;
    /** 来源单据ID */
    private String sourceId;
    /** 可选的来源行标识 */
    private String sourceLineId;
    /** 来源系统的事件标识 */
    private String sourceEventId;
    /** 同一来源事件内的输出标识 */
    private String sourceOutputKey;
    /** 调整所指定的原正数应收行ID */
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
