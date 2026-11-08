package com.punjab.oms.ar.tools;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.punjab.oms.ar.mapper.finance.ReconciliationSql;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import static java.nio.charset.StandardCharsets.UTF_8;

/** 停写窗口内试算或一次性切换已核准的存量客户范围。 @author Punjab */
public final class CashCutover {
    static final ObjectMapper JSON=new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    public record Manifest(long legalEntityId,long customerId,String currency,String actor,String reason,
                           SortedMap<Long,Long> groups,SortedMap<Long,Long> receipts) {}
    final JdbcTemplate db;
    final Manifest m;
    final MessageDigest digest;
    final SortedMap<Long,BigDecimal> cash=new TreeMap<>(), credit=new TreeMap<>();
    final SortedMap<Long,Long> lotOwners=new TreeMap<>(), settlementOwners=new TreeMap<>();
    final Set<Long> boundGroups=new HashSet<>(), boundReceipts=new HashSet<>();
    final Map<Long,String> modes=new HashMap<>();
    final Map<Long,Long> creditAccounts=new HashMap<>();
    final Set<Long> oldAccounts=new TreeSet<>();
    static final String SCOPE=" legal_entity_id=? AND customer_id=? AND currency=? AND delete_status=0 ";
    final Object[] scope;

    CashCutover(JdbcTemplate db,Manifest m) throws Exception {
        this.db=db; this.m=m; scope=new Object[]{m.legalEntityId,m.customerId,m.currency};
        digest=MessageDigest.getInstance("SHA-256");
        require(m.legalEntityId>0 && m.customerId>0 && m.currency!=null && m.currency.matches("[A-Z]{3}")
                && m.actor!=null && !m.actor.isBlank() && m.actor.length()<=100 && m.reason!=null && !m.reason.isBlank()
                && m.groups!=null && m.receipts!=null,"清单须包含范围、操作人、原因、groups 与 receipts");
    }

    public static void main(String[] args) throws Exception {
        boolean apply=args.length==4 && args[0].equals("apply") && args[2].matches("[0-9a-f]{64}") && args[3].equals("--writes-drained");
        require(apply || (args.length==2 && args[0].equals("preview")),"preview manifest.json | apply manifest.json fingerprint --writes-drained");
        var manifest=JSON.readValue(Files.readAllBytes(Path.of(args[1])),Manifest.class);
        var ds=new DriverManagerDataSource(Objects.requireNonNull(System.getenv("DB_URL")),
                Objects.requireNonNull(System.getenv("DB_USERNAME")),Objects.requireNonNull(System.getenv("DB_PASSWORD")));
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        tx.setIsolationLevel(apply?java.sql.Connection.TRANSACTION_SERIALIZABLE:java.sql.Connection.TRANSACTION_REPEATABLE_READ);
        tx.setReadOnly(!apply);
        var task=new CashCutover(new JdbcTemplate(ds),manifest);
        String result=tx.execute(status->task.run(apply,apply?args[2]:""));
        System.out.println(result);
    }
    static void require(boolean ok,String message){if(!ok) throw new IllegalArgumentException(message);}
    static String json(Object o){try{return JSON.writeValueAsString(o);}catch(Exception e){throw new IllegalArgumentException(e);}}
    static String hash(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    static long id(Map<String,Object> row,String field){return ((Number)row.get(field)).longValue();}
    static BigDecimal money(Map<String,Object> row,String field){return (BigDecimal)row.get(field);}
    List<Map<String,Object>> read(String sql){
        var rows=db.queryForList(sql,scope);
        digest.update(json(rows).getBytes(UTF_8));
        return rows;
    }
    List<Map<String,Object>> scoped(String table,String fields){
        return read("SELECT "+fields+" FROM oms_ar_"+table+" WHERE "+SCOPE+" ORDER BY id");
    }
    List<Map<String,Object>> related(String table,String parent,String key,String fields){
        return read("SELECT "+fields+" FROM oms_ar_"+table+" a JOIN (SELECT id FROM oms_ar_"+parent+" WHERE "+SCOPE+
                ") p ON p.id=a."+key+" WHERE a.delete_status=0 ORDER BY a.id");
    }
    String run(boolean apply,String expected){
        String manifest=json(m), manifestHash=hash(manifest);
        var existing=db.queryForList("SELECT manifest_hash,fingerprint,opening_report FROM oms_ar_cash_cutover WHERE legal_entity_id=? AND customer_id=? AND currency=?",scope);
        if(!existing.isEmpty()){
            var saved=existing.getFirst();
            require(manifestHash.equals(saved.get("manifest_hash")) && (!apply || expected.equals(saved.get("fingerprint"))),"该范围已用另一清单或指纹切换");
            return saved.get("opening_report").toString();
        }
        digest.update(manifest.getBytes(UTF_8));
        var report=plan();
        String fingerprint=HexFormat.of().formatHex(digest.digest());
        report.put("fingerprint",fingerprint);
        if(!apply)return json(report);
        require(fingerprint.equals(expected),"数据或清单已变化，请重新试算");
        persist();
        report.put("applied",true);
        db.update("INSERT INTO oms_ar_cash_cutover(legal_entity_id,customer_id,currency,manifest_hash,fingerprint,manifest,opening_report,created_by) VALUES(?,?,?,?,?,?,?,?)",
                m.legalEntityId,m.customerId,m.currency,manifestHash,fingerprint,manifest,json(report),m.actor);
        return json(report);
    }
    Map<String,Object> plan(){
        var differences=ReconciliationSql.count().replace("#{legalEntityId}",Long.toString(m.legalEntityId))
                .replace("#{customerId}",Long.toString(m.customerId)).replace("#{currency}","'"+m.currency+"'");
        require(db.queryForObject(differences,Long.class)==0,"历史对账存在差异，请按范围查询对账明细并修复");
        var accounts=scoped("cash_account","id,invoice_mode,status,book_amount,frozen_amount,version");
        var existingBook=new HashMap<Long,BigDecimal>();var existingFrozen=new HashMap<Long,BigDecimal>();
        for(var a:accounts){
            long key=id(a,"id");modes.put(key,a.get("invoice_mode").toString());
            require("ACTIVE".equals(a.get("status")),"切换账户必须可用 "+key);
            cash.put(key,BigDecimal.ZERO);credit.put(key,BigDecimal.ZERO);
        }
        require(modes.containsValue("INVOICE") && modes.containsValue("NO_INVOICE"),"请先开设两种用途的现金账户");
        validateKeys(scoped("business_group","id,version"),m.groups,"业务组");
        var receipts=scoped("receipt","id,amount,allocated_amount,version");
        validateKeys(receipts,m.receipts,"收款");
        for(var b:scoped("cash_group_binding","id,group_id,cash_account_id")){
            long group=id(b,"group_id");require(Objects.equals(m.groups.get(group),id(b,"cash_account_id")),"既有组映射冲突 "+group);boundGroups.add(group);
        }
        var reversals=new HashMap<Long,BigDecimal>();
        for(var reversal:related("cash_receipt_reversal","cash_receipt_lot","lot_id","a.id,a.lot_id,a.amount")) reversals.put(id(reversal,"lot_id"),money(reversal,"amount"));
        var receiptLots=new HashMap<Long,Map<String,Object>>();
        for(var b:scoped("cash_receipt_lot","id,receipt_id,cash_account_id,frozen_amount,withdrawn_amount")){
            long receipt=id(b,"receipt_id");require(Objects.equals(m.receipts.get(receipt),id(b,"cash_account_id")),"既有收款映射冲突 "+receipt);
            receiptLots.put(receipt,b);boundReceipts.add(receipt);
        }
        for(var r:receipts){
            long receipt=id(r,"id"), owner=m.receipts.get(receipt);
            var remainder=money(r,"amount").subtract(money(r,"allocated_amount"));
            var lot=receiptLots.get(receipt);
            if(lot!=null){
                remainder=remainder.subtract(money(lot,"withdrawn_amount")).subtract(reversals.getOrDefault(id(lot,"id"),BigDecimal.ZERO));
                require(remainder.compareTo(money(lot,"frozen_amount"))>=0,"批次资金不足 "+receipt);
                existingBook.merge(owner,remainder,BigDecimal::add);
                existingFrozen.merge(owner,money(lot,"frozen_amount"),BigDecimal::add);
            }
            cash.merge(owner,remainder,BigDecimal::add);
        }
        for(var a:accounts){
            long key=id(a,"id");
            require(money(a,"book_amount").compareTo(existingBook.getOrDefault(key,BigDecimal.ZERO))==0
                    && money(a,"frozen_amount").compareTo(existingFrozen.getOrDefault(key,BigDecimal.ZERO))==0,"既有资金与批次不符 "+key);
        }
        var lineOwners=new HashMap<Long,Long>();
        for(var l:scoped("line","id,group_id,amount_type,invoiced_amount,version")){
            long line=id(l,"id"), owner=m.groups.get(id(l,"group_id"));lineOwners.put(line,owner);
            if("OUTPUT_TAX".equals(l.get("amount_type")) || money(l,"invoiced_amount").signum()!=0)invoice(owner,line);
        }
        for(var a:related("invoice_allocation","invoice","invoice_id","a.id,a.receivable_line_id"))
            invoice(lineOwners.get(id(a,"receivable_line_id")),id(a,"id"));
        for(var a:related("adjustment_link","line","line_id","a.id,a.line_id,a.related_group_id,a.original_line_id")){
            var owner=lineOwners.get(id(a,"line_id"));
            require(owner.equals(m.groups.get(id(a,"related_group_id"))) && (a.get("original_line_id")==null ||
                    owner.equals(lineOwners.get(id(a,"original_line_id")))),"调整关系跨户 "+a.get("id"));
        }
        creditPlan(lineOwners);
        var report=new TreeMap<String,Object>();report.put("cashBook",cash);report.put("creditBalance",credit);
        report.put("groupCount",m.groups.size());report.put("receiptCount",m.receipts.size());report.put("applied",false);return report;
    }
    void invoice(Long owner,long id){require(owner!=null && "INVOICE".equals(modes.get(owner)),"税额或历史发票映射至不开票账户 "+id);}
    void validateKeys(List<Map<String,Object>> rows,Map<Long,Long> mapping,String kind){
        var ids=new TreeSet<Long>();
        for(var row:rows){long key=id(row,"id");ids.add(key);require(modes.containsKey(mapping.get(key)),kind+"缺少同范围账户映射 "+key);}
        require(ids.equals(mapping.keySet()),kind+"清单包含多余或跨范围记录");
    }
    void creditPlan(Map<Long,Long> lines){
        var originals=new HashMap<Long,Long>();
        for(var a:scoped("customer_account","id,cash_account_id,balance_amount,version")){
            long key=id(a,"id");
            if(a.get("cash_account_id")==null)oldAccounts.add(key);
            else {long owner=id(a,"cash_account_id");creditAccounts.put(owner,key);originals.put(key,owner);}
        }
        for(var l:scoped("balance_lot","id,account_id,source_line_id,amount,used_amount,status,version")){
            long key=id(l,"id"),old=id(l,"account_id");Long owner=lines.get(id(l,"source_line_id"));
            require(owner!=null && (oldAccounts.contains(old)||owner.equals(originals.get(old))),"信用批次来源映射冲突 "+key);
            lotOwners.put(key,owner);
            if("ACTIVE".equals(l.get("status")))credit.merge(owner,money(l,"amount").subtract(money(l,"used_amount")),BigDecimal::add);
        }
        var settlements=new HashMap<Long,Map<String,Object>>();
        for(var s:scoped("settlement","id,receipt_id,settlement_type,status,amount,version"))settlements.put(id(s,"id"),s);
        for(var a:related("settlement_allocation","settlement","settlement_id","a.id,a.settlement_id,a.positive_line_id,a.negative_line_id")){
            long key=id(a,"settlement_id");var s=settlements.get(key);Long owner=lines.get(id(a,"positive_line_id"));
            require(owner!=null,"核销目标缺失 "+key);
            switch(s.get("settlement_type").toString()){
                case "RECEIPT" -> require(owner.equals(m.receipts.get(id(s,"receipt_id"))),"收款与核销目标跨户 "+key);
                case "OFFSET" -> require(owner.equals(lines.get(id(a,"negative_line_id"))),"负数抵销跨户 "+key);
                case "BALANCE" -> {var prior=settlementOwners.putIfAbsent(key,owner);require(prior==null || prior.equals(owner),"同一历史信用核销涉及多个账户 "+key);}
                default -> throw new IllegalArgumentException("未知核销类型 "+key);
            }
        }
        for(var u:related("balance_usage","settlement","settlement_id","a.id,a.settlement_id,a.lot_id")){
            var owner=lotOwners.get(id(u,"lot_id"));require(owner!=null && owner.equals(settlementOwners.get(id(u,"settlement_id"))),"信用批次与抵扣目标跨户 "+u.get("id"));
        }
    }
    void persist(){
        for(var g:m.groups.entrySet())if(!boundGroups.contains(g.getKey()))bind("cash_group_binding","group_id",g);
        for(var r:m.receipts.entrySet())if(!boundReceipts.contains(r.getKey()))bind("cash_receipt_lot","receipt_id",r);
        for(var a:cash.entrySet())db.update("UPDATE oms_ar_cash_account SET book_amount=?,version=version+1,updated_by=?,updated_time=NOW(6) WHERE id=?",
                a.getValue(),m.actor,a.getKey());
        for(long owner:new TreeSet<>(lotOwners.values())){
            if(!creditAccounts.containsKey(owner)){
                db.update("INSERT INTO oms_ar_customer_account(customer_id,legal_entity_id,currency,cash_account_id,created_by,updated_by,created_time,updated_time) VALUES(?,?,?,?,?,?,NOW(6),NOW(6))",
                        m.customerId,m.legalEntityId,m.currency,owner,m.actor,m.actor);
                creditAccounts.put(owner,db.queryForObject("SELECT LAST_INSERT_ID()",Long.class));
            }
        }
        for(var l:lotOwners.entrySet())db.update("UPDATE oms_ar_balance_lot SET account_id=?,version=version+1,updated_by=?,updated_time=NOW(6) WHERE id=? AND account_id<>?",
                creditAccounts.get(l.getValue()),m.actor,l.getKey(),creditAccounts.get(l.getValue()));
        for(var s:settlementOwners.entrySet())db.update("UPDATE oms_ar_balance_settlement SET account_id=?,updated_by=?,updated_time=NOW(6) WHERE settlement_id=? AND delete_status=0",
                creditAccounts.get(s.getValue()),m.actor,s.getKey());
        for(var a:creditAccounts.entrySet())creditAmount(a.getValue(),credit.get(a.getKey()));
        for(long old:oldAccounts)creditAmount(old,BigDecimal.ZERO);
    }
    void creditAmount(long id,BigDecimal amount){db.update("UPDATE oms_ar_customer_account SET balance_amount=?,version=version+1,updated_by=?,updated_time=NOW(6) WHERE id=?",amount,m.actor,id);}
    void bind(String table,String field,Map.Entry<Long,Long> e){
        db.update("INSERT INTO oms_ar_"+table+"("+field+",cash_account_id,customer_id,legal_entity_id,currency,created_time,updated_time,created_by,updated_by) VALUES(?,?,?,?,?,NOW(6),NOW(6),?,?)",
                e.getKey(),e.getValue(),m.customerId,m.legalEntityId,m.currency,m.actor,m.actor);
    }
}
