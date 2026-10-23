"""Verify the exact native-tested artifacts, preserve protected files, and install two native mods."""
from pathlib import Path
import json,shutil,sys,zipfile,xml.etree.ElementTree as ET
from verify import ROOT,PROJECT,OUT,sha,manifest,running_fixtures,production_clients

def changed(old,new):
    with zipfile.ZipFile(old) as a,zipfile.ZipFile(new) as b:
        aa={n for n in a.namelist() if not n.endswith('/')};bb={n for n in b.namelist() if not n.endswith('/')}
        return sorted(n for n in aa|bb if (a.read(n) if n in aa else None)!=(b.read(n) if n in bb else None))

def run(install):
    assert not production_clients() and not running_fixtures(),'Native clients must finish and exit normally before delivery.'
    before=json.loads((OUT/'before.json').read_text('utf-8'))
    fixture=ROOT/'.work/test-mek-connections-fixed2-20261017'
    result=json.loads((fixture/'connection-result.json').read_text('utf-8'))
    assert result['passed'] and not result['error'] and not result['failures'],result
    baseline=json.loads((ROOT/'.work/test-mek-connections-baseline2-20261017/connection-result.json').read_text('utf-8'))
    assert baseline['failures']==['default creative cube supplies without extra setup']
    process=json.loads((OUT/(fixture.name+'-process.json')).read_text('utf-8'))
    sources=[next((ROOT/'.work/Mekanism/build/libs').glob('Mekanism-*.energy.1.jar')),next((PROJECT/'build/libs').glob('mekanism_extras-*render.1.jar'))]
    installed=manifest(ROOT/'mods')
    for jar in sources:assert sha(jar)==process['mods'][jar.name]==sha(fixture/'mods'/jar.name)
    previous=[next(name for name in before['mods'] if name.startswith(prefix)) for prefix in ('Mekanism-','mekanism_extras-')]
    expected={name:digest for name,digest in before['mods'].items() if name not in previous}
    actual={name:digest for name,digest in installed.items() if name not in previous and name not in {p.name for p in sources}}
    assert actual==expected,'Unrelated mod changed; do not overwrite concurrent work.'
    assert manifest(ROOT/'saves')==before['worlds'],'Original saves changed.'
    assert all(sha(ROOT/name)==digest for name,digest in before['protected'].items()),'Original game file changed.'
    backup=OUT/'install-backup';backup.mkdir(exist_ok=True)
    audit={'passed':True,'nativeChecks':sum('passed' in c for c in result['checks']),'worldSaveReloadVerified':True,'protectedGameFiles':len(before['protected']),'unchangedSaveFiles':len(before['worlds']),'unchangedOtherModFiles':len(expected),'nativeClientPid':process['pid'],'nativeClientExited':True,'artifacts':[]}
    suites=[ET.parse(p).getroot().attrib for p in (ROOT/'.work/Mekanism/build/test-results/test').glob('TEST-*.xml')]
    audit['unitTests']={key:sum(int(s.get(key,0)) for s in suites) for key in ('tests','failures','errors','skipped')}
    assert audit['unitTests']['tests']==72 and audit['unitTests']['failures']==audit['unitTests']['errors']==0
    for old_name,new in zip(previous,sources):
        old=ROOT/'mods'/old_name;saved=backup/old_name
        if old.exists():
            assert sha(old)==before['mods'][old_name]
            if not saved.exists():shutil.copy2(old,saved)
        assert sha(saved)==before['mods'][old_name]
        entries=changed(saved,new)
        if old_name.startswith('mekanism_extras-'):
            assert set(entries)=={'META-INF/MANIFEST.MF','com/jerry/mekextras/client/render/transmitter/ExtraTransmitterRenderer.class','com/jerry/mekextras/client/render/transmitter/ExtraTransmitterRenderer$1.class'}
        else:
            assert all(n=='META-INF/MANIFEST.MF' or n.startswith('mekanism/client/render/transmitter/') or n.startswith('mekanism/common/tile/TileEntityEnergyCube') or n=='mekanism/common/item/block/ItemBlockEnergyCube.class' or n=='mekanism/generated/mekanism/ComputerMethodRegistry_mekanism.class' or n in ('assets/mekanism/lang/en_us.json','assets/mekanism/lang/zh_cn.json') for n in entries)
            with zipfile.ZipFile(saved) as a,zipfile.ZipFile(new) as b:
                for lang in ('zh_cn','en_us'):
                    path='assets/mekanism/lang/'+lang+'.json';aa=json.loads(a.read(path));bb=json.loads(b.read(path))
                    for suffix in ('source','sink'):bb.pop('block.mekanism.creative_energy_cube.'+suffix)
                    assert aa==bb,'Existing language entries changed'
        audit['artifacts'].append({'previous':old_name,'previousSha256':sha(saved),'installed':new.name,'sha256':sha(new),'changedEntries':entries})
        if install and old.exists():
            destination=ROOT/'mods'/new.name;incoming=destination.with_suffix('.incoming')
            assert destination.parent==old.parent==ROOT/'mods' and not destination.exists() and not incoming.exists()
            shutil.copy2(new,incoming);assert sha(incoming)==sha(new)
            try:
                old.unlink();incoming.rename(destination)
            except BaseException:
                if not old.exists():shutil.copy2(saved,old)
                raise
        if install:assert sha(ROOT/'mods'/new.name)==sha(new)
    audit['installed']=install
    (OUT/'delivery-audit.json').write_text(json.dumps(audit,ensure_ascii=False,indent=2)+'\n','utf-8')
    print(json.dumps({k:v for k,v in audit.items() if k!='artifacts'},ensure_ascii=False))

if __name__=='__main__':run('--install' in sys.argv)
