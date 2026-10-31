package com.punjab.oms.ar.dao.finance.impl;
import com.punjab.oms.ar.dao.finance.ReceiptDao;
import com.punjab.oms.ar.domain.entity.finance.ArReceipt;
import com.punjab.oms.ar.domain.bo.finance.ReceiptDetailQueryBO;
import com.punjab.oms.ar.mapper.finance.ArReceiptMapper;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Repository;
/** 收款数据访问，不声明事务。 @author Punjab */
@Repository public class ReceiptDaoImpl extends CrudRepository<ArReceiptMapper,ArReceipt> implements ReceiptDao {
 public boolean create(ArReceipt value){return baseMapper.insert(value)==1;}
 public ArReceipt detail(ReceiptDetailQueryBO query){return baseMapper.selectOne(new LambdaQueryWrapper<ArReceipt>().eq(ArReceipt::getId,query.id()).eq(ArReceipt::getLegalEntityId,query.legalEntityId()).eq(ArReceipt::getDeleteStatus,false));}
}
