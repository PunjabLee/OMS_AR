package com.punjab.oms.ar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import static org.assertj.core.api.Assertions.assertThat;
/** @author Punjab */
class ConfigurationBoundaryTest {
 @Test @DisplayName("配置由环境注入且不导入远程配置中心")
 void localConfigurationRequiresExplicitCredentials() throws Exception {
  var resource = new ClassPathResource("application.yml");
  assertThat(resource.exists()).isTrue();
  var sources = new YamlPropertySourceLoader().load("ar", resource);
  var config = sources.getFirst();
  assertThat(config.getProperty("spring.datasource.password")).isEqualTo("${DB_PASSWORD}");
  assertThat(config.getProperty("spring.datasource.url")).isEqualTo("${DB_URL}");
  assertThat(config.getProperty("spring.config.import")).isNull();
 }
}
