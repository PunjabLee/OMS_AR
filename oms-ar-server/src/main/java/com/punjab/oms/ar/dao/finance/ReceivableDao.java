package com.punjab.oms.ar.dao.finance;
import com.punjab.oms.ar.domain.entity.finance.*;
/** 应收聚合数据访问，不承载业务事务。 @author Punjab */
public interface ReceivableDao {
 ArOperation reserve(ArOperation operation);
 boolean complete(ArOperation operation);
 ArGroup group(Long id);
 boolean createDocument(ArDocument value);
 boolean createGroup(ArGroup value);
 boolean createLine(ArLine value);
 boolean createSource(ArSource value);
 boolean createEvent(ArEvent value);
 boolean createChange(ArChange value);
}
