package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.base.PageResult;
import com.punjab.oms.ar.domain.query.finance.*;
import com.punjab.oms.ar.domain.vo.finance.ReceivableResponse;
/** 应收查询。 @author Punjab */
public interface ReceivableQueryService {
 ReceivableResponse detail(ReceivableDetailQuery query);
 PageResult<ReceivableResponse.Document> page(ReceivablePageQuery query);
}
