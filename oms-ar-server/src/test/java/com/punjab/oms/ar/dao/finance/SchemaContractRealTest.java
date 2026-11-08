package com.punjab.oms.ar.dao.finance;
import com.punjab.oms.ar.config.SchemaContractVerifier;import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;import org.springframework.jdbc.datasource.DriverManagerDataSource;import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.condition.EnabledIfSystemProperty;import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;
/** scripts/test-mysql-migration：仅随机临时库内修改结构，验证真实漂移。 @author Punjab */
@EnabledIfSystemProperty(named="ar.schema-contract.real",matches="true") @EnabledIfSystemProperty(named="ar.schema-contract.real.commit",matches="true")
class SchemaContractRealTest {
 SchemaContractVerifier verifier;JdbcTemplate jdbc;Flyway flyway;
 @BeforeEach void setup(TestInfo info){String prefix=System.getenv("AR_MIGRATION_DATABASE_PREFIX");assertThat(prefix).matches("oms_ar_migration_[a-f0-9]{12}");String url="jdbc:mysql://127.0.0.1:13306/"+prefix+"_contract_"+info.getTestMethod().orElseThrow().getName();var source=new DriverManagerDataSource(url,"oms_ar",System.getenv("AR_TEST_PASSWORD"));flyway=Flyway.configure().dataSource(source).locations("classpath:db/migration").cleanDisabled(true).baselineOnMigrate(false).load();assertThat(flyway.migrate().migrationsExecuted).isEqualTo(18);jdbc=new JdbcTemplate(source);verifier=new SchemaContractVerifier(source,new ObjectMapper());}
 @Test @DisplayName("正式迁移匹配结构基准，自增计数变化不误报")void baseline()throws Exception{
  if(Boolean.getBoolean("ar.schema.contract.export")){var contract=verifier.capture();assertThat(contract.tableHashes()).isNotEmpty();Files.writeString(Path.of("src/main/resources/db/schema-contract.json"),new ObjectMapper().writerWithDefaultPrettyPrinter().with(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).writeValueAsString(contract)+"\n");return;}
  verifier.verify();jdbc.execute("ALTER TABLE oms_ar_document AUTO_INCREMENT=500");verifier.verify();
 }
 @Test @DisplayName("列定义漂移即使Flyway文件校验通过也拒绝")void column(){jdbc.execute("ALTER TABLE oms_ar_document MODIFY doc_no VARCHAR(65) NOT NULL");flyway.validate();assertThatThrownBy(verifier::verify).isInstanceOf(IllegalStateException.class).hasMessageContaining("oms_ar_document");}
 @Test @DisplayName("缺失索引拒绝")void index(){jdbc.execute("ALTER TABLE oms_ar_line_source DROP INDEX ix_source_document");assertThatThrownBy(verifier::verify).isInstanceOf(IllegalStateException.class).hasMessageContaining("oms_ar_line_source");}
 @Test @DisplayName("外键缺失拒绝")void foreignkey(){jdbc.execute("ALTER TABLE oms_ar_line_source DROP FOREIGN KEY fk_source_original");assertThatThrownBy(verifier::verify).isInstanceOf(IllegalStateException.class).hasMessageContaining("oms_ar_line_source");}
 @Test @DisplayName("CHECK仍存在但不执行时拒绝")void check(){jdbc.execute("ALTER TABLE oms_ar_line ALTER CHECK ck_line_balance NOT ENFORCED");assertThatThrownBy(verifier::verify).isInstanceOf(IllegalStateException.class).hasMessageContaining("oms_ar_line");}
 @Test @DisplayName("业务表缺失或新增未知业务表拒绝")void table()throws Exception{jdbc.execute("CREATE TABLE oms_ar_unexpected(id BIGINT)");assertThatThrownBy(verifier::verify).isInstanceOf(IllegalStateException.class);jdbc.execute("DROP TABLE oms_ar_unexpected");verifier.verify();jdbc.execute("DROP TABLE oms_ar_outbox_retry");assertThatThrownBy(verifier::verify).isInstanceOf(IllegalStateException.class).hasMessageContaining("oms_ar_outbox_retry");}
 @Test @DisplayName("数据库迁移版本与当前应用不一致时拒绝")void version(){jdbc.update("UPDATE flyway_schema_history SET version='999' WHERE version='018'");assertThatThrownBy(verifier::verify).isInstanceOf(IllegalStateException.class).hasMessageContaining("版本");}
}
