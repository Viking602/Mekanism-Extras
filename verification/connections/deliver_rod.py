"""Install only the Mekanism JAR exercised by the isolated rod-rendering probe."""
from pathlib import Path
import json, re, shutil, subprocess, sys, zipfile, xml.etree.ElementTree as ET
from verify import ROOT, sha, manifest, running_fixtures, production_clients
from deliver import changed

OUT = ROOT / '.work/verification/mekanism-rod-render-20261017'


def registry_targets(jar):
    """The annotation processor emits registrations in unspecified order; compare their actual targets."""
    output = subprocess.check_output([str(ROOT / '.work/devtools/jdk-25/bin/javap.exe'), '-v', '-p', '-c', '-classpath',
                                      str(jar), 'mekanism.generated.mekanism.ComputerMethodRegistry_mekanism'], text=True)
    targets, current = [], []
    for line in output.splitlines():
        if match := re.search(r'\bldc(?:_w)?\s+.*// class (\S+)', line): current.append(match.group(1))
        if match := re.search(r'// Method mekanism/common/integration/computer/FactoryRegistry\.(register(?:Interface)?):', line):
            targets.append((match.group(1), *current))
            current = []
    constructors = sorted(re.findall(r'REF_newInvokeSpecial (\S+\."<init>":\(\)V)', output))
    assert len(targets) > 50 and len(constructors) >= len(targets)
    return sorted(targets), constructors


def run(install):
    assert not production_clients() and not running_fixtures(), 'Clients must exit normally first.'
    before = json.loads((OUT / 'before.json').read_text('utf-8'))
    baseline_fixture = ROOT / '.work/test-mek-rod-render-baseline2-20261017'
    baseline = json.loads((baseline_fixture / 'rod-result.json').read_text('utf-8'))
    assert baseline['passed'] and not baseline['error']
    assert len(baseline['failures']) == 144 and all('terminal plate count=0' in failure for failure in baseline['failures'])
    fixture = ROOT / '.work/test-mek-rod-render-fixed-20261017'
    result = json.loads((fixture / 'rod-result.json').read_text('utf-8'))
    assert result['passed'] and not result['error'] and not result['failures'], result
    model_rows = [row for check in result['checks'] for row in check.get('models', [])]
    assert len(model_rows) == 288 and all(row['cacheMatches'] for row in model_rows)
    power_rows = [row for check in result['checks'] for row in check.get('power', [])]
    assert len(power_rows) == 48 and all(row['rodEnergy'] > 0 for row in power_rows)
    process = json.loads((OUT / (fixture.name + '-process.json')).read_text('utf-8'))
    jar = next((ROOT / '.work/Mekanism/build/libs').glob('Mekanism-*.energy.2.jar'))
    assert sha(jar) == process['mods'][jar.name] == sha(fixture / 'mods' / jar.name)
    previous = next(name for name in before['mods'] if name.startswith('Mekanism-'))
    expected_other = {name: digest for name, digest in before['mods'].items() if name != previous}
    installed = manifest(ROOT / 'mods')
    assert {name: digest for name, digest in installed.items() if name not in (previous, jar.name)} == expected_other, 'Another mod changed.'
    assert manifest(ROOT / 'saves') == before['worlds'], 'Original saves changed.'
    assert all(sha(ROOT / name) == digest for name, digest in before['protected'].items()), 'Original game file changed.'
    suites = [ET.parse(p).getroot().attrib for p in (ROOT / '.work/Mekanism/build/test-results/test').glob('TEST-*.xml')]
    tests = {key: sum(int(s.get(key, 0)) for s in suites) for key in ('tests', 'failures', 'errors', 'skipped')}
    assert tests == {'tests': 74, 'failures': 0, 'errors': 0, 'skipped': 0}, tests
    old = ROOT / 'mods' / previous
    backup = OUT / 'install-backup' / previous
    backup.parent.mkdir(exist_ok=True)
    if old.exists():
        assert sha(old) == before['mods'][previous]
        if not backup.exists(): shutil.copy2(old, backup)
    assert sha(backup) == before['mods'][previous]
    entries = changed(backup, jar)
    assert all(name == 'META-INF/MANIFEST.MF' or name.startswith('mekanism/client/model/blockstate/TransmitterBlockStateModel')
               or name.startswith('mekanism/client/model/data/TransmitterModelData')
               or name in ('mekanism/common/content/network/transmitter/Transmitter.class', 'mekanism/common/tile/transmitter/TileEntityTransmitter.class',
                           'mekanism/generated/mekanism/ComputerMethodRegistry_mekanism.class')
               for name in entries), entries
    assert registry_targets(backup) == registry_targets(jar), 'Computer registry targets changed.'
    if install and old.exists():
        destination = ROOT / 'mods' / jar.name
        incoming = destination.with_suffix('.incoming')
        assert not destination.exists() and not incoming.exists() and old.parent == destination.parent == ROOT / 'mods'
        shutil.copy2(jar, incoming)
        assert sha(incoming) == sha(jar)
        try:
            old.unlink()
            incoming.rename(destination)
        except BaseException:
            if not old.exists(): shutil.copy2(backup, old)
            raise
    if install: assert sha(ROOT / 'mods' / jar.name) == sha(jar)
    evidence = OUT / 'native-evidence'
    evidence.mkdir(exist_ok=True)
    for prefix, folder in (('baseline', baseline_fixture), ('fixed', fixture)):
        for file in [folder / 'rod-result.json', *folder.glob('rod-*.png')]:
            shutil.copy2(file, evidence / (prefix + '-' + file.name))
    audit = {'passed': True, 'installed': install, 'nativeModelCases': len(model_rows), 'nativePowerCases': len(power_rows),
             'unitTests': tests, 'protectedGameFiles': len(before['protected']), 'unchangedSaveFiles': len(before['worlds']),
             'unchangedOtherModFiles': len(expected_other), 'nativeClientPid': process['pid'], 'nativeClientExited': True,
             'computerRegistryTargetsUnchanged': True,
             'previous': previous, 'previousSha256': sha(backup), 'artifact': jar.name, 'sha256': sha(jar), 'changedEntries': entries}
    (OUT / 'delivery-audit.json').write_text(json.dumps(audit, ensure_ascii=False, indent=2) + '\n', 'utf-8')
    print(json.dumps({key: value for key, value in audit.items() if key != 'changedEntries'}, ensure_ascii=False))


if __name__ == '__main__': run('--install' in sys.argv)
