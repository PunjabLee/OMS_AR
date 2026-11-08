package com.punjab.oms.ar.dao.finance.impl;
import com.punjab.oms.ar.dao.finance.ReconciliationDao;import com.punjab.oms.ar.mapper.finance.ReconciliationMapper;import com.punjab.oms.ar.domain.entity.finance.*;import com.punjab.oms.ar.domain.bo.finance.ReconciliationQueryBO;import com.punjab.oms.ar.base.PageResult;import com.baomidou.mybatisplus.extension.repository.CrudRepository;import org.springframework.stereotype.Repository;
/** 不执行修复写入。 @author Punjab */
@Repository public class ReconciliationDaoImpl extends CrudRepository<ReconciliationMapper,ArLine> implements ReconciliationDao {public PageResult<ArReconciliationDifference> page(ReconciliationQueryBO q){return new PageResult<>(baseMapper.count(q),q.getPage(),q.getSize(),baseMapper.page(q));}
 public PageResult<ArReconciliationDifference> cashPage(ReconciliationQueryBO q) {
     return new PageResult<>(baseMapper.cashCount(q),q.getPage(),q.getSize(),baseMapper.cashPage(q));
 }
}
