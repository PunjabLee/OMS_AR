package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.domain.dto.finance.CashHoldRequest.*;
import com.punjab.oms.ar.domain.vo.finance.CashHoldView;
import java.math.BigDecimal;
public interface CashHoldService {
    Long freeze(Freeze request);
    Long unfreeze(Release request);
    CashHoldView detail(Long scope,Long accountId,Long holdId);
    Long reserveWithdrawal(Long scope,Long accountId,BigDecimal amount,com.punjab.oms.ar.domain.vo.finance.JournalTicket ticket,String reason);
    void releaseWithdrawal(Long scope,Long accountId,Long holdId,com.punjab.oms.ar.domain.vo.finance.JournalTicket ticket,String reason);
    void captureWithdrawal(Long scope,Long accountId,Long holdId,com.punjab.oms.ar.domain.vo.finance.JournalTicket ticket,String reason);
}
