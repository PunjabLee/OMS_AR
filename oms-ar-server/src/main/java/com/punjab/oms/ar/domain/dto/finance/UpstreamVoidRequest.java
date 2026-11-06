package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.Valid;import jakarta.validation.constraints.*;import java.util.List;
/** 上游显式选择来源槽，NULL来源行不隐式匹配其他行。 @author Punjab */
public record UpstreamVoidRequest(
    /** 调用方提供的幂等请求键 */
    @NotBlank @Size(max=128) String requestKey,
    /** 核算主体ID，也是权限范围 */
    @NotNull @Positive Long legalEntityId,
    /** 来源系统标识 */
    @NotBlank @Size(max=64) String sourceSystem,
    /** 待处理的来源槽集合 */
    @NotEmpty @Size(max=200) List<@NotNull @Valid Source> sources,
    /** 业务操作原因 */
    @NotBlank @Size(max=500) String reason){
 /** 来源单据和可选行。 @author Punjab */
 public record Source(
    /** 来源单据类型 */
    @NotBlank @Size(max=64) String sourceType,
    /** 来源单据ID */
    @NotBlank @Size(max=128) String sourceId,
    /** 可选的来源行标识 */
    @Size(max=128) String sourceLineId){}
}
