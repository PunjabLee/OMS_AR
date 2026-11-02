package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.domain.dto.finance.*;
import com.punjab.oms.ar.domain.vo.finance.JournalTicket;
/** 必须加入调用者已有事务，禁止独立提交流水。 @author Punjab */
public interface FinanceJournalService {JournalTicket begin(JournalBeginRequest request);void complete(JournalTicket ticket,Long result);Long append(JournalEventRequest request,JournalTicket ticket);}
