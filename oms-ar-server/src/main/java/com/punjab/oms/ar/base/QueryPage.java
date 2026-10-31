package com.punjab.oms.ar.base;
import jakarta.validation.constraints.*;
/** 分页输入。 @author Punjab */
public class QueryPage {
 @Min(1) private int page=1;
 @Min(1) @Max(200) private int size=20;
 public int getPage(){return page;} public void setPage(int v){page=v;}
 public int getSize(){return size;} public void setSize(int v){size=v;}
}
