package com.punjab.oms.ar.base;
import java.util.List;
/** 分页结果。 @author Punjab */
public record PageResult<T>(long total,int page,int size,List<T> records){public PageResult{records=List.copyOf(records);}}
