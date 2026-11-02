package com.punjab.oms.ar.dao.finance;
import com.punjab.oms.ar.domain.entity.finance.*;import java.util.List;
/** 同财务聚合事件快照与通知存储。 @author Punjab */
public interface OutboxDao {ArEvent event(Long id);List<ArChange> changes(Long eventId);boolean create(ArOutbox value);}
