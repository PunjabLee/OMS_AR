import os
import sys
import tempfile
import unittest
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from ar_local import read_env, build_launch

class LocalLaunchTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.jdk = self.root / 'jdk'
        java = self.jdk / 'bin/java'
        java.parent.mkdir(parents=True)
        java.write_text('test executable')
        java.chmod(0o700)
        target = self.root / 'oms-ar-server/target'
        target.mkdir(parents=True)
        (target / 'oms-ar-server-test.jar').write_text('placeholder')
        (self.root / '.env').write_text('MYSQL_PASSWORD=file-secret\nMYSQL_ROOT_PASSWORD=root-secret\nMYSQL_PORT=13306\nAR_API_TOKEN=' + 'f'*40 + '\n')
        self.env = {'AR_JDK_HOME': str(self.jdk), 'PATH': '/usr/bin'}
    def test_literal_env_values_never_execute(self):
        marker = self.root / 'must-not-exist'
        value = '$(touch ' + str(marker) + ')'
        (self.root / '.env').write_text('MYSQL_PASSWORD=' + value + '\nAR_API_TOKEN="a # b = c"\n')
        result = read_env(self.root / '.env')
        self.assertEqual(result['MYSQL_PASSWORD'], value)
        self.assertEqual(result['AR_API_TOKEN'], 'a # b = c')
        self.assertFalse(marker.exists())
    def test_precedence_and_secrets_only_in_child_environment(self):
        self.env.update(DB_PASSWORD='override-secret', AR_API_TOKEN='e'*40)
        args, env = build_launch(self.root, self.env)
        self.assertEqual(args[0], str(self.jdk / 'bin/java'))
        self.assertEqual(args[1], '-jar')
        self.assertEqual(env['DB_PASSWORD'], 'override-secret')
        self.assertEqual(env['DB_URL'], 'jdbc:mysql://127.0.0.1:13306/oms_ar')
        self.assertEqual(env['AR_API_TOKEN'], 'e'*40)
        self.assertNotIn('override-secret', ' '.join(args))
        self.assertNotIn('MYSQL_ROOT_PASSWORD', env)
        self.assertNotIn('MYSQL_PASSWORD', env)
    def test_token_required_and_errors_do_not_echo_secret(self):
        self.env['AR_API_TOKEN'] = 'short-secret'
        with self.assertRaises(ValueError) as error:
            build_launch(self.root, self.env)
        self.assertNotIn('short-secret', str(error.exception))
        (self.root / '.env').write_text('MYSQL_PASSWORD=file-secret\n')
        self.env.pop('AR_API_TOKEN')
        with self.assertRaises(ValueError):
            build_launch(self.root, self.env)
    def test_malformed_env_and_invalid_port_are_rejected(self):
        (self.root / '.env').write_text('BAD SECRET CONTENT\n')
        with self.assertRaises(ValueError) as error:
            read_env(self.root / '.env')
        self.assertNotIn('SECRET CONTENT', str(error.exception))
        (self.root / '.env').write_text('MYSQL_PASSWORD=pw\nAR_API_TOKEN='+'t'*40+'\nMYSQL_PORT=invalid\n')
        with self.assertRaises(ValueError):
            build_launch(self.root, self.env)
    def test_ambiguous_artifacts_and_missing_jdk_rejected(self):
        (self.root / 'oms-ar-server/target/oms-ar-server-old.jar').write_text('old')
        with self.assertRaises(ValueError):
            build_launch(self.root, self.env)
        self.env['AR_JDK_HOME'] = str(self.root / 'missing')
        with self.assertRaises(ValueError):
            build_launch(self.root, self.env)

if __name__ == '__main__':
    unittest.main()
