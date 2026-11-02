package com.punjab.oms.ar.dao.finance;
import com.punjab.oms.ar.domain.entity.finance.*;import java.util.List;
/** 作废涉及的同一finance聚合数据访问。 @author Punjab */
public interface ReceivableVoidDao {
 ArDocument document(Long id,Long scope,boolean lock);List<ArLine> lines(Long documentId);List<ArSource> sources(List<Long> lineIds);
 boolean initializeGuard(ArSourceGuard guard);ArSourceGuard lockGuard(Long scope,String key);boolean blockGuard(ArSourceGuard guard,Long version);
 List<ArLine> sourceLines(ArSourceGuard guard);ArGroup lockGroup(Long id,Long scope);ArAdjustmentLink adjustment(Long id);ArLine lockLine(Long id);
 long dependencies(Long id);List<ArLine> dependents(Long id);List<ArInvoiceEligibilityLine> invoiceGroup(ArGroup group);
 boolean voidLine(ArLine line,Long version);boolean updateDocument(ArDocument document,Long version);List<ArChange> changes(Long eventId);
}
