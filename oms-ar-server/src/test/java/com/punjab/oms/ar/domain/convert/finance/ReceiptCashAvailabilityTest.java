package com.punjab.oms.ar.domain.convert.finance;

import com.punjab.oms.ar.domain.entity.finance.ArReceipt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mapstruct.factory.Mappers;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

/** 核对收款详情中已核销、已提现和冻结额的可用口径。 @author Punjab */
class ReceiptCashAvailabilityTest {
    @Test @DisplayName("收款100核销60冻结10提现5，实际可用25")
    void availableAfterAllOccupations() {
        var receipt=new ArReceipt();
        receipt.setAmount(new BigDecimal("100"));
        receipt.setAllocatedAmount(new BigDecimal("60"));
        var view=Mappers.getMapper(ReceiptConverter.class).toResponse(receipt,8L,BigDecimal.TEN,new BigDecimal("5"));
        assertThat(view.availableAmount()).isEqualByComparingTo("25");
        assertThat(view.cashAccountId()).isEqualTo(8L);
        assertThat(view.frozenAmount()).isEqualByComparingTo("10");
        assertThat(view.withdrawnAmount()).isEqualByComparingTo("5");
    }
}
