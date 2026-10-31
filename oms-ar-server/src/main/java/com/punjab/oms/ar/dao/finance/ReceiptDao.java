package com.punjab.oms.ar.dao.finance;
import com.punjab.oms.ar.domain.entity.finance.ArReceipt;
import com.punjab.oms.ar.domain.bo.finance.ReceiptDetailQueryBO;
/** 收款持久化。 @author Punjab */
public interface ReceiptDao {boolean create(ArReceipt value);ArReceipt detail(ReceiptDetailQueryBO query);}
