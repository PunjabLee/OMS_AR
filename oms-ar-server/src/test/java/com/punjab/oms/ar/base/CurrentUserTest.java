package com.punjab.oms.ar.base;
import org.junit.jupiter.api.*;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
/** 审计与主体权限上下文。 @author Punjab */
class CurrentUserTest {
 @AfterEach void clear(){CurrentUser.clear();}
 @Test @DisplayName("没有操作人时拒绝业务操作") void missing(){assertThatThrownBy(()->CurrentUser.require(1L,"ar:create")).isInstanceOf(ServiceException.class);}
 @Test @DisplayName("主体与动作双重授权") void permissions(){
  CurrentUser.set(new CurrentUser.Identity("finance",Set.of(1L),Set.of("ar:create")));
  assertThat(CurrentUser.require(1L,"ar:create")).isEqualTo("finance");
  assertThatThrownBy(()->CurrentUser.require(2L,"ar:create")).isInstanceOf(ServiceException.class);
  assertThatThrownBy(()->CurrentUser.require(1L,"ar:void")).isInstanceOf(ServiceException.class);
 }
 @Test @DisplayName("上下文清理后不可复用身份") void cleared(){
  CurrentUser.set(new CurrentUser.Identity("finance",Set.of(1L),Set.of("ar:create")));
  CurrentUser.clear();assertThatThrownBy(()->CurrentUser.require(1L,"ar:create")).isInstanceOf(ServiceException.class);
 }
}
