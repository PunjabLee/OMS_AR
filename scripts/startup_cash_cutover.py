"""Exercise the packaged historical cutover CLI against the startup test's private schema."""
import json
import subprocess
import tempfile
from datetime import date
from pathlib import Path


def verify(root, env, api, port, sql, key):
    scope = dict(legalEntityId=1, customerId=9, currency='CNY')
    receipt = dict(scope, requestKey=key(), amount=123.45, receivedDate=str(date.today()),
                   sourceSystem='CUTOVER_TEST', sourceId=key(), sourceNo='opening')
    receipt_id = api(port, 'receipts/create', receipt)
    for mode in ('INVOICE', 'NO_INVOICE'):
        account = api(port, 'cash-accounts/open', dict(scope, requestKey=key(), invoiceMode=mode, reason='opening'))
        if mode == 'INVOICE':
            owner = account
    manifest = dict(scope, actor='migration-test', reason='test opening', groups={}, receipts={str(receipt_id):owner})
    with tempfile.TemporaryDirectory() as folder:
        path = Path(folder) / 'manifest.json'
        path.write_text(json.dumps(manifest))
        def run(action, *args, success=True):
            result = subprocess.run([str(root / 'scripts/cash-cutover'), action, str(path), *args],
                                    env=env, text=True, capture_output=True, timeout=30)
            if success:
                assert result.returncode == 0, 'Packaged cutover failed'
                return json.loads(result.stdout.strip().splitlines()[-1])
            assert result.returncode != 0 and '对账存在差异' in result.stderr
        # 注入缺少批次分摊的历史冻结单，专项对账必须阻止切换。
        sql(f"""INSERT INTO oms_ar_cash_hold(cash_account_id,purpose,amount,created_time,created_by,updated_time,updated_by)
                    VALUES({owner},'MANUAL',1,NOW(6),'cutover-test',NOW(6),'cutover-test');""")
        run('preview', success=False)
        sql(f"DELETE FROM oms_ar_cash_hold WHERE cash_account_id={owner} AND created_by='cutover-test';")
        preview = run('preview')
        assert not preview['applied'] and preview['cashBook'][str(owner)] == '123.45'
        args = (preview['fingerprint'], '--writes-drained')
        report = run('apply', *args)
        assert report == run('apply', *args)
    ledger = api(port, 'cash-accounts/ledger/page', dict(legalEntityId=1, accountId=owner, page=1, size=10))
    entry = ledger['records'][0]
    assert entry['eventType'] == 'CASH_CUTOVER' and entry['eventId'] == report['eventId']
    assert entry['bookBefore'] == 0 and entry['bookAfter'] == 123.45
    api(port, 'receipts/create', dict(receipt, requestKey=key(), sourceId=key()), status=409)
