#!/usr/bin/env python3
"""Sign a CI APK using the owner's private backup; never put the backup in this repository."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import tempfile
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument('--backup', required=True)
parser.add_argument('--apksigner', required=True)
parser.add_argument('--input', required=True)
parser.add_argument('--output', required=True)
args = parser.parse_args()

with zipfile.ZipFile(args.backup) as archive, tempfile.TemporaryDirectory(prefix='linguakey-sign-') as temp:
    info = json.loads(archive.read('signing.json'))
    key = Path(temp) / 'release.p12'
    key.write_bytes(archive.read('linguakey-release.p12'))
    key.chmod(0o600)
    env = dict(os.environ, LINGUAKEY_SIGNING_PASSWORD=info['password'])
    subprocess.run(['java', '-jar', args.apksigner, 'sign', '--ks', str(key),
                    '--ks-type', 'PKCS12', '--ks-key-alias', info['alias'],
                    '--ks-pass', 'env:LINGUAKEY_SIGNING_PASSWORD',
                    '--key-pass', 'env:LINGUAKEY_SIGNING_PASSWORD',
                    '--out', args.output, args.input], env=env, check=True)
subprocess.run(['java', '-jar', args.apksigner, 'verify', '--verbose', '--print-certs', args.output], check=True)
