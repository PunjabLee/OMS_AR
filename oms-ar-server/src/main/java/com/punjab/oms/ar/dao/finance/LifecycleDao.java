package com.punjab.oms.ar.dao.finance;
import com.punjab.oms.ar.domain.entity.finance.*;import com.punjab.oms.ar.domain.bo.finance.LifecycleQueryBO;import java.util.List;
/** 生命周期只读数据访问。 @author Punjab */
public interface LifecycleDao {boolean accessible(LifecycleQueryBO query);long count(LifecycleQueryBO query);List<ArEvent> page(LifecycleQueryBO query);List<ArChange> changes(LifecycleQueryBO query,List<Long> eventIds);}
