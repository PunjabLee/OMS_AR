package com.punjab.oms.ar.tools;

import com.punjab.oms.ar.ArApplication;
import com.punjab.oms.ar.base.CurrentUser;
import com.punjab.oms.ar.domain.dto.finance.*;
import com.punjab.oms.ar.enums.*;
import com.punjab.oms.ar.service.finance.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** scripts/test-mysql-migration 在独占的随机库验证期初及两种反核销。 @author Punjab */
@SpringBootTest(classes=ArApplication.class,webEnvironment=SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("pre")
@EnabledIfSystemProperty(named="ar.migration.real.commit",matches="true")
class CashCutoverRealTest {
    @DynamicPropertySource static void config(DynamicPropertyRegistry p){
        String prefix=System.getenv("AR_MIGRATION_DATABASE_PREFIX");
        if(prefix==null || !prefix.matches("oms_ar_migration_[a-f0-9]{12}"))throw new IllegalArgumentException("需要专属随机库");
        p.add("spring.datasource.url",()->"jdbc:mysql://127.0.0.1:13306/"+prefix+"_cutover");
        p.add("spring.datasource.username",()->"oms_ar");p.add("spring.datasource.password",()->System.getenv("AR_TEST_PASSWORD"));
        p.add("spring.flyway.enabled",()->true);p.add("ar.cash.binding-scopes",()->"");
    }
    @Autowired JdbcTemplate db;
    @Autowired TransactionTemplate tx;
    @Autowired ReceivableService ar;
    @Autowired CashAccountService cash;
    @Autowired ReceiptService receipts;
    @Autowired SettlementService settlements;
    @Autowired CustomerBalanceService credit;
    @Autowired BalanceSettlementService spend;
    String key(){return UUID.randomUUID().toString();}
    BigDecimal n(String amount){return new BigDecimal(amount);}
    long line(String amount){
        var input=new ReceivableCreateRequest.Line("A",null,AmountType.PRINCIPAL,n(amount),null,null,null,null);
        long doc=ar.create(new ReceivableCreateRequest(key(),7L,1L,"CNY","TAX_ADJUSTMENT",LocalDate.now(),null,"test",List.of(input)));
        return db.queryForObject("SELECT id FROM oms_ar_line WHERE document_id=?",Long.class,doc);
    }
    long group(long line){return db.queryForObject("SELECT group_id FROM oms_ar_line WHERE id=?",Long.class,line);}
    CashCutover task(SortedMap<Long,Long> groups,SortedMap<Long,Long> receiptMap)throws Exception {
        return new CashCutover(db,new CashCutover.Manifest(1,7,"CNY","finance","历史迁移",groups,receiptMap));
    }
    @AfterEach void clear(){CurrentUser.clear();}
    @Test @DisplayName("跨户和漏映射拒绝；期初扣除占用并可原路反核销") void opening()throws Exception {
        CurrentUser.set(new CurrentUser.Identity("finance",Set.of(1L),Set.of("ar:create","ar:read","ar:receipt:create",
                "ar:cash:open","ar:balance:transfer","ar:settle","ar:reverse")));
        long aa=cash.open(new CashAccountOpenRequest(key(),1L,7L,"CNY","INVOICE","test"));
        long ab=cash.open(new CashAccountOpenRequest(key(),1L,7L,"CNY","NO_INVOICE","test"));
        long receipt=receipts.create(new ReceiptCreateRequest(key(),7L,1L,"CNY",n("50"),LocalDate.now(),"TEST",key(),key()));
        long target=group(line("100")),source=line("-20");
        long lot=credit.transfer(new BalanceTransferRequest(key(),1L,source,n("20"),0L,"test"));
        long legacy=db.queryForObject("SELECT account_id FROM oms_ar_balance_lot WHERE id=?",Long.class,lot);
        var cr=new BalanceSettlementPreviewRequest(1L,legacy,n("6"),List.of(target),AllocationStrategy.GROUP_TAX_FIRST);
        long bs=spend.confirm(new BalanceSettlementConfirmRequest(key(),cr,spend.preview(cr).fingerprint(),"test"));
        var cs=new SettlementPreviewRequest(1L,receipt,n("7"),List.of(target),AllocationStrategy.GROUP_TAX_FIRST);
        long rs=settlements.confirm(new SettlementConfirmRequest(key(),cs,settlements.preview(cs).fingerprint(),"test"));
        var gs=new TreeMap<Long,Long>();gs.put(target,aa);gs.put(group(source),aa);
        var rm=new TreeMap<Long,Long>();
        assertThatThrownBy(()->task(gs,rm).run(false,"")).hasMessageContaining("缺少");
        rm.put(receipt,ab);
        assertThatThrownBy(()->task(gs,rm).run(false,"")).hasMessageContaining("跨户");
        rm.put(receipt,aa);
        String fingerprint=CashCutover.JSON.readTree(task(gs,rm).run(false,"")).path("fingerprint").asText();
        var stale=task(gs,rm);
        assertThatThrownBy(()->tx.execute(st->stale.run(true,"0".repeat(64)))).hasMessageContaining("重新试算");
        var commit=task(gs,rm);String result=tx.execute(st->commit.run(true,fingerprint));
        assertThat(new BigDecimal(CashCutover.JSON.readTree(result).path("cashBook").get(""+aa).asText())).isEqualByComparingTo("43");
        var replay=task(gs,rm);
        assertThat(CashCutover.JSON.readTree(replay.run(true,fingerprint)).path("applied").asBoolean()).isTrue();
        long openingEvent=CashCutover.JSON.readTree(result).path("eventId").asLong();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM oms_ar_outbox WHERE event_id=? AND status='PENDING'",Long.class,openingEvent)).isEqualTo(1);
        assertThat(db.queryForObject("SELECT JSON_VALUE(after_snapshot,'$.bookAmount' RETURNING DECIMAL(24,8)) FROM oms_ar_lifecycle_change WHERE event_id=? AND object_type='CASH_ACCOUNT' AND object_id=?",BigDecimal.class,openingEvent,aa)).isEqualByComparingTo("43");
        long scoped=db.queryForObject("SELECT account_id FROM oms_ar_balance_lot WHERE id=?",Long.class,lot);
        assertThat(scoped).isNotEqualTo(legacy);
        assertThat(db.queryForObject("SELECT balance_amount FROM oms_ar_customer_account WHERE id=?",BigDecimal.class,scoped)).isEqualByComparingTo("14");
        settlements.reverse(new SettlementReverseRequest(key(),1L,rs,"undo cash"));
        settlements.reverse(new SettlementReverseRequest(key(),1L,bs,"undo credit"));
        assertThat(db.queryForObject("SELECT book_amount FROM oms_ar_cash_account WHERE id=?",BigDecimal.class,aa)).isEqualByComparingTo("50");
        assertThat(db.queryForObject("SELECT balance_amount FROM oms_ar_customer_account WHERE id=?",BigDecimal.class,scoped)).isEqualByComparingTo("20");
        assertThat(db.queryForObject("SELECT used_amount FROM oms_ar_balance_lot WHERE id=?",BigDecimal.class,lot)).isEqualByComparingTo("0");
    }
}
