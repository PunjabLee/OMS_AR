"""Verify offline withdrawal commitments against the packaged application's disposable schema."""


def check(api, port, key, aa, payer):
    scope = 1

    def post(route, body, **options):
        return api(port, 'withdrawals/' + route, body, **options)

    def command(**fields):
        return dict(legalEntityId=scope, requestKey=key(), reason='提现验收', **fields)

    def cash():
        return api(port, f'cash-accounts/detail?id={aa}&legalEntityId={scope}')

    def switch(action):
        return api(port, 'cash-accounts/' + action, command(accountId=aa, expectedVersion=cash()['version']))

    def action(route, wid, **options):
        return post(route, command(id=wid), **options)

    def detail(wid):
        return api(port, f'withdrawals/detail?id={wid}&legalEntityId={scope}')

    base = cash()
    payout = post('payout-accounts/bind', command(customerId=7, currency='CNY', beneficiaryRef='vault-'+key(), lastFour='1248'))

    def apply(amount):
        cmd = command(cashAccountId=aa, payoutAccountId=payout, amount=amount)
        wid = post('apply', cmd)
        assert post('apply', cmd) == wid
        assert detail(wid)['maskedAccount'] == '****1248'
        return wid

    def approve(wid):
        assert action('approve', wid, reviewer=True) == wid

    cancelled = apply(2)
    action('approve', cancelled, status=409)
    action('cancel', cancelled)
    assert detail(cancelled)['status'] == 'CANCELLED'
    rejected = apply(3)
    action('reject', rejected, reviewer=True)
    assert cash()['availableAmount'] == base['availableAmount']

    first, second = apply(11), apply(5)
    approve(first)
    approve(second)
    action('claim', first)
    action('cancel', first, status=409)
    post('result', command(id=first, outcome='UNKNOWN'))
    assert cash()['frozenAmount'] == base['frozenAmount'] + 16
    assert detail(first)['status'] == 'UNKNOWN'
    post('result', command(id=first, outcome='FAILED'), status=409)
    switch('stop')
    action('claim', second, status=409)
    receipt = 'BANK-' + key()
    settled = command(id=first, outcome='PAID', bankReceipt=receipt)
    assert post('result', settled) == post('result', settled) == first
    assert cash()['bookAmount'] == base['bookAmount'] - 11
    assert detail(first)['bankReceipt'] == receipt
    switch('resume')
    action('claim', second)
    post('result', command(id=second, outcome='PAID', bankReceipt=receipt), status=409)
    assert detail(second)['status'] == 'PROCESSING'
    assert cash()['frozenAmount'] == base['frozenAmount'] + 5
    post('result', command(id=second, outcome='FAILED', evidence='bank-rejection-'+key()))
    assert cash()['availableAmount'] == base['availableAmount'] - 11
    assert cash()['frozenAmount'] == base['frozenAmount']
    assert detail(second)['status'] == 'FAILED'
    action('payout-accounts/disable', payout)
    post('apply', command(cashAccountId=aa, payoutAccountId=payout, amount=1), status=409)
