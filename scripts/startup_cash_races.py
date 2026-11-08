"""Competing HTTP commands against one exclusively owned startup account."""
from concurrent.futures import ThreadPoolExecutor
from threading import Barrier


def check(api, port, key, account, today):
    scope = dict(legalEntityId=1, customerId=8, currency='CNY')
    doc = api(port, 'receivables/create', dict(scope, requestKey=key(), businessType='SERVICE',
              businessDate=today, reason='并发验收', lines=[dict(groupKey='race', invoiceMode='INVOICE',
              amountType='PRINCIPAL', amount=200)]))
    group = api(port, f'receivables/detail?id={doc}&legalEntityId=1')['lines'][0]['groupId']
    receipt = api(port, 'receipts/create', dict(scope, cashAccountId=account, requestKey=key(), amount=100,
                  receivedDate=today, sourceSystem='RACE', sourceId=key(), sourceNo=key()))

    def confirm(path, source):
        preview = dict(source, legalEntityId=1, amount=80, groupIds=[group], strategy='GROUP_TAX_FIRST')
        fingerprint = api(port, path+'/preview', preview)['fingerprint']
        return path+'/confirm', dict(requestKey=key(), preview=preview, expectedFingerprint=fingerprint, reason='并发分配')

    def race(first, second):
        barrier = Barrier(2)

        def send(command):
            barrier.wait(timeout=5)
            return api(port, *command, status=(200, 409))

        with ThreadPoolExecutor(max_workers=2) as pool:
            results = list(pool.map(send, [first, second]))
        assert sorted(code for code, data in results) == [200, 409], results
        return next((index, data) for index, (code, data) in enumerate(results) if code == 200)

    def balance():
        return api(port, f'cash-accounts/detail?id={account}&legalEntityId=1')['availableAmount']

    def reverse(settlement):
        api(port, 'settlements/reverse', dict(legalEntityId=1, settlementId=settlement,
            requestKey=key(), reason='核对原路恢复'))

    direct = lambda: confirm('settlements', dict(receiptId=receipt))
    pooled = lambda: confirm('cash-accounts/settlements', dict(accountId=account))
    winner, data = race(direct(), pooled())
    assert balance() == 20
    reverse(data if winner == 0 else data['settlementIds'][0])
    assert balance() == 100

    hold = ('cash-accounts/freeze', dict(legalEntityId=1, accountId=account, amount=50,
            requestKey=key(), reason='竞争冻结'))
    winner, data = race(direct(), hold)
    if winner == 0:
        assert balance() == 20
        reverse(data)
    else:
        assert balance() == 50
        api(port, 'cash-accounts/unfreeze', dict(hold[1], holdId=data, requestKey=key()))
    assert balance() == 100
    assert api(port, 'cash-accounts/reconciliation/page', scope)['total'] == 0
