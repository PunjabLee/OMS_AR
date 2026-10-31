package com.punjab.oms.ar.base;
/** 统一响应，成功code=0。 @author Punjab */
public record ApiReturn<T>(int code,T data,String msg){
 public static <T> ApiReturn<T> of(T data){return new ApiReturn<>(0,data,"成功");}
 public static ApiReturn<Void> error(int code,String message){return new ApiReturn<>(code,null,message);}
}
