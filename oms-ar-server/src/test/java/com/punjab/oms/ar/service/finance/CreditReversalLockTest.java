package com.punjab.oms.ar.service.finance;

import com.punjab.oms.ar.base.ServiceException;
import com.punjab.oms.ar.common.utils.FinanceValues;
import com.punjab.oms.ar.dao.finance.*;
import com.punjab.oms.ar.domain.dto.finance.*;
import com.punjab.oms.ar.domain.entity.finance.*;
import com.punjab.oms.ar.domain.vo.finance.JournalTicket;
import com.punjab.oms.ar.service.finance.impl.*;
import jakarta.validation.Validator;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 现金主锁受阻时，两条信用撤销路径均不得占用信用账户或批次。 @author Punjab */
class CreditReversalLockTest {
    final CashGroupOwnership ownership = mock(CashGroupOwnership.class);
    final CustomerBalanceDao credit = mock(CustomerBalanceDao.class);
    final FinanceJournalService journal = mock(FinanceJournalService.class);
    final FinanceValues values = mock(FinanceValues.class);
    final Validator validator = mock(Validator.class);

    @BeforeEach void setup() {
        var account = new ArCustomerAccount();
        account.setId(11L);
        account.setCashAccountId(12L);
        when(credit.account(11L,1L,false)).thenReturn(account);
        when(journal.begin(any())).thenReturn(new JournalTicket(8L,null,"finance",1L));
        when(ownership.lockOpen(12L,1L)).thenThrow(new ServiceException("现金主锁受阻"));
    }
    @Test @DisplayName("转入撤销等待现金主锁时不持有信用锁") void transferReversal() {
        var lot = new ArBalanceLot();
        lot.setId(9L); lot.setAccountId(11L); lot.setStatus("ACTIVE");
        when(credit.lot(9L,1L,false)).thenReturn(lot);
        var service = new CustomerBalanceServiceImpl(null,ownership,credit,null,journal,values,validator);
        assertThatThrownBy(()->service.reverseTransfer(new BalanceTransferReverseRequest("k",1L,9L,"undo")))
                .hasMessage("现金主锁受阻");
        verify(credit,never()).account(11L,1L,true);
        verify(credit,never()).lot(9L,1L,true);
    }
    @Test @DisplayName("核销撤销等待现金主锁时不持有信用锁") void settlementReversal() {
        var settlements=mock(SettlementDao.class);
        var sources=mock(BalanceSettlementDao.class);
        var settlement=new ArSettlement();
        settlement.setId(10L); settlement.setSettlementType("BALANCE"); settlement.setStatus("POSTED");
        when(settlements.settlement(10L,1L,true)).thenReturn(settlement);
        var source=new ArBalanceSettlement(); source.setAccountId(11L);
        when(sources.source(10L)).thenReturn(source);
        var service=new BalanceSettlementServiceImpl(ownership,sources,credit,settlements,null,journal,values,validator);
        assertThatThrownBy(()->service.reverse(new SettlementReverseRequest("k",1L,10L,"undo")))
                .hasMessage("现金主锁受阻");
        verify(credit,never()).account(11L,1L,true);
    }
}
