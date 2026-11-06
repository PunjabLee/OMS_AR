package com.punjab.oms.ar.domain.vo.finance;
import com.fasterxml.jackson.databind.JsonNode;
/** 完整事件可能含同主体多个客户，仅通知运营权限可读。 @author Punjab */
public record OutboxDetailResponse(
    /** 通知记录摘要 */
    OutboxResponse notification,
    /** 序列化的请求或事件内容 */
    JsonNode payload){}
