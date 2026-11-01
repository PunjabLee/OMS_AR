package com.punjab.oms.ar.dao.finance;
import com.punjab.oms.ar.domain.entity.finance.*;import java.util.List;
/** 同一finance结算聚合的数据访问，事务由Service控制。 @author Punjab */
public interface SettlementDao {
 ArReceipt receipt(Long id,Long legalEntityId,boolean lock);
 List<ArGroup> groups(List<Long> ids,Long legalEntityId,boolean lock);
 List<ArLine> groupLines(List<Long> groups,boolean lock);
 List<ArLine> linesByIds(List<Long> ids,boolean lock);
 boolean updateReceipt(ArReceipt value,Long expectedVersion);
 boolean updateLine(ArLine value,Long expectedVersion);
 boolean create(ArSettlement value);boolean update(ArSettlement value,Long expectedVersion);
 boolean allocate(ArSettlementAllocation value);
 ArSettlement settlement(Long id,Long legalEntityId,boolean lock);
 List<ArSettlementAllocation> allocations(Long id);
 Long postedEvent(Long id);
}
