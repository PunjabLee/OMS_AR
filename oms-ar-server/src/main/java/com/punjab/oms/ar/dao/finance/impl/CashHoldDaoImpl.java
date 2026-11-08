package com.punjab.oms.ar.dao.finance.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.punjab.oms.ar.base.ServiceException;
import com.punjab.oms.ar.dao.finance.CashHoldDao;
import com.punjab.oms.ar.domain.entity.finance.*;
import com.punjab.oms.ar.mapper.finance.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository @RequiredArgsConstructor
public class CashHoldDaoImpl implements CashHoldDao {
 private final ArCashAccountMapper accounts;
 private final ArCashReceiptLotMapper lots;
 private final ArReceiptMapper receipts;
 private final ArCashHoldMapper holds;
 private final ArCashHoldSliceMapper slices;
 public ArCashAccount account(Long id,Long scope,boolean lock){
  var q=new LambdaQueryWrapper<ArCashAccount>().eq(ArCashAccount::getId,id).eq(ArCashAccount::getLegalEntityId,scope).eq(ArCashAccount::getDeleteStatus,false);
  if(lock) q.last("FOR UPDATE");
  return accounts.selectOne(q);
 }
 public List<ArCashReceiptLot> lots(Long account){
  return lots.selectList(new LambdaQueryWrapper<ArCashReceiptLot>().eq(ArCashReceiptLot::getCashAccountId,account).eq(ArCashReceiptLot::getDeleteStatus,false).notExists("SELECT 1 FROM oms_ar_cash_receipt_reversal rev WHERE rev.lot_id=oms_ar_cash_receipt_lot.id").orderByAsc(ArCashReceiptLot::getId).last("FOR UPDATE"));
 }
 public ArCashReceiptLot lot(Long id,Long account){
  return lots.selectOne(new LambdaQueryWrapper<ArCashReceiptLot>().eq(ArCashReceiptLot::getId,id).eq(ArCashReceiptLot::getCashAccountId,account).eq(ArCashReceiptLot::getDeleteStatus,false).last("FOR UPDATE"));
 }
 public ArReceipt receipt(Long id){
  return receipts.selectOne(new LambdaQueryWrapper<ArReceipt>().eq(ArReceipt::getId,id).eq(ArReceipt::getDeleteStatus,false).last("FOR UPDATE"));
 }
 public ArCashHold hold(Long id,Long account,boolean lock){
  var q=new LambdaQueryWrapper<ArCashHold>().eq(ArCashHold::getId,id).eq(ArCashHold::getCashAccountId,account).eq(ArCashHold::getDeleteStatus,false);
  if(lock)q.last("FOR UPDATE");
  return holds.selectOne(q);
 }
 public List<ArCashHoldSlice> slices(Long id,boolean lock){
  var q=new LambdaQueryWrapper<ArCashHoldSlice>().eq(ArCashHoldSlice::getHoldId,id).eq(ArCashHoldSlice::getDeleteStatus,false).orderByAsc(ArCashHoldSlice::getLotId);
  if(lock)q.last("FOR UPDATE");
  return slices.selectList(q);
 }
 public void insert(ArCashHold row){check(holds.insert(row));}
 public void insert(ArCashHoldSlice row){check(slices.insert(row));}
 public void update(ArCashHold row){check(holds.updateById(row));}
 public void update(ArCashHoldSlice row){check(slices.updateById(row));}
 public void update(ArCashReceiptLot row){check(lots.updateById(row));}
 public void update(ArCashAccount row){check(accounts.update(null,new LambdaUpdateWrapper<ArCashAccount>().eq(ArCashAccount::getId,row.getId()).eq(ArCashAccount::getVersion,row.getVersion()-1).set(ArCashAccount::getBookAmount,row.getBookAmount()).set(ArCashAccount::getFrozenAmount,row.getFrozenAmount()).set(ArCashAccount::getVersion,row.getVersion()).set(ArCashAccount::getUpdatedTime,row.getUpdatedTime()).set(ArCashAccount::getUpdatedBy,row.getUpdatedBy())));}
 private void check(int rows){if(rows!=1)throw new ServiceException("现金冻结记录已变化");}
}
