package com.punjab.oms.ar.service.finance;

import com.punjab.oms.ar.base.ServiceException;
import com.punjab.oms.ar.common.utils.FinanceValues;
import com.punjab.oms.ar.dao.finance.WithdrawalDao;
import com.punjab.oms.ar.domain.dto.finance.WithdrawalRequests.*;
import com.punjab.oms.ar.domain.vo.finance.*;
import com.punjab.oms.ar.service.finance.impl.WithdrawalServiceImpl;
import jakarta.validation.Validator;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.Set;

/** 提现审核与线下付款结果的资金门禁。 @author Punjab */
class WithdrawalServiceImplTest {
    private final CashHoldService holds=mock(CashHoldService.class);
    private final FinanceJournalService journal=mock(FinanceJournalService.class);
    private final WithdrawalDao dao=mock(WithdrawalDao.class);
    private final Validator validator=mock(Validator.class);
    private final WithdrawalService service=new WithdrawalServiceImpl(dao,holds,journal,mock(FinanceValues.class),validator);
    private final JournalTicket ticket=new JournalTicket(5L,null,"applicant",1L);
    @BeforeEach void setup(){
        when(validator.validate(any())).thenReturn(Set.of());
        when(journal.begin(any())).thenReturn(ticket);
    }
    private void state(String status){
        when(dao.lock(1,2)).thenReturn(new WithdrawalDao.Row(2,3,4,6,status,"applicant",1));
        when(dao.detail(1,2)).thenReturn(new WithdrawalResponse(2L,3L,4L,"****1234","CNY",BigDecimal.TEN,status,"applicant","reviewer","applicant",null,null,1L));
    }
    @Test @DisplayName("申请人不能审核自己的提现") void independentReviewer(){
        state("PENDING");
        assertThrows(ServiceException.class,()->service.approve(new Action("k",1L,2L,"approve")));
        verifyNoInteractions(holds);
    }
    @Test @DisplayName("未知付款保留冻结直至有凭证") void unknown(){
        state("PROCESSING");
        assertEquals(2L,service.result(new Result("k",1L,2L,Outcome.UNKNOWN,null,null,"timeout")));
        verifyNoInteractions(holds);
        verify(dao).change(2,"UNKNOWN","applicant",null,null);
    }
    @Test @DisplayName("无失败凭证不能解冻") void failureEvidence(){
        state("UNKNOWN");
        assertThrows(ServiceException.class,()->service.result(new Result("k",1L,2L,Outcome.FAILED,null," ","timeout")));
        verifyNoInteractions(holds);
    }
    @Test @DisplayName("在途成功结果扣除原冻结") void paid(){
        state("UNKNOWN");
        assertEquals(2L,service.result(new Result("k",1L,2L,Outcome.PAID,"BANK-10",null,"paid")));
        verify(holds).captureWithdrawal(1L,3L,6L,ticket,"paid");
        verify(dao,never()).account(anyLong(),anyLong());
    }
}
