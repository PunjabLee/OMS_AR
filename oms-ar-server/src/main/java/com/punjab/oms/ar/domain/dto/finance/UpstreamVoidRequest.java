package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.Valid;import jakarta.validation.constraints.*;import java.util.List;
/** 上游显式选择来源槽，NULL来源行不隐式匹配其他行。 @author Punjab */
public record UpstreamVoidRequest(@NotBlank @Size(max=128) String requestKey,@NotNull @Positive Long legalEntityId,@NotBlank @Size(max=64) String sourceSystem,@NotEmpty @Size(max=200) List<@NotNull @Valid Source> sources,@NotBlank @Size(max=500) String reason){
 /** 来源单据和可选行。 @author Punjab */
 public record Source(@NotBlank @Size(max=64) String sourceType,@NotBlank @Size(max=128) String sourceId,@Size(max=128) String sourceLineId){}
}
