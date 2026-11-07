package com.punjab.oms.ar;
import org.junit.jupiter.api.*;import java.nio.file.*;import static org.assertj.core.api.Assertions.*;
/** 正式DDL与现有隔离业务夹具同源，生产不能静默跳过已存在表。 @author Punjab */
class MigrationResourcesTest {
 @Test @DisplayName("正式迁移与夹具结构一致且版本齐全")void resources()throws Exception{
  var production=Path.of("src/main/resources/db/migration");assertThat(production).isDirectory();
  try(var fixtures=Files.list(Path.of("../scratch/dbddl"))){var files=fixtures.filter(p->p.getFileName().toString().matches("V[0-9]+__.*\\.sql")).sorted().toList();assertThat(files).hasSize(14);for(var fixture:files){var target=production.resolve(fixture.getFileName());assertThat(target).exists();String actual=Files.readString(target);assertThat(actual).doesNotContain("IF NOT EXISTS");assertThat(actual).isEqualTo(Files.readString(fixture).replace("CREATE TABLE IF NOT EXISTS","CREATE TABLE"));}}
 }
 @Test @DisplayName("结构摘要覆盖每张正式业务表且启动校验有迁移依赖")void contract()throws Exception{
  var contract=new com.fasterxml.jackson.databind.ObjectMapper().readTree(Files.readString(Path.of("src/main/resources/db/schema-contract.json")));
  assertThat(contract.path("mysqlVersion").asText()).isEqualTo("8.0.42");assertThat(contract.path("migrationVersion").asText()).isEqualTo("014");
  var names=new java.util.TreeSet<String>();try(var files=Files.list(Path.of("src/main/resources/db/migration"))){for(var file:files.toList()){var matcher=java.util.regex.Pattern.compile("CREATE TABLE (oms_ar_[a-z0-9_]+)").matcher(Files.readString(file));while(matcher.find())names.add(matcher.group(1));}}
  var hashes=contract.path("tableHashes");var expected=new java.util.TreeSet<String>();hashes.fieldNames().forEachRemaining(expected::add);assertThat(expected).isEqualTo(names);hashes.forEach(hash->assertThat(hash.asText()).matches("[a-f0-9]{64}"));
  var method=com.punjab.oms.ar.config.SchemaIntegrityConfiguration.class.getMethod("schemaContractVerifier",javax.sql.DataSource.class,com.fasterxml.jackson.databind.ObjectMapper.class);
  assertThat(method.isAnnotationPresent(org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization.class)).isTrue();
  assertThat(method.getAnnotation(org.springframework.context.annotation.Bean.class).initMethod()).isEqualTo("verify");
 }

}
