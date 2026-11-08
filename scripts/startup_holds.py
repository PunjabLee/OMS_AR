"""Exercise exact receipt-lot holds against the packaged application."""


def check(api, port, key, aa, receipt_aa, direct, fifo, ab=None):
    def account():
        return api(port, f'cash-accounts/detail?id={aa}&legalEntityId=1')

    initial = account()
    assert initial['bookAmount'] == initial['availableAmount'] == 181
    assert initial['frozenAmount'] == 0
    freeze = dict(requestKey=key(), legalEntityId=1, accountId=aa,
                  amount=120, reason='两批部分冻结')
    hold = api(port, 'cash-accounts/freeze', freeze)
    assert api(port, 'cash-accounts/freeze', freeze) == hold
    assert account()['version'] == initial['version'] + 1
    api(port, 'cash-accounts/freeze', dict(freeze, amount=121), status=409)
    assert account()['availableAmount'] == 61

    detail_path = f'cash-accounts/holds/detail?legalEntityId=1&accountId={aa}&holdId={hold}'
    detail = api(port, detail_path)
    assert detail['purpose'] == 'MANUAL' and detail['remainingAmount'] == 120
    assert [s['amount'] for s in detail['slices']] == [100, 20]
    assert api(port, f'receipts/detail?id={receipt_aa}&legalEntityId=1')['availableAmount'] == 0
    api(port, 'settlements/preview', dict(direct, amount=1), status=409)
    api(port, 'cash-accounts/settlements/preview', dict(fifo, amount=62), status=409)

    release = dict(requestKey=key(), legalEntityId=1, accountId=aa, holdId=hold,
                   amount=105, reason='部分解冻')
    if ab is not None:
        api(port, 'cash-accounts/unfreeze', dict(release, accountId=ab), status=409)
    assert api(port, 'cash-accounts/unfreeze', release) == hold
    assert api(port, 'cash-accounts/unfreeze', release) == hold
    assert account()['version'] == initial['version'] + 2
    assert account()['availableAmount'] == 166
    partial = api(port, detail_path)
    assert partial['remainingAmount'] == 15
    assert [s['releasedAmount'] for s in partial['slices']] == [100, 5]
    assert api(port, f'receipts/detail?id={receipt_aa}&legalEntityId=1')['availableAmount'] == 100
    api(port, 'cash-accounts/unfreeze', dict(release, requestKey=key(), amount=16), status=409)
    api(port, 'cash-accounts/unfreeze', dict(release, requestKey=key(), amount=15))
    restored = account()
    assert restored['version'] == initial['version'] + 3
    assert restored['frozenAmount'] == 0 and restored['availableAmount'] == restored['bookAmount'] == 181
    assert api(port, detail_path)['remainingAmount'] == 0
