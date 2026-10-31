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
  p.add("ar.auth.clients[0].permissions[0]",()->"ar:create");p.add("ar.auth.clients[0].permissions[1]",()->"ar:read");p.add("ar.auth.clients[0].permissions[2]",()->"ar:receipt:create");
 }
 @LocalServerPort int port;
 @Autowired JdbcTemplate jdbc;
 @Autowired ObjectMapper json;
 @Test @DisplayName("真实HTTP鉴权、创建、重放及详情完整闭环") void createReplayRead() throws Exception {
  assertThat(jdbc.queryForObject("SELECT DATABASE()",String.class)).isEqualTo("oms_ar_test");
  for(String name:List.of("V001__ar_core.sql","V002__ar_audit.sql","V003__ar_receipt.sql"))for(String sql:Files.readString(Path.of("../scratch/dbddl/"+name)).split(";"))if(!sql.isBlank())jdbc.execute(sql);
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
   assertThat(detail.statusCode()).isEqualTo(200);assertThat(json.readTree(detail.body()).at("/data/principal/openAmount").decimalValue()).isEqualByComparingTo("100");
  }
 }
}
