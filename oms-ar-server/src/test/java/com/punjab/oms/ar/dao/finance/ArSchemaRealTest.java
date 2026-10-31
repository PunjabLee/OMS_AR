package com.punjab.oms.ar.dao.finance;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.sql.*;
import java.nio.file.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

/**
 * 独立本地测试库DDL与约束；运行 scripts/test-mysql-schema。
 * 仅接受数据库名oms_ar_test，默认门禁，不连接业务库。
 * @author Punjab
 */
@EnabledIfSystemProperty(named="ar.schema.real", matches="true")
@EnabledIfSystemProperty(named="ar.schema.real.commit", matches="true")
class ArSchemaRealTest {
    private Connection db;
    @BeforeEach void connect() throws Exception {
        db=DriverManager.getConnection("jdbc:mysql://127.0.0.1:13306/oms_ar_test", "oms_ar", System.getenv("AR_TEST_PASSWORD"));
        assertThat(db.getCatalog()).isEqualTo("oms_ar_test");
        Path script=Path.of("../scratch/dbddl/V001__ar_core.sql");
        assertThat(script).exists();
        for(String sql:Files.readString(script).split(";")) if(!sql.isBlank()) {
            try(var s=db.createStatement()) {s.execute(sql);}
        }
        Path audit=Path.of("../scratch/dbddl/V002__ar_audit.sql");
        if(Files.exists(audit)) for(String sql:Files.readString(audit).split(";")) if(!sql.isBlank()) {
            try(var statement=db.createStatement()){statement.execute(sql);}
        }
        db.setAutoCommit(false);
    }
    @AfterEach void cleanup() throws Exception { if(db!=null){if(!db.getAutoCommit())db.rollback();db.close();} }
    private long insert(String sql) throws SQLException {
        try(var s=db.createStatement()){s.executeUpdate(sql,Statement.RETURN_GENERATED_KEYS);try(var k=s.getGeneratedKeys()){k.next();return k.getLong(1);}}
    }
    private long group(long customer) throws SQLException {
        return insert("INSERT INTO oms_ar_business_group(group_no,customer_id,legal_entity_id,currency,business_date,created_by,updated_by) VALUES ('"+UUID.randomUUID()+"',"+customer+",1,'CNY','2026-10-31','test','test')");
    }
    private long document(long customer) throws SQLException {
        return insert("INSERT INTO oms_ar_document(doc_no,customer_id,legal_entity_id,currency,business_type,generation_mode,business_date,created_by,updated_by) VALUES ('"+UUID.randomUUID()+"',"+customer+",1,'CNY','SHIPMENT','UPSTREAM','2026-10-31','test','test')");
    }
    private long line(long doc,long group,String original,String settled,String transfer,String open) throws SQLException {
        return insert("INSERT INTO oms_ar_line(document_id,group_id,customer_id,legal_entity_id,currency,line_no,amount_type,receivable_amount,settled_amount,transferred_amount,open_amount,created_by,updated_by) VALUES ("+doc+","+group+",1,1,'CNY',1,'PRINCIPAL',"+original+","+settled+","+transfer+","+open+",'test','test')");
    }
    @Test @DisplayName("未提供来源行ID仍不允许重复生成") void sourceWithoutLineIdIsUnique() throws Exception {
        long id=line(document(1),group(1),"100","0","0","100");
        String key=UUID.randomUUID().toString();
        String sql="INSERT INTO oms_ar_line_source(line_id,source_system,source_type,source_no,source_id,source_event_id,source_output_key,created_by,updated_by) VALUES ("+id+",'OMS','SHIPMENT','NO','ID','"+key+"','price','test','test')";
        insert(sql);
        long second=line(document(1),group(1),"100","0","0","100");
        String duplicate=sql.replace("VALUES ("+id+",", "VALUES ("+second+",");
        assertThatThrownBy(()->insert(duplicate)).isInstanceOf(SQLException.class).satisfies(e->assertThat(((SQLException)e).getSQLState()).isEqualTo("23000"));
    }
    @Test @DisplayName("金额越零、方向不符与公式失衡均拒绝") void amountConstraints() throws Exception {
        long d=document(1),g=group(1);
        assertThatThrownBy(()->line(d,g,"100","110","0","-10")).isInstanceOf(SQLException.class).satisfies(e->assertThat(((SQLException)e).getErrorCode()).isEqualTo(3819));
        assertThatThrownBy(()->line(d,g,"100","-10","0","110")).isInstanceOf(SQLException.class).satisfies(e->assertThat(((SQLException)e).getErrorCode()).isEqualTo(3819));
        assertThatThrownBy(()->line(d,g,"100","0","0","90")).isInstanceOf(SQLException.class).satisfies(e->assertThat(((SQLException)e).getErrorCode()).isEqualTo(3819));
        assertThat(line(d,g,"-100","-60","-30","-10")).isPositive();
    }
    @Test @DisplayName("禁止跨客户业务组关联") void customerIsolation() throws Exception {
        assertThatThrownBy(()->line(document(1),group(2),"100","0","0","100")).isInstanceOf(SQLException.class).satisfies(e->assertThat(((SQLException)e).getErrorCode()).isEqualTo(1452));
    }
    @Test @DisplayName("开票金额与结算余额独立") void invoicingIndependent() throws Exception {
        long id=line(document(1),group(1),"100","0","0","100");
        try(var s=db.createStatement()) {
            assertThat(s.executeUpdate("UPDATE oms_ar_line SET invoiced_amount=100 WHERE id="+id)).isEqualTo(1);
            try(var r=s.executeQuery("SELECT open_amount FROM oms_ar_line WHERE id="+id)){r.next();assertThat(r.getBigDecimal(1)).isEqualByComparingTo("100");}
        }
    }

    @Test @DisplayName("相同调用方的请求键唯一，其他调用方隔离") void requestUniqueness() throws Exception {
        String key=UUID.randomUUID().toString();
        String sql="INSERT INTO oms_ar_operation_request(legal_entity_id,caller_id,operation_type,request_key,payload_hash,created_by,updated_by) VALUES (1,'OMS','CREATE','"+key+"','"+"a".repeat(64)+"','test','test')";
        assertThat(insert(sql)).isPositive();
        assertThatThrownBy(()->insert(sql)).isInstanceOf(SQLException.class).satisfies(e->assertThat(((SQLException)e).getErrorCode()).isEqualTo(1062));
        assertThat(insert(sql.replace("'OMS'","'OTHER'"))).isPositive();
    }
    @Test @DisplayName("初始化快照和事件可原样关联读取") void initializationSnapshot() throws Exception {
        long event=insert("INSERT INTO oms_ar_lifecycle_event(event_type,occurred_at,actor,correlation_id,created_by,updated_by) VALUES ('CREATED',NOW(6),'test','"+UUID.randomUUID()+"','test','test')");
        insert("INSERT INTO oms_ar_lifecycle_change(event_id,object_type,object_id,before_snapshot,delta_snapshot,after_snapshot,before_version,after_version,created_by,updated_by) VALUES ("+event+",'LINE',99,NULL,JSON_OBJECT('openAmount','13.00'),JSON_OBJECT('openAmount','13.00','taxRate','0.13'),NULL,0,'test','test')");
        try(var statement=db.createStatement();var rows=statement.executeQuery("SELECT JSON_UNQUOTE(JSON_EXTRACT(after_snapshot,'$.taxRate')) FROM oms_ar_lifecycle_change WHERE event_id="+event)) {
            assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo("0.13");
        }
    }
}
