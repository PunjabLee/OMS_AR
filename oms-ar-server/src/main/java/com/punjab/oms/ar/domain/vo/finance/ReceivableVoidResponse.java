package com.punjab.oms.ar.domain.vo.finance;
import java.util.List;
/** 事件标识及本次实际作废行和状态发生变化的单据。 @author Punjab */
public record ReceivableVoidResponse(
    /** 事件标识 */
    Long eventId,
    /** 本次操作涉及的应收单ID集合 */
    List<Long> documentIds,
    /** 相关明细行ID集合 */
    List<Long> lineIds){public ReceivableVoidResponse{documentIds=List.copyOf(documentIds);lineIds=List.copyOf(lineIds);}}
