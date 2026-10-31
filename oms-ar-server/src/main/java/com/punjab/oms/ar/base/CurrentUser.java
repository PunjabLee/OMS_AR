package com.punjab.oms.ar.base;
import java.util.Set;
/** 仅由鉴权适配层设置，不接受请求中的自报操作人。 @author Punjab */
public final class CurrentUser {
 private static final ThreadLocal<Identity> CONTEXT = new ThreadLocal<>();
 private CurrentUser(){}
 /** 经验证身份，集合不可修改。 @author Punjab */
 public record Identity(String username,Set<Long> legalEntities,Set<String> permissions){
  public Identity {
   if(username==null || username.isBlank() || username.length()>100 || legalEntities==null || permissions==null)
    throw new ServiceException("操作人身份无效");
   legalEntities=Set.copyOf(legalEntities);permissions=Set.copyOf(permissions);
  }
 }
 public static void set(Identity identity){ if(identity==null)clear();else CONTEXT.set(identity); }
 public static void clear(){CONTEXT.remove();}
 public static String require(Long legalEntityId,String permission){
  Identity identity=CONTEXT.get();
  if(identity==null)throw new ServiceException("无法获取当前操作人");
  if(legalEntityId==null || !identity.legalEntities().contains(legalEntityId) || !identity.permissions().contains(permission))
   throw new ServiceException("无权执行该主体的业务操作");
  return identity.username();
 }
}
