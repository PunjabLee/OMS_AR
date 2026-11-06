package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.Valid;import jakarta.validation.constraints.*;import java.time.LocalDate;
/** 登记外部发票关联，不直接调用税务开票平台。 @author Punjab */
public record InvoiceRegisterRequest(
    /** 调用方提供的幂等请求键 */
    @NotBlank @Size(max=128) String requestKey,
    /** 来源系统标识 */
    @NotBlank @Size(max=64) String sourceSystem,
 /** 来源单据ID */
 @NotBlank @Size(max=128) String sourceId,
    /** 外部发票编号 */
    @NotBlank @Size(max=128) String invoiceNo,
    /** 外部发票日期 */
    @NotNull LocalDate invoiceDate,
 /** 用户确认的原预览请求 */
 @NotNull @Valid InvoicePreviewRequest preview,
    /** 确认时须匹配的预览指纹 */
    @NotBlank @Size(min=64,max=64) String expectedFingerprint,
    /** 业务操作原因 */
    @NotBlank @Size(max=500) String reason){}
