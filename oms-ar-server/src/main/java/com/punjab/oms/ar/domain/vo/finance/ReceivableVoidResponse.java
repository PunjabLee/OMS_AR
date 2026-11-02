package com.punjab.oms.ar.domain.vo.finance;
import java.util.List;
/** 事件标识及本次实际作废行和状态发生变化的单据。 @author Punjab */
public record ReceivableVoidResponse(Long eventId,List<Long> documentIds,List<Long> lineIds){public ReceivableVoidResponse{documentIds=List.copyOf(documentIds);lineIds=List.copyOf(lineIds);}}
