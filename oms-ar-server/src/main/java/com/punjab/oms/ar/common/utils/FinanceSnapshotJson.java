package com.punjab.oms.ar.common.utils;
import com.fasterxml.jackson.databind.*;import com.fasterxml.jackson.databind.node.*;
import com.punjab.oms.ar.base.ServiceException;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Component;
/** 版本2快照：小数转十进制文本，避免MySQL JSON二进制浮点舍入；整数ID保持数值。 @author Punjab */
@Component @RequiredArgsConstructor
public class FinanceSnapshotJson {
 private final ObjectMapper json;
 public String normalize(String snapshot){
  if(snapshot==null)return null;
  try{return json.writeValueAsString(exact(json.reader().with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree(snapshot)));}
  catch(Exception e){throw new ServiceException("财务快照解析失败");}
 }
 private JsonNode exact(JsonNode node){
  if(node.isFloatingPointNumber())return TextNode.valueOf(node.decimalValue().toPlainString());
  if(node.isObject()){var object=(ObjectNode)node;var fields=object.properties();for(var entry:fields)entry.setValue(exact(entry.getValue()));}
  else if(node.isArray()){var array=(ArrayNode)node;for(int i=0;i<array.size();i++)array.set(i,exact(array.get(i)));}
  return node;
 }
}
