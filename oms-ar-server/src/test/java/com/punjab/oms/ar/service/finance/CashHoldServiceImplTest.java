package com.punjab.oms.ar.service.finance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.punjab.oms.ar.base.ServiceException;
import com.punjab.oms.ar.common.utils.FinanceValues;
import com.punjab.oms.ar.dao.finance.CashHoldDao;
import com.punjab.oms.ar.domain.dto.finance.CashHoldRequest.*;
import com.punjab.oms.ar.domain.entity.finance.*;
import com.punjab.oms.ar.domain.vo.finance.JournalTicket;
import com.punjab.oms.ar.service.finance.impl.CashHoldServiceImpl;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CashHoldServiceImplTest {
 private final CashHoldDao dao=mock(CashHoldDao.class);
 private final FinanceJournalService journal=mock(FinanceJournalService.class);
 private final JournalTicket ticket=new JournalTicket(17L,null,"actor",1L);
 private final CashHoldServiceImpl service=new CashHoldServiceImpl(dao,journal,new FinanceValues(new ObjectMapper().findAndRegisterModules()),mock(Validator.class));
 private final ArCashAccount account=new ArCashAccount();
 private final List<ArCashHoldSlice> slices=new ArrayList<>();
 private static BigDecimal n(String x){return new BigDecimal(x);}
 @BeforeEach void setup(){
  account.setId(4L);account.setBookAmount(n("70"));account.setFrozenAmount(n("5"));account.setAvailableAmount(n("65"));account.setStatus("ACTIVE");account.setCurrency("CNY");account.setVersion(1L);
  when(dao.account(4L,1L,true)).thenReturn(account);
  when(journal.begin(any())).thenReturn(ticket);
  doAnswer(call->{ArCashHold h=call.getArgument(0);h.setId(8L);when(dao.hold(8L,4L,true)).thenReturn(h);return null;}).when(dao).insert(any(ArCashHold.class));
  doAnswer(call->{ArCashHoldSlice s=call.getArgument(0);s.setId((long)slices.size()+1);slices.add(s);return null;}).when(dao).insert(any(ArCashHoldSlice.class));
  when(dao.slices(8L,true)).thenReturn(slices);
 }
 private ArCashReceiptLot lot(long id,String gross,String allocated,String frozen,String withdrawn){
  var lot=new ArCashReceiptLot();lot.setId(id);lot.setCashAccountId(4L);lot.setReceiptId(100+id);lot.setFrozenAmount(n(frozen));lot.setWithdrawnAmount(n(withdrawn));
  var receipt=new ArReceipt();receipt.setAmount(n(gross));receipt.setAllocatedAmount(n(allocated));
  when(dao.receipt(100+id)).thenReturn(receipt);when(dao.lot(id,4L)).thenReturn(lot);
  return lot;
 }
 @Test void freezeAndPartialReleaseUseOriginalLotsAndPreserveOtherHolds(){
  var first=lot(10,"50","20","5","0");var second=lot(20,"50","0","0","10");
  when(dao.lots(4L)).thenReturn(List.of(first,second));
  assertThat(service.freeze(new Freeze("freeze",1L,4L,n("40"),"manual"))).isEqualTo(8L);
  assertThat(slices).extracting(ArCashHoldSlice::getAmount).containsExactly(n("25"),n("15"));
  account.setStatus("STOPPED");
  service.unfreeze(new Release("release",1L,4L,8L,n("30"),"partial"));
  assertThat(first.getFrozenAmount()).isEqualByComparingTo("5");
  assertThat(second.getFrozenAmount()).isEqualByComparingTo("10");
  assertThat(account.getBookAmount()).isEqualByComparingTo("70");
  assertThat(account.getAvailableAmount()).isEqualByComparingTo("55");
  assertThatThrownBy(()->service.unfreeze(new Release("over",1L,4L,8L,n("11"),"excess"))).isInstanceOf(ServiceException.class);
 }
 @Test void withdrawalCaptureCompletesOnStoppedAccountAndCannotConsumeManualHolds(){
  var first=lot(10,"70","0","5","0");when(dao.lots(4L)).thenReturn(List.of(first));
  service.reserveWithdrawal(1L,4L,n("40"),ticket,"pay");
  assertThatThrownBy(()->service.unfreeze(new Release("manual",1L,4L,8L,n("1"),"wrong purpose"))).isInstanceOf(ServiceException.class);
  account.setStatus("STOPPED");
  service.captureWithdrawal(1L,4L,8L,ticket,"paid");
  assertThat(account.getBookAmount()).isEqualByComparingTo("30");
  assertThat(account.getAvailableAmount()).isEqualByComparingTo("25");
  assertThat(first.getFrozenAmount()).isEqualByComparingTo("5");
  assertThat(first.getWithdrawnAmount()).isEqualByComparingTo("40");
  assertThatThrownBy(()->service.captureWithdrawal(1L,4L,8L,ticket,"again")).isInstanceOf(ServiceException.class);
 }
}
