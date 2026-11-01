package com.punjab.oms.ar.controller.finance;
import com.punjab.oms.ar.ArApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
/** scripts/test-mysql-http：独立测试库与随机本机HTTP端口。 @author Punjab */
@SpringBootTest(classes=ArApplication.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("pre")
@EnabledIfSystemProperty(named="ar.http.real",matches="true")
@EnabledIfSystemProperty(named="ar.http.real.commit",matches="true")
class ReceivableHttpRealTest {
 private static final String TOKEN="real-http-test-only-token-01234567890123456789";
 @DynamicPropertySource static void config(DynamicPropertyRegistry p){
  p.add("spring.datasource.url",()->"jdbc:mysql://127.0.0.1:13306/oms_ar_test");
  p.add("spring.datasource.username",()->"oms_ar");p.add("spring.datasource.password",()->System.getenv("AR_TEST_PASSWORD"));
  p.add("ar.auth.clients[0].token",()->TOKEN);p.add("ar.auth.clients[0].username",()->"http-integration");
  p.add("ar.auth.clients[0].legal-entities[0]",()->"1");
  p.add("ar.auth.clients[0].permissions[0]",()->"ar:create");p.add("ar.auth.clients[0].permissions[1]",()->"ar:read");p.add("ar.auth.clients[0].permissions[2]",()->"ar:receipt:create");p.add("ar.auth.clients[0].permissions[3]",()->"ar:settle");p.add("ar.auth.clients[0].permissions[4]",()->"ar:reverse");
 }
 @LocalServerPort int port;
 @Autowired JdbcTemplate jdbc;
 @Autowired ObjectMapper json;
 @Test @DisplayName("真实HTTP鉴权、创建、重放及详情完整闭环") void createReplayRead() throws Exception {
  assertThat(jdbc.queryForObject("SELECT DATABASE()",String.class)).isEqualTo("oms_ar_test");
  for(String name:List.of("V001__ar_core.sql","V002__ar_audit.sql","V003__ar_receipt.sql","V004__ar_settlement.sql","V005__ar_adjustment_link.sql"))for(String sql:Files.readString(Path.of("../scratch/dbddl/"+name)).split(";"))if(!sql.isBlank())jdbc.execute(sql);
  String root="http://127.0.0.1:"+port+"/api/finance/receivables";
  String body="{\"requestKey\":\""+UUID.randomUUID()+"\",\"customerId\":1,\"legalEntityId\":1,\"currency\":\"CNY\",\"businessType\":\"SERVICE\",\"businessDate\":\"2026-10-31\",\"reason\":\"http test\",\"lines\":[{\"groupKey\":\"A\",\"amountType\":\"PRINCIPAL\",\"amount\":100}]}";
  try(var client=HttpClient.newHttpClient()){
   var denied=client.send(HttpRequest.newBuilder(URI.create(root+"/detail?id=1&legalEntityId=1")).GET().build(),HttpResponse.BodyHandlers.ofString());
   assertThat(denied.statusCode()).isEqualTo(401);
   var create=HttpRequest.newBuilder(URI.create(root+"/create")).header("Authorization","Bearer "+TOKEN).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
   var result=client.send(create,HttpResponse.BodyHandlers.ofString());assertThat(result.statusCode()).isEqualTo(200);
   long id=json.readTree(result.body()).get("data").asLong();assertThat(id).isPositive();
   assertThat(json.readTree(client.send(create,HttpResponse.BodyHandlers.ofString()).body()).get("data").asLong()).isEqualTo(id);
   var detail=client.send(HttpRequest.newBuilder(URI.create(root+"/detail?id="+id+"&legalEntityId=1")).header("Authorization","Bearer "+TOKEN).GET().build(),HttpResponse.BodyHandlers.ofString());
   String receiptBody=json.writeValueAsString(java.util.Map.of("requestKey",UUID.randomUUID().toString(),"customerId",1,"legalEntityId",1,"currency","CNY","amount",120,"receivedDate","2026-10-31","sourceSystem","TEST","sourceId",UUID.randomUUID().toString(),"sourceNo","TEST-NO"));
   var receipt=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/finance/receipts/create")).header("Authorization","Bearer "+TOKEN).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(receiptBody)).build(),HttpResponse.BodyHandlers.ofString());
   assertThat(receipt.statusCode()).isEqualTo(200);long receiptId=json.readTree(receipt.body()).get("data").asLong();assertThat(receiptId).isPositive();
   var balance=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/finance/receipts/detail?id="+receiptId+"&legalEntityId=1")).header("Authorization","Bearer "+TOKEN).GET().build(),HttpResponse.BodyHandlers.ofString());
   assertThat(balance.statusCode()).isEqualTo(200);assertThat(json.readTree(balance.body()).at("/data/availableAmount").decimalValue()).isEqualByComparingTo("120");
   String settlementRoot="http://127.0.0.1:"+port+"/api/finance/settlements";
   long groupId=json.readTree(detail.body()).at("/data/lines/0/groupId").asLong();
   var previewBody=java.util.Map.of("legalEntityId",1,"receiptId",receiptId,"amount",20,"groupIds",java.util.List.of(groupId),"strategy","GROUP_TAX_FIRST");
   var preview=client.send(HttpRequest.newBuilder(URI.create(settlementRoot+"/preview")).header("Authorization","Bearer "+TOKEN).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(previewBody))).build(),HttpResponse.BodyHandlers.ofString());
   assertThat(preview.statusCode()).isEqualTo(200);String fingerprint=json.readTree(preview.body()).at("/data/fingerprint").asText();
   var confirmBody=java.util.Map.of("requestKey",UUID.randomUUID().toString(),"preview",previewBody,"expectedFingerprint",fingerprint,"reason","http settle");
   var confirmation=client.send(HttpRequest.newBuilder(URI.create(settlementRoot+"/confirm")).header("Authorization","Bearer "+TOKEN).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(confirmBody))).build(),HttpResponse.BodyHandlers.ofString());
   assertThat(confirmation.statusCode()).isEqualTo(200);long settlementId=json.readTree(confirmation.body()).at("/data").asLong();assertThat(settlementId).isPositive();
   var reverseBody=java.util.Map.of("requestKey",UUID.randomUUID().toString(),"legalEntityId",1,"settlementId",settlementId,"reason","http reverse");
   var reversal=client.send(HttpRequest.newBuilder(URI.create(settlementRoot+"/reverse")).header("Authorization","Bearer "+TOKEN).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(reverseBody))).build(),HttpResponse.BodyHandlers.ofString());assertThat(reversal.statusCode()).isEqualTo(200);
   assertThat(jdbc.queryForObject("SELECT allocated_amount FROM oms_ar_receipt WHERE id=?",java.math.BigDecimal.class,receiptId)).isEqualByComparingTo("0");
   var creditBody=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(body);creditBody.put("requestKey",UUID.randomUUID().toString());
   ((com.fasterxml.jackson.databind.node.ObjectNode)creditBody.at("/lines/0")).put("amount",-10);
   long creditDoc=post(client,root+"/create",creditBody).get("data").asLong();
   long creditLine=jdbc.queryForObject("SELECT id FROM oms_ar_line WHERE document_id=?",Long.class,creditDoc);
   var offsetRequest=Map.of("legalEntityId",1,"negativeLineId",creditLine,"amount",10,"groupIds",List.of(groupId),"continueOtherTargets",false,"strategy","GROUP_TAX_FIRST");
   var offsetPreview=post(client,"http://127.0.0.1:"+port+"/api/finance/offsets/preview",offsetRequest);
   assertThat(offsetPreview.at("/data/usedAmount").decimalValue()).isEqualByComparingTo("10");
   long offsetId=post(client,"http://127.0.0.1:"+port+"/api/finance/offsets/confirm",Map.of("requestKey",UUID.randomUUID().toString(),"preview",offsetRequest,"expectedFingerprint",offsetPreview.at("/data/fingerprint").asText(),"reason","http offset")).get("data").asLong();
   assertThat(offsetId).isPositive();
   post(client,settlementRoot+"/reverse",Map.of("requestKey",UUID.randomUUID().toString(),"legalEntityId",1,"settlementId",offsetId,"reason","http offset reverse"));
   assertThat(jdbc.queryForObject("SELECT open_amount FROM oms_ar_line WHERE id=?",java.math.BigDecimal.class,creditLine)).isEqualByComparingTo("-10");
   assertThat(detail.statusCode()).isEqualTo(200);assertThat(json.readTree(detail.body()).at("/data/principal/openAmount").decimalValue()).isEqualByComparingTo("100");
  }
 }
 private com.fasterxml.jackson.databind.JsonNode post(HttpClient client,String url,Object body)throws Exception{
  var response=client.send(HttpRequest.newBuilder(URI.create(url)).header("Authorization","Bearer "+TOKEN).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
  assertThat(response.statusCode()).isEqualTo(200);return json.readTree(response.body());
 }
}
