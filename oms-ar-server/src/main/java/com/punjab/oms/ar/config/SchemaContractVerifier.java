package com.punjab.oms.ar.config;
import javax.sql.DataSource;import com.fasterxml.jackson.databind.ObjectMapper;import java.util.*;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;
import org.flywaydb.core.api.MigrationVersion;
/** MySQL8.0.42启动结构契约；只读SHOW CREATE，排除运行时自增计数。 @author Punjab */
public class SchemaContractVerifier {
 private final DataSource source;private final ObjectMapper json;
 public SchemaContractVerifier(DataSource source,ObjectMapper json){this.source=source;this.json=json;}
 public void verify()throws Exception{
  Contract expected;try(var input=SchemaContractVerifier.class.getResourceAsStream("/db/schema-contract.json")){
   if(input==null)throw new IllegalStateException("数据库结构基准缺失");expected=json.readValue(input,Contract.class);
  }
  if(expected==null || expected.tableHashes()==null || expected.tableHashes().isEmpty() || expected.mysqlVersion()==null || expected.migrationVersion()==null || expected.tableHashes().values().stream().anyMatch(v->v==null || !v.matches("[a-f0-9]{64}")))throw new IllegalStateException("数据库结构基准无效");
  var actual=capture();
  if(!expected.mysqlVersion().equals(actual.mysqlVersion()))throw new IllegalStateException("MySQL版本与结构基准不一致");
  if(MigrationVersion.fromVersion(expected.migrationVersion()).compareTo(MigrationVersion.fromVersion(actual.migrationVersion()))!=0)throw new IllegalStateException("数据库迁移版本与应用结构基准不一致");
  var differences=new TreeSet<String>();differences.addAll(expected.tableHashes().keySet());differences.addAll(actual.tableHashes().keySet());differences.removeIf(name->Objects.equals(expected.tableHashes().get(name),actual.tableHashes().get(name)));
  if(!differences.isEmpty())throw new IllegalStateException("数据库结构与正式迁移基准不一致: "+String.join(",",differences));
 }
 public Contract capture()throws Exception{
  try(var connection=source.getConnection()){
   if(!"MySQL".equals(connection.getMetaData().getDatabaseProductName()))throw new IllegalStateException("结构基准仅支持MySQL");
   String mysqlVersion=connection.getMetaData().getDatabaseProductVersion().split("-")[0];String version;
   try(var statement=connection.createStatement();var rows=statement.executeQuery("SELECT version,success FROM flyway_schema_history WHERE type='SQL' ORDER BY installed_rank DESC LIMIT 1")){
    if(!rows.next() || !rows.getBoolean(2) || rows.getString(1)==null)throw new IllegalStateException("数据库迁移版本历史不完整");version=rows.getString(1);
   }
   var names=new ArrayList<String>();
   try(var statement=connection.createStatement();var rows=statement.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND LEFT(table_name,7)='oms_ar_' ORDER BY table_name")){while(rows.next())names.add(rows.getString(1));}
   var hashes=new TreeMap<String,String>();
   for(String name:names){
    if(!name.matches("oms_ar_[a-z0-9_]+"))throw new IllegalStateException("数据库业务表名称无效");
    try(var statement=connection.createStatement();var rows=statement.executeQuery("SHOW CREATE TABLE `"+name+"`")){
     if(!rows.next())throw new IllegalStateException("无法读取数据库结构: "+name);
     String ddl=rows.getString(2).replaceAll(" AUTO_INCREMENT=\\d+", "").replace("\r\n","\n");
     hashes.put(name,HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(ddl.getBytes(StandardCharsets.UTF_8))));
    }
   }
   return new Contract(mysqlVersion,version,Map.copyOf(hashes));
  }
 }
 /** 版本化结构摘要，不含业务数据、库名或凭据。 @author Punjab */
 public record Contract(String mysqlVersion,String migrationVersion,Map<String,String> tableHashes){}
}
