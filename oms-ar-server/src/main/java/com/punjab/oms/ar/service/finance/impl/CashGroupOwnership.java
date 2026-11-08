package com.punjab.oms.ar.service.finance.impl;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.punjab.oms.ar.base.ServiceException;
import com.punjab.oms.ar.dao.finance.CashAccountDao;
import com.punjab.oms.ar.domain.dto.finance.ReceivableCreateRequest;
import com.punjab.oms.ar.domain.entity.finance.*;
import com.punjab.oms.ar.enums.AmountType;

/** 校验已选范围中的业务组归属，产出新组所需的绑定计划。 @author Punjab */
@Component
public class CashGroupOwnership {
    private final CashAccountDao accounts;
    private final Set<String> scopes;

    public CashGroupOwnership(CashAccountDao accounts, @Value("${ar.cash.binding-scopes:}") String configured) {
        this.accounts = accounts;
        this.scopes = Set.copyOf(Arrays.asList(configured.split(",")));
    }

    /** 判断结算范围是否已启用分户。 */
    public boolean enabled(Long scope, Long customer, String currency) {
        return scopes.contains(scope + "/" + customer + "/" + currency) || accounts.cutoverComplete(scope,customer,currency);
    }

    /** 返回启用分户范围内每个业务组的归属，缺失映射时中止资金操作。 */
    public Map<Long,ArCashAccount> groupAccounts(Long legalEntityId, Long customerId, String currency,
                                                 Collection<Long> groupIds) {
        if (!enabled(legalEntityId,customerId,currency)) return Map.of();
        Map<Long,ArCashAccount> result = new HashMap<>();
        Map<Long,ArCashAccount> cache = new HashMap<>();
        for (Long groupId : groupIds) {
            Long id = accounts.boundAccount(groupId);
            ArCashAccount row = id == null ? null : cache.computeIfAbsent(id, key -> accounts.find(key, legalEntityId));
            if (row == null || !customerId.equals(row.getCustomerId()) || !currency.equals(row.getCurrency()))
                throw new ServiceException("业务组缺少同范围账户映射");
            result.put(groupId, row);
        }
        return result;
    }

    /** 锁定有资金或应收变化的归属账户，与销户串行。 */
    public ArCashAccount lockOpen(Long id,Long scope) {
        var account=accounts.account(id,scope,true);
        if(account==null || "CLOSED".equals(account.getStatus())) throw new ServiceException("账户已关闭，请先恢复");
        return account;
    }

    /** 同一新组可由一条明细指定属性，其余行共同继承。关联调整从原行所在组继承。 */
    public Map<String, ArCashAccount> resolve(ReceivableCreateRequest request, Map<Long,ArLine> originals) {
        if (!enabled(request.legalEntityId(),request.customerId(),request.currency())) {
            if (request.lines().stream().anyMatch(line -> line.invoiceMode() != null))
                throw new ServiceException("当前范围尚未启用现金账户归属");
            return Map.of();
        }
        Map<String, ArCashAccount> result = new HashMap<>();
        Map<Long, ArCashAccount> inherited = new HashMap<>();
        Map<String, ArCashAccount> modes = new HashMap<>();
        for (var line : request.lines()) {
            Long group = line.originalLineId() == null ? line.groupId() : originals.get(line.originalLineId()).getGroupId();
            ArCashAccount account;
            if (group != null) {
                account = inherited.computeIfAbsent(group, id -> {
                    Long binding = accounts.boundAccount(id);
                    return binding == null ? null : accounts.find(binding, request.legalEntityId());
                });
                if (account == null) throw new ServiceException("原业务组缺少账户归属，请先完成存量映射");
            } else if (line.invoiceMode() != null) {
                account = modes.computeIfAbsent(line.invoiceMode(), mode -> accounts.byMode(request.customerId(),
                        request.legalEntityId(), request.currency(), mode));
                if (account == null) throw new ServiceException("指定类型的现金账户尚未开设");
            } else continue;
            if ("CLOSED".equals(account.getStatus())) throw new ServiceException("归属账户已关闭");
            if (line.invoiceMode() != null && !line.invoiceMode().equals(account.getInvoiceMode()))
                throw new ServiceException("开票属性与原账户不一致");
            var prior = result.putIfAbsent(line.groupKey(), account);
            if (prior != null && !prior.getId().equals(account.getId()))
                throw new ServiceException("同一业务组不能绑定多个账户");
        }
        result.values().stream().map(ArCashAccount::getId).distinct().sorted()
                .forEach(id->lockOpen(id,request.legalEntityId()));
        for (var line : request.lines()) {
            var account = result.get(line.groupKey());
            if (account == null) throw new ServiceException("新增业务组必须选择开票属性");
            if (line.amountType() == AmountType.OUTPUT_TAX && !"INVOICE".equals(account.getInvoiceMode()))
                throw new ServiceException("不开票账户不能产生销项税应收");
        }
        return result;
    }
}
