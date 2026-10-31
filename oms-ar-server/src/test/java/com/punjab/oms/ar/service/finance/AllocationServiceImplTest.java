package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.service.finance.impl.AllocationServiceImpl;
import com.punjab.oms.ar.domain.dto.finance.AllocationPreviewRequest;
import com.punjab.oms.ar.domain.dto.finance.AllocationPreviewRequest.*;
import com.punjab.oms.ar.enums.*;
import com.punjab.oms.ar.base.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
/** 核销预览金额契约。 @author Punjab */
class AllocationServiceImplTest {
    private final AllocationService service = new AllocationServiceImpl();
    private BigDecimal n(String value) { return new BigDecimal(value); }
    private Line line(long id, AmountType type, String amount) { return new Line(id,type,n(amount)); }
    private Group group(long id, Line... lines) { return new Group(id,List.of(lines)); }
    private AllocationPreviewRequest request(String available, Group... groups) {
        return new AllocationPreviewRequest(n(available),2,AllocationStrategy.GROUP_TAX_FIRST,List.of(groups));
    }
    private Group a() { return group(1,line(1,AmountType.PRINCIPAL,"100"),line(2,AmountType.OUTPUT_TAX,"13")); }
    @Test @DisplayName("按组而非整单先税后价") void allocatesByGroup() {
        var result=service.preview(request("120",a(),group(2,line(3,AmountType.PRINCIPAL,"200"),line(4,AmountType.OUTPUT_TAX,"26"))));
        assertThat(result.allocations()).extracting(x->x.lineId()).containsExactly(2L,1L,4L);
        assertThat(result.allocations()).extracting(x->x.amount()).usingComparatorForType(BigDecimal::compareTo,BigDecimal.class).containsExactly(n("13"),n("100"),n("7"));
        assertThat(result.unallocatedAmount()).isEqualByComparingTo("0");
    }
    @Test @DisplayName("不足税额时不核销价款") void partialTax() {
        var result=service.preview(request("10",a()));
        assertThat(result.allocations()).hasSize(1);
        assertThat(result.allocations().getFirst().lineId()).isEqualTo(2L);
        assertThat(result.allocations().getFirst().amount()).isEqualByComparingTo("10");
    }
    @Test @DisplayName("超额保留且不改变输入余额") void excessRemains() {
        var original=a();var result=service.preview(request("120",original));
        assertThat(result.unallocatedAmount()).isEqualByComparingTo("7");
        assertThat(original.lines().getFirst().openAmount()).isEqualByComparingTo("100");
    }
    @Test @DisplayName("独立信用金额按税后价分配且不改变类型") void independentCreditAllocation() {
        var result=service.preview(request("20",group(1,line(1,AmountType.PRINCIPAL,"100"),line(2,AmountType.OUTPUT_TAX,"5"))));
        assertThat(result.allocations().get(0).amount()).isEqualByComparingTo("5");
        assertThat(result.allocations().get(1).amount()).isEqualByComparingTo("15");
        assertThat(result.allocations().get(1).amountType()).isEqualTo(AmountType.PRINCIPAL);
    }
    @Test @DisplayName("零余额跳过且独立税额可结清") void zeroAndTaxOnly() {
        var result=service.preview(request("0.01",group(1,line(1,AmountType.PRINCIPAL,"0")),group(2,line(2,AmountType.OUTPUT_TAX,"0.01"))));
        assertThat(result.allocations()).hasSize(1);
        assertThat(result.allocations().getFirst().lineId()).isEqualTo(2L);
    }
    @Test @DisplayName("拒绝未实现策略") void reservedStrategy() {
        assertThatThrownBy(()->service.preview(new AllocationPreviewRequest(n("1"),2,AllocationStrategy.DOCUMENT_TAX_FIRST,List.of(a()))))
            .isInstanceOf(ServiceException.class).hasMessage("核销策略尚未启用");
    }
    @Test @DisplayName("拒绝币种精度外金额而不是自动舍入") void precision() {
        assertThatThrownBy(()->service.preview(request("0.001",a()))).isInstanceOf(ServiceException.class);
        assertThatThrownBy(()->service.preview(request("1",group(1,line(1,AmountType.PRINCIPAL,"0.001"))))).isInstanceOf(ServiceException.class);
    }
    @Test @DisplayName("重复行不能重复核销") void duplicateLine() {
        assertThatThrownBy(()->service.preview(request("200",a(),group(2,line(1,AmountType.PRINCIPAL,"100"))))).isInstanceOf(ServiceException.class);
    }
    @Test @DisplayName("负数余额不作为正数目标，必须走抵销来源") void negativeTarget() {
        assertThatThrownBy(()->service.preview(request("10",group(1,line(1,AmountType.PRINCIPAL,"-100"))))).isInstanceOf(ServiceException.class);
    }
    @Test @DisplayName("空目标返回全额未分配，零资金不产生明细") void emptyOrZero() {
        assertThat(service.preview(request("20")).unallocatedAmount()).isEqualByComparingTo("20");
        assertThat(service.preview(request("0",a())).allocations()).isEmpty();
    }
    @Test @DisplayName("拒绝缺失和非法金额") void invalid() {
        assertThatThrownBy(()->service.preview(null)).isInstanceOf(ServiceException.class);
        assertThatThrownBy(()->service.preview(request("-1",a()))).isInstanceOf(ServiceException.class);
        assertThatThrownBy(()->service.preview(request("10000000000000000",a()))).isInstanceOf(ServiceException.class);
        assertThatThrownBy(()->service.preview(request("10",a(),a()))).isInstanceOf(ServiceException.class);
    }
}
