package com.punjab.oms.ar.dao.finance;
import com.punjab.oms.ar.domain.entity.finance.*;
import java.util.List;
public interface CashHoldDao {
 ArCashAccount account(Long id,Long scope,boolean lock);
 List<ArCashReceiptLot> lots(Long account);
 ArCashReceiptLot lot(Long id,Long account);
 ArReceipt receipt(Long id);
 ArCashHold hold(Long id,Long account,boolean lock);
 List<ArCashHoldSlice> slices(Long id,boolean lock);
 void insert(ArCashHold hold);
 void insert(ArCashHoldSlice slice);
 void update(ArCashHold hold);
 void update(ArCashHoldSlice slice);
 void update(ArCashReceiptLot lot);
 void update(ArCashAccount account);
}
