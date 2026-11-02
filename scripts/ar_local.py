"""Local launch configuration. Values are data; no shell expansion is performed."""
import os
import re
from pathlib import Path

DEFAULT_JDK = '/Users/punjab/Library/Java/JavaVirtualMachines/ms-21.0.13.0.2/Contents/Home'

def read_env(path):
    path = Path(path)
    if not path.exists():
        return {}
    result = {}
    for number, raw in enumerate(path.read_text().splitlines(), 1):
        line = raw.strip()
        if not line or line.startswith('#'):
            continue
        if '=' not in line:
            raise ValueError(f'Invalid .env entry at line {number}')
        key, value = line.split('=', 1)
        key, value = key.strip(), value.strip()
        if not re.fullmatch(r'[A-Z][A-Z0-9_]*', key) or key in result:
            raise ValueError(f'Invalid or duplicate .env key at line {number}')
        if value.startswith(('"', "'")):
            if len(value) < 2 or value[-1] != value[0]:
                raise ValueError(f'Unclosed .env quote at line {number}')
            value = value[1:-1]
        result[key] = value
    return result

def build_launch(root, environ):
    root = Path(root)
    settings = dict(read_env(root / '.env'), **dict(environ))
    token = settings.get('AR_API_TOKEN', '')
    if len(token) < 32 or len(token) > 4000 or any(ord(c) < 33 or ord(c) > 126 for c in token):
        raise ValueError('AR_API_TOKEN must contain 32–4000 printable non-space ASCII characters')
    password = settings.get('DB_PASSWORD', settings.get('MYSQL_PASSWORD', ''))
    if not password:
        raise ValueError('DB_PASSWORD or MYSQL_PASSWORD is required')
    try:
        port = int(settings.get('MYSQL_PORT', '13306'))
        server_port = int(settings.get('SERVER_PORT', '8080'))
        if not 1 <= port <= 65535 or not 0 <= server_port <= 65535:
            raise ValueError()
    except ValueError:
        raise ValueError('MYSQL_PORT or SERVER_PORT is outside its valid range') from None
    jdk = Path(settings.get('AR_JDK_HOME', DEFAULT_JDK)).expanduser()
    java = jdk / 'bin/java'
    if not java.is_file() or not os.access(java, os.X_OK):
        raise ValueError('AR_JDK_HOME must point to an installed JDK 21')
    jars = [Path(settings['AR_APP_JAR']).expanduser()] if settings.get('AR_APP_JAR') else list((root / 'oms-ar-server/target').glob('oms-ar-server-*.jar'))
    if len(jars) != 1 or not jars[0].is_file():
        raise ValueError('Build one executable JAR with Maven package, or set AR_APP_JAR explicitly')
    child = dict(environ)
    child.update({key: value for key, value in settings.items() if key.startswith(('AR_', 'DB_', 'SERVER_', 'SPRING_'))})
    child['DB_URL'] = settings.get('DB_URL', f'jdbc:mysql://127.0.0.1:{port}/oms_ar')
    child['DB_USERNAME'] = settings.get('DB_USERNAME', 'oms_ar')
    child['DB_PASSWORD'] = password
    child['AR_API_TOKEN'] = token
    child['JAVA_HOME'] = str(jdk)
    child['PATH'] = str(jdk / 'bin') + os.pathsep + child.get('PATH', os.defpath)
    child.pop('MYSQL_ROOT_PASSWORD', None)
    child.pop('MYSQL_PASSWORD', None)
    return [str(java), '-jar', str(jars[0].resolve())], child
