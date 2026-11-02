package com.punjab.oms.ar.service.finance;
/** 仅内部调度，无HTTP入口；返回本轮领取数量。 @author Punjab */
public interface OutboxDispatchService {int dispatchOnce();}
