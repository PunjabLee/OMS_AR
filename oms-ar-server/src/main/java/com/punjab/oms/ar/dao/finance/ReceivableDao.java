package com.punjab.oms.ar.dao.finance;
import com.punjab.oms.ar.domain.entity.finance.*;
/** 应收聚合数据访问，不承载业务事务。 @author Punjab */
public interface ReceivableDao {
 boolean initializeSourceGuard(ArSourceGuard value);
 ArSourceGuard lockSourceGuard(Long legalEntityId,String sourceKey);
 ArOperation reserve(ArOperation operation);
 boolean complete(ArOperation operation);
 ArGroup group(Long id);
 ArLine line(Long id, boolean lock);
 ArAdjustmentLink adjustmentLink(Long lineId);
 boolean createAdjustmentLink(ArAdjustmentLink value);
 boolean createDocument(ArDocument value);
 boolean createGroup(ArGroup value);
 boolean createLine(ArLine value);
 boolean createExchangeBasis(ArExchangeBasis value);
 java.util.List<ArExchangeBasis> exchangeBases(java.util.List<Long> lineIds);
 boolean createSource(ArSource value);
 boolean createEvent(ArEvent value);
 boolean createChange(ArChange value);

 ArDocument document(com.punjab.oms.ar.domain.bo.finance.ReceivableDetailQueryBO query);
 java.util.List<ArLine> lines(Long documentId);
 java.util.List<ArSource> sources(java.util.List<Long> lineIds);
 com.punjab.oms.ar.base.PageResult<ArDocument> page(com.punjab.oms.ar.domain.bo.finance.ReceivablePageQueryBO query);
}
