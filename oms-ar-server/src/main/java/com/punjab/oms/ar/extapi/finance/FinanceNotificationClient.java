package com.punjab.oms.ar.extapi.finance;
import com.punjab.oms.ar.config.OutboxProperties;import com.punjab.oms.ar.domain.vo.finance.OutboxDeliveryResponse;
import java.net.http.*;import java.time.Duration;import java.util.concurrent.*;import org.springframework.stereotype.Component;import jakarta.annotation.PreDestroy;
/** 固定接收端客户端：不跟随重定向，总超时覆盖响应正文，不记录凭据或响应正文。 @author Punjab */
@Component public class FinanceNotificationClient {
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).version(HttpClient.Version.HTTP_1_1).build();
 /** null表示2xx，其他值为可持久化错误分类。 */
 public String send(OutboxProperties.Receiver receiver,OutboxDeliveryResponse delivery,int timeoutMillis){
  var builder=HttpRequest.newBuilder(receiver.url()).timeout(Duration.ofMillis(timeoutMillis)).header("Content-Type","application/json; charset=utf-8")
    .header("Idempotency-Key","ar-"+delivery.legalEntityId()+"-"+delivery.eventId()+"-"+delivery.destination())
    .header("X-AR-Event-Id",delivery.eventId().toString()).POST(HttpRequest.BodyPublishers.ofString(delivery.payload(),java.nio.charset.StandardCharsets.UTF_8));
  if(receiver.bearerToken()!=null && !receiver.bearerToken().isEmpty())builder.header("Authorization","Bearer "+receiver.bearerToken());
  var future=client.sendAsync(builder.build(),HttpResponse.BodyHandlers.discarding());
  try{int status=future.get(timeoutMillis,TimeUnit.MILLISECONDS).statusCode();return status>=200 && status<300?null:"HTTP_"+status;}
  catch(TimeoutException e){future.cancel(true);return "TIMEOUT";}
  catch(InterruptedException e){future.cancel(true);Thread.currentThread().interrupt();return "INTERRUPTED";}
  catch(ExecutionException e){return e.getCause() instanceof HttpTimeoutException?"TIMEOUT":"IO_ERROR";}
  catch(CancellationException e){return "IO_ERROR";}
 }
 @PreDestroy public void close(){client.shutdownNow();}
}
