package com.punjab.oms.ar.mapper.finance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;import com.punjab.oms.ar.domain.entity.finance.*;import com.punjab.oms.ar.domain.bo.finance.ReconciliationQueryBO;import org.apache.ibatis.annotations.*;import java.util.List;
/** 只读差异聚合。 @author Punjab */
@Mapper public interface ReconciliationMapper extends BaseMapper<ArLine> {@SelectProvider(type=ReconciliationSql.class,method="count") long count(ReconciliationQueryBO query);@SelectProvider(type=ReconciliationSql.class,method="page") List<ArReconciliationDifference> page(ReconciliationQueryBO query);
 @SelectProvider(type=CashReconciliationSql.class,method="count") long cashCount(ReconciliationQueryBO query);
 @SelectProvider(type=CashReconciliationSql.class,method="page") List<ArReconciliationDifference> cashPage(ReconciliationQueryBO query);
}
