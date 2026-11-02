package com.punjab.oms.ar;
import org.junit.jupiter.api.*;import java.nio.file.*;import static org.assertj.core.api.Assertions.*;
/** 正式DDL与现有隔离业务夹具同源，生产不能静默跳过已存在表。 @author Punjab */
class MigrationResourcesTest {
 @Test @DisplayName("正式迁移与夹具结构一致且版本齐全")void resources()throws Exception{
  var production=Path.of("src/main/resources/db/migration");assertThat(production).isDirectory();
  try(var fixtures=Files.list(Path.of("../scratch/dbddl"))){var files=fixtures.filter(p->p.getFileName().toString().matches("V[0-9]+__.*\\.sql")).sorted().toList();assertThat(files).hasSize(12);for(var fixture:files){var target=production.resolve(fixture.getFileName());assertThat(target).exists();String actual=Files.readString(target);assertThat(actual).doesNotContain("IF NOT EXISTS");assertThat(actual).isEqualTo(Files.readString(fixture).replace("CREATE TABLE IF NOT EXISTS","CREATE TABLE"));}}
 }
}
