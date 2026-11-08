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
  p.add("ar.auth.clients[0].permissions[0]",()->"ar:create");p.add("ar.auth.clients[0].permissions[1]",()->"ar:read");p.add("ar.auth.clients[0].permissions[2]",()->"ar:receipt:create");p.add("ar.auth.clients[0].permissions[3]",()->"ar:settle");p.add("ar.auth.clients[0].permissions[4]",()->"ar:reverse");p.add("ar.auth.clients[0].permissions[5]",()->"ar:balance:transfer");p.add("ar.auth.clients[0].permissions[6]",()->"ar:invoice:write");p.add("ar.auth.clients[0].permissions[7]",()->"ar:void");p.add("ar.auth.clients[0].permissions[8]",()->"ar:void:upstream:WMS");
 }
 @LocalServerPort int port;
 @Autowired JdbcTemplate jdbc;
 @Autowired ObjectMapper json;
 @Test @DisplayName("真实HTTP鉴权、创建、重放及详情完整闭环") void createReplayRead() throws Exception {
  assertThat(jdbc.queryForObject("SELECT DATABASE()",String.class)).isEqualTo("oms_ar_test");
  for(String name:List.of("V001__ar_core.sql","V002__ar_audit.sql","V011__ar_outbox.sql","V003__ar_receipt.sql","V013__ar_cash_account.sql","V015__ar_cash_receipt_lot.sql","V019__ar_cash_receipt_reversal.sql","V004__ar_settlement.sql","V005__ar_adjustment_link.sql","V006__ar_customer_balance.sql","V018__ar_credit_cash_scope.sql","V007__ar_balance_settlement.sql","V008__ar_invoice.sql","V009__ar_exchange_basis.sql","V010__ar_source_guard.sql"))if(com.punjab.oms.ar.ScratchCreditSchema.shouldApply(jdbc,Path.of("../scratch/dbddl/"+name)))for(String sql:Files.readString(Path.of("../scratch/dbddl/"+name)).split(";"))if(!sql.isBlank())jdbc.execute(sql);
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
   long creditVersion=jdbc.queryForObject("SELECT version FROM oms_ar_line WHERE id=?",Long.class,creditLine);
   String balancesRoot="http://127.0.0.1:"+port+"/api/finance/customer-balances";
   long lotId=post(client,balancesRoot+"/transfer",Map.of("requestKey",UUID.randomUUID().toString(),"legalEntityId",1,"negativeLineId",creditLine,"amount",7,"expectedLineVersion",creditVersion,"reason","http credit transfer")).get("data").asLong();assertThat(lotId).isPositive();
   var lotResult=client.send(HttpRequest.newBuilder(URI.create(balancesRoot+"/lot-detail?id="+lotId+"&legalEntityId=1")).header("Authorization","Bearer "+TOKEN).GET().build(),HttpResponse.BodyHandlers.ofString());assertThat(lotResult.statusCode()).isEqualTo(200);assertThat(json.readTree(lotResult.body()).at("/data/sourceLineId").asLong()).isEqualTo(creditLine);
   long accountId=json.readTree(lotResult.body()).at("/data/accountId").asLong();
   var spendRequest=Map.of("legalEntityId",1,"accountId",accountId,"amount",7,"groupIds",List.of(groupId),"strategy","GROUP_TAX_FIRST");
   var spendPreview=post(client,balancesRoot+"/settlements/preview",spendRequest);assertThat(spendPreview.at("/data/usedAmount").decimalValue()).isEqualByComparingTo("7");
   long spendId=post(client,balancesRoot+"/settlements/confirm",Map.of("requestKey",UUID.randomUUID().toString(),"preview",spendRequest,"expectedFingerprint",spendPreview.at("/data/fingerprint").asText(),"reason","http credit spend")).get("data").asLong();assertThat(spendId).isPositive();
   post(client,settlementRoot+"/reverse",Map.of("requestKey",UUID.randomUUID().toString(),"legalEntityId",1,"settlementId",spendId,"reason","http credit spend reverse"));
   post(client,balancesRoot+"/reverse-transfer",Map.of("requestKey",UUID.randomUUID().toString(),"legalEntityId",1,"lotId",lotId,"reason","http credit reverse"));
   assertThat(jdbc.queryForObject("SELECT open_amount FROM oms_ar_line WHERE id=?",java.math.BigDecimal.class,creditLine)).isEqualByComparingTo("-10");
   var fxBody=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(body);fxBody.put("requestKey",UUID.randomUUID().toString());fxBody.put("businessType","EXCHANGE_DIFFERENCE");
   var fxLine=(com.fasterxml.jackson.databind.node.ObjectNode)fxBody.at("/lines/0");fxLine.put("amount",20);fxLine.set("exchangeBasis",json.valueToTree(Map.of("originalCurrency","USD","originalAmount",100,"oldRate",new java.math.BigDecimal("7.1"),"newRate",new java.math.BigDecimal("7.3"),"rateDirection","SETTLEMENT_PER_ORIGINAL")));
   long fxDoc=post(client,root+"/create",fxBody).get("data").asLong();
   var fxDetail=client.send(HttpRequest.newBuilder(URI.create(root+"/detail?id="+fxDoc+"&legalEntityId=1")).header("Authorization","Bearer "+TOKEN).GET().build(),HttpResponse.BodyHandlers.ofString());assertThat(fxDetail.statusCode()).isEqualTo(200);assertThat(json.readTree(fxDetail.body()).at("/data/lines/0/exchangeBasis/confirmedDifference").decimalValue()).isEqualByComparingTo("20");assertThat(json.readTree(fxDetail.body()).at("/data/lines/0/version").asLong(-1)).isZero();
   var quota=post(client,"http://127.0.0.1:"+port+"/api/finance/invoice-links/eligibility",Map.of("legalEntityId",1,"customerId",1,"currency","CNY","groupIds",List.of(groupId)));
   assertThat(quota.at("/data/buckets/0/blueAvailableAmount").decimalValue()).isEqualByComparingTo("100");assertThat(quota.at("/data/fingerprint").asText()).hasSize(64);
   long fxGroup=jdbc.queryForObject("SELECT group_id FROM oms_ar_line WHERE document_id=?",Long.class,fxDoc);
   var fxQuota=post(client,"http://127.0.0.1:"+port+"/api/finance/invoice-links/eligibility",Map.of("legalEntityId",1,"customerId",1,"currency","CNY","groupIds",List.of(fxGroup)));assertThat(fxQuota.at("/data/buckets").size()).isZero();assertThat(fxQuota.at("/data/lines/0/blockedReason").asText()).isEqualTo("EXCHANGE_DIFFERENCE_DISABLED");
   String invoiceRoot="http://127.0.0.1:"+port+"/api/finance/invoice-links";
   long priceLine=json.readTree(detail.body()).at("/data/lines/0/id").asLong();
   var blueRequest=Map.of("legalEntityId",1,"customerId",1,"currency","CNY","kind","BLUE","totalAmount",100,"lines",List.of(Map.of("invoiceLineId","1","receivableLineId",priceLine,"amount",100)));
   var bluePreview=post(client,invoiceRoot+"/preview",blueRequest);
   var blueRegister=Map.of("requestKey",UUID.randomUUID().toString(),"sourceSystem","HTTP","sourceId",UUID.randomUUID().toString(),"invoiceNo","HTTP-BLUE","invoiceDate","2026-11-01","preview",blueRequest,"expectedFingerprint",bluePreview.at("/data/fingerprint").asText(),"reason","http blue");
   long blueId=post(client,invoiceRoot+"/register",blueRegister).get("data").asLong();
   assertThat(post(client,invoiceRoot+"/register",blueRegister).get("data").asLong()).isEqualTo(blueId);
   var blueDetail=client.send(HttpRequest.newBuilder(URI.create(invoiceRoot+"/detail?id="+blueId+"&legalEntityId=1")).header("Authorization","Bearer "+TOKEN).GET().build(),HttpResponse.BodyHandlers.ofString());
   assertThat(blueDetail.statusCode()).isEqualTo(200);var blueData=json.readTree(blueDetail.body());
   assertThat(blueData.at("/data/allocations/0/redAvailableAmount").decimalValue()).isEqualByComparingTo("100");
   long originalAllocation=blueData.at("/data/allocations/0/id").asLong();
   var redRequest=Map.of("legalEntityId",1,"customerId",1,"currency","CNY","kind","RED","originalInvoiceId",blueId,"totalAmount",-10,"lines",List.of(Map.of("invoiceLineId","1","receivableLineId",creditLine,"amount",-10,"originalAllocationId",originalAllocation)));
   var redPreview=post(client,invoiceRoot+"/preview",redRequest);
   long redId=post(client,invoiceRoot+"/register",Map.of("requestKey",UUID.randomUUID().toString(),"sourceSystem","HTTP","sourceId",UUID.randomUUID().toString(),"invoiceNo","HTTP-RED","invoiceDate","2026-11-01","preview",redRequest,"expectedFingerprint",redPreview.at("/data/fingerprint").asText(),"reason","http red")).get("data").asLong();
   var blueReverse=Map.of("requestKey",UUID.randomUUID().toString(),"legalEntityId",1,"invoiceId",blueId,"reason","http blue reverse");
   var blocked=client.send(HttpRequest.newBuilder(URI.create(invoiceRoot+"/reverse")).header("Authorization","Bearer "+TOKEN).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(blueReverse))).build(),HttpResponse.BodyHandlers.ofString());
   assertThat(blocked.statusCode()).isEqualTo(409);
   post(client,invoiceRoot+"/reverse",Map.of("requestKey",UUID.randomUUID().toString(),"legalEntityId",1,"invoiceId",redId,"reason","http red reverse"));
   post(client,invoiceRoot+"/reverse",blueReverse);
   assertThat(jdbc.queryForObject("SELECT invoiced_amount FROM oms_ar_line WHERE id=?",java.math.BigDecimal.class,priceLine)).isEqualByComparingTo("0");
   assertThat(jdbc.queryForObject("SELECT open_amount FROM oms_ar_line WHERE id=?",java.math.BigDecimal.class,priceLine)).isEqualByComparingTo("100");
   var receiptSettlementDetail=client.send(HttpRequest.newBuilder(URI.create(settlementRoot+"/detail?id="+settlementId+"&legalEntityId=1")).header("Authorization","Bearer "+TOKEN).GET().build(),HttpResponse.BodyHandlers.ofString());
   assertThat(receiptSettlementDetail.statusCode()).isEqualTo(200);var receiptSettlementData=json.readTree(receiptSettlementDetail.body());
   assertThat(receiptSettlementData.at("/data/header/status").asText()).isEqualTo("REVERSED");assertThat(receiptSettlementData.at("/data/header/receiptId").asLong()).isEqualTo(receiptId);assertThat(receiptSettlementData.at("/data/allocations/0/amount").decimalValue()).isEqualByComparingTo("20");
   var offsetDetail=client.send(HttpRequest.newBuilder(URI.create(settlementRoot+"/detail?id="+offsetId+"&legalEntityId=1")).header("Authorization","Bearer "+TOKEN).GET().build(),HttpResponse.BodyHandlers.ofString());assertThat(offsetDetail.statusCode()).isEqualTo(200);assertThat(json.readTree(offsetDetail.body()).at("/data/allocations/0/negativeLineId").asLong()).isEqualTo(creditLine);
   var spendDetail=client.send(HttpRequest.newBuilder(URI.create(settlementRoot+"/detail?id="+spendId+"&legalEntityId=1")).header("Authorization","Bearer "+TOKEN).GET().build(),HttpResponse.BodyHandlers.ofString());assertThat(spendDetail.statusCode()).isEqualTo(200);var spendData=json.readTree(spendDetail.body());assertThat(spendData.at("/data/accountId").asLong()).isEqualTo(accountId);assertThat(spendData.at("/data/usages/0/lotId").asLong()).isEqualTo(jdbc.queryForObject("SELECT lot_id FROM oms_ar_balance_usage WHERE settlement_id=? ORDER BY sequence_no,id LIMIT 1",Long.class,spendId));assertThat(spendData.at("/data/usages/0/amount").decimalValue()).isEqualByComparingTo("7");
   var wrongSettlement=client.send(HttpRequest.newBuilder(URI.create(settlementRoot+"/detail?id="+settlementId+"&legalEntityId=2")).header("Authorization","Bearer "+TOKEN).GET().build(),HttpResponse.BodyHandlers.ofString());assertThat(wrongSettlement.statusCode()).isEqualTo(403);
   var settlementPage=post(client,settlementRoot+"/page",Map.of("legalEntityId",1,"customerId",1,"currency","CNY","status","REVERSED","settlementType","RECEIPT","receiptId",receiptId,"page",1,"size",1));
   assertThat(settlementPage.at("/data/total").asLong()).isEqualTo(1);assertThat(settlementPage.at("/data/records/0/id").asLong()).isEqualTo(settlementId);
   var accountPage=post(client,balancesRoot+"/page",Map.of("legalEntityId",1,"customerId",1,"currency","CNY"));assertThat(accountPage.at("/data/total").asLong()).isEqualTo(1);assertThat(accountPage.at("/data/records/0/id").asLong()).isEqualTo(accountId);
   var lotPage=post(client,balancesRoot+"/lots/page",Map.of("legalEntityId",1,"accountId",accountId,"sourceLineId",creditLine,"status","REVERSED"));assertThat(lotPage.at("/data/total").asLong()).isEqualTo(1);assertThat(lotPage.at("/data/records/0/id").asLong()).isEqualTo(lotId);
   var emptyPage=post(client,settlementRoot+"/page",Map.of("legalEntityId",1,"receiptId",receiptId,"page",2147483647,"size",1));assertThat(emptyPage.at("/data/total").asLong()).isEqualTo(1);assertThat(emptyPage.at("/data/records").size()).isZero();
   for(String endpoint:List.of(settlementRoot+"/page",balancesRoot+"/page",balancesRoot+"/lots/page")){
    var badPage=client.send(HttpRequest.newBuilder(URI.create(endpoint)).header("Authorization","Bearer "+TOKEN).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{\"legalEntityId\":1,\"size\":201}")).build(),HttpResponse.BodyHandlers.ofString());assertThat(badPage.statusCode()).isEqualTo(400);
    var forbiddenPage=client.send(HttpRequest.newBuilder(URI.create(endpoint)).header("Authorization","Bearer "+TOKEN).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{\"legalEntityId\":2}")).build(),HttpResponse.BodyHandlers.ofString());assertThat(forbiddenPage.statusCode()).isEqualTo(403);
   }
   var reconciliationPage=post(client,"http://127.0.0.1:"+port+"/api/finance/reconciliation/page",Map.of("legalEntityId",1,"customerId",Long.MAX_VALUE,"currency","CNY"));assertThat(reconciliationPage.at("/data/total").asLong()).isZero();
   var voidBody=Map.of("requestKey",UUID.randomUUID().toString(),"legalEntityId",1,"documentId",id,"reason","http void");
   var voidResult=post(client,root+"/void",voidBody);assertThat(voidResult.at("/data/lineIds/0").asLong()).isEqualTo(priceLine);
   assertThat(post(client,root+"/void",voidBody).at("/data/eventId").asLong()).isEqualTo(voidResult.at("/data/eventId").asLong());
   var voidDetail=client.send(HttpRequest.newBuilder(URI.create(root+"/detail?id="+id+"&legalEntityId=1")).header("Authorization","Bearer "+TOKEN).GET().build(),HttpResponse.BodyHandlers.ofString());
   assertThat(voidDetail.statusCode()).isEqualTo(200);assertThat(json.readTree(voidDetail.body()).at("/data/document/voidReason").asText()).isEqualTo("http void");assertThat(json.readTree(voidDetail.body()).at("/data/settlementStatus").asText()).isEqualTo("VOID");
   var lifecycle=post(client,root+"/lifecycle/page",Map.of("legalEntityId",1,"documentId",id,"page",1,"size",200));
   assertThat(lifecycle.at("/data/records/0/eventType").asText()).isEqualTo("RECEIVABLE_VOIDED");
   var eventTypes=new java.util.HashSet<String>();for(var event:lifecycle.at("/data/records"))eventTypes.add(event.get("eventType").asText());
   assertThat(eventTypes).contains("CREATED","RECEIPT_SETTLED","OFFSET_SETTLED","BALANCE_SETTLED","INVOICE_REGISTERED","INVOICE_REVERSED");
   String canceledSource=UUID.randomUUID().toString();
   var upstream=post(client,root+"/void-upstream",Map.of("requestKey",UUID.randomUUID().toString(),"legalEntityId",1,"sourceSystem","WMS","sources",List.of(Map.of("sourceType","OUTBOUND","sourceId",canceledSource)),"reason","cancel before delivery"));
   assertThat(upstream.at("/data/lineIds").size()).isZero();
   var late=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(body);late.put("requestKey",UUID.randomUUID().toString());((com.fasterxml.jackson.databind.node.ObjectNode)late.at("/lines/0")).set("source",json.valueToTree(Map.of("system","WMS","type","OUTBOUND","id",canceledSource,"number",canceledSource,"eventId",UUID.randomUUID().toString(),"outputKey","price")));
   var lateResult=client.send(HttpRequest.newBuilder(URI.create(root+"/create")).header("Authorization","Bearer "+TOKEN).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(late))).build(),HttpResponse.BodyHandlers.ofString());assertThat(lateResult.statusCode()).isEqualTo(409);
   assertThat(detail.statusCode()).isEqualTo(200);assertThat(json.readTree(detail.body()).at("/data/principal/openAmount").decimalValue()).isEqualByComparingTo("100");
  }
 }
 private com.fasterxml.jackson.databind.JsonNode post(HttpClient client,String url,Object body)throws Exception{
  var response=client.send(HttpRequest.newBuilder(URI.create(url)).header("Authorization","Bearer "+TOKEN).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
  assertThat(response.statusCode()).isEqualTo(200);return json.readTree(response.body());
 }
}
