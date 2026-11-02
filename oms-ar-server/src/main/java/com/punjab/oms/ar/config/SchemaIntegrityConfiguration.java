package com.punjab.oms.ar.config;
import javax.sql.DataSource;import com.fasterxml.jackson.databind.ObjectMapper;import org.springframework.context.annotation.*;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
/** Flyway完成后、容器完成启动前校验结构；失败不自动修改数据库。 @author Punjab */
@Configuration(proxyBeanMethods=false) @ConditionalOnProperty(name="ar.database.verify-schema",havingValue="true",matchIfMissing=true)
public class SchemaIntegrityConfiguration {
 @Bean(initMethod="verify") @DependsOnDatabaseInitialization
 public SchemaContractVerifier schemaContractVerifier(DataSource source,ObjectMapper json){return new SchemaContractVerifier(source,json);}
}
