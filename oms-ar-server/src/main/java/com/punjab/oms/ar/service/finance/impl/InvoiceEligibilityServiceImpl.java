package com.punjab.oms.ar.service.finance.impl;
import com.punjab.oms.ar.service.finance.InvoiceEligibilityService;import com.punjab.oms.ar.domain.query.finance.InvoiceEligibilityQuery;import com.punjab.oms.ar.domain.vo.finance.InvoiceEligibilityResponse;import com.punjab.oms.ar.domain.entity.finance.ArInvoiceEligibilityLine;import com.punjab.oms.ar.domain.convert.finance.InvoiceEligibilityConverter;import com.punjab.oms.ar.dao.finance.InvoiceEligibilityDao;import com.punjab.oms.ar.config.InvoiceProperties;import com.punjab.oms.ar.common.utils.FinanceValues;import com.punjab.oms.ar.base.*;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.*;import lombok.RequiredArgsConstructor;import jakarta.validation.Validator;
import java.math.BigDecimal;import java.util.*;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;
/** 收款及核销不扣开票额度，负数调整按原组/类型/税率归集。 @author Punjab */
@Service @RequiredArgsConstructor
public class InvoiceEligibilityServiceImpl implements InvoiceEligibilityService {
 private final InvoiceEligibilityDao dao;private final InvoiceEligibilityConverter converter;private final InvoiceProperties properties;private final FinanceValues values;private final Validator validator;
 /** 税率标准化后的共享额度键；NULL与零税率不同。 @author Punjab */
 private record BucketKey(Long groupId,String amountType,BigDecimal taxRate){}
 /** 按查询权限读取各业务组的开票额度和明细。 */
 @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public InvoiceEligibilityResponse query(InvoiceEligibilityQuery query){return calculate(query,"ar:read");}
 /** 按开票写权限计算登记时用于校验的共享额度。 */
 @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public InvoiceEligibilityResponse forRegistration(InvoiceEligibilityQuery query){return calculate(query,"ar:invoice:write");}
 /** 在授权范围内按归属组和税率归集有效应收及已开票净额。 */
 private InvoiceEligibilityResponse calculate(InvoiceEligibilityQuery query,String permission){
  if(query==null || !validator.validate(query).isEmpty() || new HashSet<>(query.groupIds()).size()!=query.groupIds().size())throw new ServiceException("开票额度查询范围无效");CurrentUser.require(query.legalEntityId(),permission);
  var bo=converter.toQueryBO(query);if(dao.groups(bo).size()!=query.groupIds().size())throw new ServiceException("业务组不存在或客户、主体、币种不一致");
  boolean exchangeEnabled=properties.isExchangeDifferenceEnabled();var rows=dao.lines(bo);Map<BucketKey,BigDecimal[]> totals=new LinkedHashMap<>();var lines=new ArrayList<InvoiceEligibilityResponse.Line>();
  for(var row:rows){
   if(row.getAdjustmentOriginalLineId()!=null && (!row.getAmountType().equals(row.getOriginalAmountType()) || !FinanceValues.sameTaxRate(row.getTaxRate(),row.getOriginalTaxRate())))throw new ServiceException("调整应收与原行价税类型或税率不一致，请先更正调整单");
   if(row.getInvoicedAmount().abs().compareTo(row.getReceivableAmount().abs())>0 || (row.getInvoicedAmount().signum()!=0 && row.getInvoicedAmount().signum()!=row.getReceivableAmount().signum()))throw new ServiceException("已开票金额方向或额度不一致，请先对账");
   if("OUTPUT_TAX".equals(row.getAmountType()) && row.getTaxRate()==null)throw new ServiceException("税额行缺少税率，无法计算开票额度");
   String blocked=null;
   if("EXCHANGE_DIFFERENCE".equals(row.getBusinessType())){if(!exchangeEnabled)blocked="EXCHANGE_DIFFERENCE_DISABLED";else if(row.getExchangeBasisId()==null)blocked="EXCHANGE_BASIS_MISSING";}
   var remaining=row.getReceivableAmount().subtract(row.getInvoicedAmount());var rate=row.getTaxRate()==null?null:row.getTaxRate().stripTrailingZeros();
   lines.add(new InvoiceEligibilityResponse.Line(row.getId(),row.getGroupId(),row.getInvoiceGroupId(),row.getAmountType(),rate,row.getReceivableAmount(),row.getInvoicedAmount(),remaining,row.getVersion(),blocked==null,blocked));
   if(blocked!=null)continue;
   var key=new BucketKey(row.getInvoiceGroupId(),row.getAmountType(),rate);var sum=totals.computeIfAbsent(key,k->new BigDecimal[]{BigDecimal.ZERO,BigDecimal.ZERO});sum[0]=sum[0].add(row.getReceivableAmount());sum[1]=sum[1].add(row.getInvoicedAmount());
  }
  var buckets=new ArrayList<InvoiceEligibilityResponse.Bucket>();
  for(var entry:totals.entrySet()){var key=entry.getKey();var sum=entry.getValue();var remaining=sum[0].subtract(sum[1]);buckets.add(new InvoiceEligibilityResponse.Bucket(key.groupId(),key.amountType(),key.taxRate(),sum[0],sum[1],remaining,remaining.max(BigDecimal.ZERO)));}
  Map<Long,Integer> order=new HashMap<>();for(int i=0;i<query.groupIds().size();i++)order.put(query.groupIds().get(i),i);
  buckets.sort(Comparator.comparingInt((InvoiceEligibilityResponse.Bucket b)->order.get(b.groupId())).thenComparing(InvoiceEligibilityResponse.Bucket::amountType).thenComparing(InvoiceEligibilityResponse.Bucket::taxRate,Comparator.nullsFirst(Comparator.naturalOrder())));
  return new InvoiceEligibilityResponse(buckets,lines,hash(values.json(List.of(query,exchangeEnabled,rows))));
 }
 /** 对序列化输入计算摘要，用于检测内容变化。 */
 private String hash(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new ServiceException("开票额度摘要生成失败");}}
}
