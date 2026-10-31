package com.punjab.oms.ar.config;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.punjab.oms.ar.base.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
/** 不信任客户端身份头；token匹配服务端配置并清理线程上下文。 @author Punjab */
public class ApiAuthenticationFilter extends OncePerRequestFilter {
 private final ObjectMapper json;
 private final List<Credential> credentials;
 /** 凭据只持有摘要。 @author Punjab */
 private record Credential(byte[] digest,CurrentUser.Identity identity){}
 public ApiAuthenticationFilter(ApiAuthProperties properties,ObjectMapper json){
  this.json=json;var values=new ArrayList<Credential>();var unique=new HashSet<String>();
  for(var client:properties.getClients()){
   if(client.token()==null || client.token().isBlank())continue;
   if(client.token().length()<32 || !unique.add(client.token()))throw new IllegalArgumentException("API凭据长度不足或重复");
   values.add(new Credential(hash(client.token()),new CurrentUser.Identity(client.username(),client.legalEntities(),client.permissions())));
  }
  credentials=List.copyOf(values);
 }
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
  CurrentUser.clear();
  try{
   String header=request.getHeader("Authorization");CurrentUser.Identity identity=null;
   if(header!=null && header.startsWith("Bearer ") && header.length()<=4096){
    byte[] supplied=hash(header.substring(7));
    for(var credential:credentials)if(MessageDigest.isEqual(supplied,credential.digest()))identity=credential.identity();
   }
   if(identity==null){response.setStatus(401);response.setContentType("application/json");response.setCharacterEncoding("UTF-8");json.writeValue(response.getWriter(),ApiReturn.error(401,"需要有效访问凭据"));return;}
   CurrentUser.set(identity);chain.doFilter(request,response);
  }finally{CurrentUser.clear();}
 }
 private byte[] hash(String token){try{return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException("凭据摘要不可用",e);}}
}
