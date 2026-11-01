package com.punjab.oms.ar.config;
import org.springframework.stereotype.Component;import org.springframework.boot.context.properties.ConfigurationProperties;
/** 汇率差异开票需显式业务配置，默认禁用。 @author Punjab */
@Component @ConfigurationProperties(prefix="ar.invoice")
public class InvoiceProperties {
 private boolean exchangeDifferenceEnabled;
 public boolean isExchangeDifferenceEnabled(){return exchangeDifferenceEnabled;}
 public void setExchangeDifferenceEnabled(boolean value){exchangeDifferenceEnabled=value;}
}
