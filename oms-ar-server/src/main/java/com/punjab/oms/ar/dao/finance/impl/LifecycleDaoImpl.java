package com.punjab.oms.ar.dao.finance.impl;
import com.punjab.oms.ar.dao.finance.LifecycleDao;import com.punjab.oms.ar.domain.entity.finance.*;import com.punjab.oms.ar.domain.bo.finance.LifecycleQueryBO;import com.punjab.oms.ar.mapper.finance.*;import com.baomidou.mybatisplus.extension.repository.CrudRepository;import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;import org.springframework.stereotype.Repository;import lombok.RequiredArgsConstructor;import java.util.List;
/** 保留作废历史，排除逻辑删除对象。 @author Punjab */
@Repository @RequiredArgsConstructor public class LifecycleDaoImpl extends CrudRepository<LifecycleMapper,ArEvent> implements LifecycleDao {
 private final ArDocumentMapper documents;
 public boolean accessible(LifecycleQueryBO q){return documents.selectCount(new LambdaQueryWrapper<ArDocument>().eq(ArDocument::getId,q.getDocumentId()).eq(ArDocument::getLegalEntityId,q.getLegalEntityId()).eq(ArDocument::getDeleteStatus,false))==1;}
 public long count(LifecycleQueryBO q){return baseMapper.count(q);}public List<ArEvent> page(LifecycleQueryBO q){return baseMapper.page(q);}public List<ArChange> changes(LifecycleQueryBO q,List<Long> ids){return ids.isEmpty()?List.of():baseMapper.changes(q,ids);}
}
