package com.punjab.oms.ar.base;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.http.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/** 统一异常响应，不向客户端暴露SQL和内部异常内容。 @author Punjab */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
 private static final Logger LOG=LoggerFactory.getLogger(ApiExceptionHandler.class);
 @ExceptionHandler(AccessDeniedException.class) public ResponseEntity<ApiReturn<Void>> denied(AccessDeniedException e){return ResponseEntity.status(403).body(ApiReturn.error(403,e.getMessage()));}
 @ExceptionHandler(ServiceException.class) public ResponseEntity<ApiReturn<Void>> business(ServiceException e){return ResponseEntity.status(409).body(ApiReturn.error(409,e.getMessage()));}
 @ExceptionHandler(DataIntegrityViolationException.class) public ResponseEntity<ApiReturn<Void>> conflict(DataIntegrityViolationException e){return ResponseEntity.status(409).body(ApiReturn.error(409,"数据已变化或业务记录重复，请刷新后重试"));}
 @ExceptionHandler(Exception.class) public ResponseEntity<ApiReturn<Void>> unexpected(Exception e){LOG.error("业务请求处理失败",e);return ResponseEntity.internalServerError().body(ApiReturn.error(500,"服务处理失败"));}
 @Override protected ResponseEntity<Object> handleExceptionInternal(Exception ex,Object body,HttpHeaders headers,HttpStatusCode status,WebRequest request){
  return new ResponseEntity<>(ApiReturn.error(status.value(),"请求格式或参数无效"),headers,status);
 }
}
