package com.punjab.oms.ar.dao.finance;

import com.punjab.oms.ar.domain.entity.finance.ArCashAccount;
import com.punjab.oms.ar.domain.entity.finance.ArCashReceiptLot;

/** 现金账户持久化。 @author Punjab */
public interface CashAccountDao {
    ArCashAccount byMode(Long customerId, Long legalEntityId, String currency, String invoiceMode);
    Long boundAccount(Long groupId);
    boolean bind(com.punjab.oms.ar.domain.entity.finance.ArCashGroupBinding binding);
    ArCashAccount account(Long id, Long scope, boolean lock);
    ArCashReceiptLot receiptLot(Long receipt);
    java.util.List<ArCashReceiptLot> fifo(Long cashAccountId);
    String batchSnapshot(Long eventId, Long operationId);
    boolean insertLot(ArCashReceiptLot lot);
    boolean updateBook(ArCashAccount account, Long expectedVersion);
    boolean hasOpenDependencies(Long id);
    boolean updateStatus(ArCashAccount row, Long previousVersion);
    boolean create(ArCashAccount row);
    ArCashAccount find(Long id, Long legalEntityId);
}
