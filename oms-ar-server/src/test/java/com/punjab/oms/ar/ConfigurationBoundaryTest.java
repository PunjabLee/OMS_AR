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
  assertThat(config.getProperty("spring.flyway.enabled")).isEqualTo(true);
  assertThat(config.getProperty("spring.flyway.baseline-on-migrate")).isEqualTo(false);
  assertThat(config.getProperty("spring.flyway.clean-disabled")).isEqualTo(true);
  assertThat(config.getProperty("spring.flyway.validate-on-migrate")).isEqualTo(true);
  assertThat(config.getProperty("spring.flyway.locations")).isEqualTo("classpath:db/migration");
 }
}
