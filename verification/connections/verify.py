"""Native, serial full-pack cable/energy verification. Only marked disposable worlds."""
from pathlib import Path
import hashlib,json,shutil,subprocess,sys,uuid,zipfile

PROJECT=Path(__file__).resolve().parents[2]
ROOT=PROJECT.parent.parent
OUT=ROOT/('.work/verification/mekanism-rod-render-20261017' if '--rod' in sys.argv else '.work/verification/mekanism-connections-20261017')
JAVA=ROOT/'.work/devtools/jdk-25/bin'
sys.path.insert(0,str(ROOT/'.work/SISE-Core-26/tools/multiblock'))
from verify_pack import allowed,arguments,running_fixtures
from verify_catalog_delivery import production_clients

def sha(p):
    with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()

def manifest(folder):return {p.relative_to(folder).as_posix():sha(p) for p in folder.rglob('*') if p.is_file()}

def probe(jars):
    classes=PROJECT/'build/connection-probe-classes'
    if classes.exists():
        assert classes.resolve().is_relative_to((PROJECT/'build').resolve())
        shutil.rmtree(classes)
    classes.mkdir(parents=True)
    paths=list((ROOT/'libraries').rglob('*.jar'))+jars
    paths.sort(key=lambda p:(0 if 'minecraft-client-patched' in str(p) else 1,str(p)))
    args=['-encoding','UTF-8','--release','25','-proc:none','-classpath',';'.join(map(str,paths)),'-d',str(classes)]+list(map(str,(PROJECT/'verification/connections/src').rglob('*.java')))
    argfile=PROJECT/'build/connection-probe.args';argfile.write_text('\n'.join('"'+s.replace('\\','/')+'"' for s in args),'utf-8')
    subprocess.run([str(JAVA/'javac.exe'),'@'+str(argfile)],check=True)
    target=PROJECT/'build/connection-probe.jar'
    with zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as z:
        for p in classes.rglob('*.class'):z.write(p,p.relative_to(classes).as_posix())
        z.writestr('META-INF/neoforge.mods.toml','modLoader="javafml"\nloaderVersion="[11,)"\nlicense="MIT"\n[[mods]]\nmodId="mekextras_connection_probe"\nversion="1.0"\ndisplayName="Isolated cable verification"\n')
    return target

def launch(baseline):
    assert not production_clients() and not running_fixtures(),'Close the existing client normally first.'
    OUT.mkdir(parents=True,exist_ok=True)
    if not (OUT/'before.json').exists():
        protected=json.loads((ROOT/'.work/verification/multiblock-tools-20261017/minecraft-before.json').read_text('utf-8'))
        (OUT/'before.json').write_text(json.dumps({'mods':manifest(ROOT/'mods'),'worlds':manifest(ROOT/'saves'),'protected':{name:sha(ROOT/name) for name in protected}},indent=2),'utf-8')
    name='test-mek-connections-'+(('baseline2' if '--recheck' in sys.argv else 'baseline') if baseline else ('fixed2' if '--recheck' in sys.argv else 'fixed'))+'-20261017'
    if '--rod' in sys.argv:name='test-mek-rod-render-'+('baseline' if baseline else 'fixed')+('2' if '--recheck' in sys.argv else '')+'-20261017'
    fixture=ROOT/'.work'/name
    assert not fixture.exists() or ((fixture/'SISE_AUTOTEST_ONLY').is_file() and not (fixture/'saves').exists()),fixture
    fixture.mkdir(exist_ok=True);(fixture/'SISE_AUTOTEST_ONLY').write_text('Disposable connection verification\n','utf-8')
    mods=fixture/'mods';mods.mkdir(exist_ok=True)
    for p in (ROOT/'mods').glob('*.jar'):
        if not baseline and (p.name.startswith('Mekanism-') or ('--rod' not in sys.argv and p.name.startswith('mekanism_extras-'))):continue
        shutil.copy2(p,mods/p.name)
    if not baseline:
        candidates=[(ROOT/'.work/Mekanism','Mekanism-*.energy.2.jar')] if '--rod' in sys.argv else [(ROOT/'.work/Mekanism','Mekanism-*.energy.1.jar'),(PROJECT,'mekanism_extras-*render.1.jar')]
        for project,pattern in candidates:
            jar=next((project/'build/libs').glob(pattern));shutil.copy2(jar,mods/jar.name)
    testjar=probe(list(mods.glob('*.jar')));shutil.copy2(testjar,mods/testjar.name)
    for folder in ('config','defaultconfigs','kubejs','tacz','CustomSkinLoader'):
        if (ROOT/folder).exists():shutil.copytree(ROOT/folder,fixture/folder)
    (fixture/'config/dynamic_fps.json').write_text('{"enabled":false}','utf-8')
    (fixture/'options.txt').write_text('version:4790\nlang:zh_cn\nguiScale:2\nresourcePacks:[]\nfullscreen:false\nonboardAccessibility:false\ntutorialStep:none\nsoundCategory_master:0.0\nrenderDistance:3\nsimulationDistance:5\nmaxFps:60\npauseOnLostFocus:false\n','utf-8')
    metadata=json.loads((ROOT/'project02.json').read_text('utf-8'))
    cp=[str(ROOT/'libraries'/e['downloads']['artifact']['path']) for e in metadata['libraries'] if allowed(e)]+[str(ROOT/'project02.jar')]
    player='CableProbe';repl={'auth_player_name':player,'auth_uuid':uuid.UUID(bytes=hashlib.md5(('OfflinePlayer:'+player).encode()).digest(),version=3).hex,'auth_access_token':'0','auth_xuid':'','clientid':'','user_type':'legacy','version_name':'project02','version_type':'release','game_directory':str(fixture),'assets_root':str(ROOT/'assets'),'assets_index_name':metadata['assetIndex']['id'],'natives_directory':str(ROOT/'natives'),'library_directory':str(ROOT/'libraries'),'launcher_name':'cable-verification','launcher_version':'1','classpath':';'.join(cp)}
    cmd=[str(JAVA/'javaw.exe'),'-Xms1G','-Xmx7G','-Dmekextras.connections.fixture='+str(fixture),'-Dmekextras.connections.baseline='+str(baseline).lower()]+arguments(metadata['arguments']['jvm'],repl)
    if '--rod' in sys.argv:cmd.insert(4,'-Dmekextras.connections.rodOnly=true')
    (fixture/'jna-tmp').mkdir();cmd+=['-Djna.tmpdir='+str(fixture/'jna-tmp'),metadata['mainClass']]+arguments(metadata['arguments']['game'],repl)+['--width','1280','--height','800']
    with (OUT/(name+'.log')).open('w',encoding='utf-8') as log:proc=subprocess.Popen(cmd,cwd=fixture,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
    info={'pid':proc.pid,'fixture':str(fixture),'mods':manifest(mods)}
    (OUT/(name+'-process.json')).write_text(json.dumps(info,indent=2),'utf-8');print(json.dumps({k:v for k,v in info.items() if k!='mods'}))

if __name__=='__main__':launch('--baseline' in sys.argv)
