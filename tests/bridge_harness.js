// Стенд с НАСТОЯЩИМ мостом Capacitor (native-bridge.js из @capacitor/android):
// те же глобальные объекты и внедрение плагина, что делает Android (JSInjector/JSExport),
// а «нативная сторона» отвечает асинхронно через window.androidBridge.postMessage.
const {JSDOM}=require('jsdom'),FT=require('@sinonjs/fake-timers'),fs=require('fs'),path=require('path');
const BRIDGE=fs.readFileSync(path.join(path.dirname(require.resolve('@capacitor/android/package.json')),'capacitor/src/main/assets/native-bridge.js'),'utf8');
const TH=fs.readFileSync(__dirname+'/../www/theory.js','utf8');
const HTML=fs.readFileSync(__dirname+'/../www/index.html','utf8').replace('<script src="theory.js"></script>','<script>'+TH+'</script>');
const METHODS=['sync','hasOverlay','requestOverlay','requestNotifications','requestBattery','openAppSettings','openAutostart','getPending','getLog','clearLog'];
// То же, что генерирует JSExport.getPluginJS для плагина с promise-методами
function pluginJS(){
  const ms=METHODS.map(m=>"t['"+m+"'] = function(_options) {\nreturn w.Capacitor.nativePromise('BitReminder', '"+m+"', _options)\n}").join('\n');
  return "(function(w) {\nvar a = (w.Capacitor = w.Capacitor || {});\nvar p = (a.Plugins = a.Plugins || {});\nvar t = (p['BitReminder'] = {});\nt.addListener = function(eventName, callback) {\n  return w.Capacitor.addListener('BitReminder', eventName, callback);\n}\n"+ms+"\n})(window);\nwindow.Capacitor.PluginHeaders = "+JSON.stringify([{name:'BitReminder',methods:METHODS.map(n=>({name:n,rtype:'promise'}))}])+";";
}
exports.make=function(state,startMs,opt={}){
  const native=Object.assign({
    overlay:true,notifications:true,battery:false,running:true,device:'Xiaomi Redmi Note 12, Android 14 (API 34)',maker:'xiaomi',lastErr:'',pending:false,alarming:false
  },opt.native||{});
  const posted=[];
  const impl={
    sync:o=>{posted.push({m:'sync',o});return {}},
    hasOverlay:()=>{if(native.failHasOverlay)throw new Error(native.failHasOverlay);const n=Date.now();return {overlay:native.overlay,notifications:native.notifications,battery:native.battery,running:native.running,lastTick:native.running?(opt.nowFn?opt.nowFn():n)-1000:0,now:opt.nowFn?opt.nowFn():n,lastErr:native.lastErr,device:native.device,maker:native.maker,alarming:native.alarming}},
    requestOverlay:()=>{posted.push({m:'requestOverlay'});return {}},
    requestNotifications:()=>{posted.push({m:'requestNotifications'});return {}},
    requestBattery:()=>{posted.push({m:'requestBattery'});return {}},
    openAppSettings:()=>{posted.push({m:'openAppSettings'});return {}},
    openAutostart:()=>{posted.push({m:'openAutostart'});return {opened:''}},
    getPending:()=>{const r={start:native.pending,alarming:native.alarming};native.pending=false;return r},
    getLog:()=>({log:'01.01 00:00:00  служба запущена'}),clearLog:()=>({})
  };
  const nativeOnly=opt.noPlugin?'':pluginJS();
  const inject="window.Capacitor = { DEBUG: false, isLoggingEnabled: false, Plugins: {} };\nwindow.WEBVIEW_SERVER_URL = 'https://localhost';\n"+BRIDGE+"\n"+nativeOnly;
  const html=opt.web?HTML:HTML.replace('<head>','<head><script>'+inject.replace(/<\/script>/g,'<\\/script>')+'</script>');
  const dom=new JSDOM(html,{runScripts:'dangerously',url:'https://localhost/',pretendToBeVisual:true,
    beforeParse(w){
      w.localStorage.setItem('bit',JSON.stringify(state));w.alert=()=>{};w.confirm=()=>{posted.push({m:'confirm'});return opt.confirmAns!==false};
      w.HTMLCanvasElement.prototype.getContext=()=>new Proxy({},{get:()=>()=>{},set:()=>true});
      if(!opt.web){
        w.androidBridge={postMessage(s){const d=JSON.parse(s);
          setImmediate(()=>{let res;try{
            if(native.unregistered||d.pluginId!=='BitReminder'||!impl[d.methodName])throw new Error('"'+d.pluginId+'.'+d.methodName+'" is not implemented on android');
            res={callbackId:d.callbackId,pluginId:d.pluginId,methodName:d.methodName,save:false,success:true,data:impl[d.methodName](d.options)}}
            catch(e){res={callbackId:d.callbackId,pluginId:d.pluginId,methodName:d.methodName,save:false,success:false,error:{message:e.message}}}
            w.Capacitor.fromNative(res)})}};
      }
      w.clock=FT.withGlobal(w).install({now:startMs,toFake:['Date','setInterval','clearInterval','setTimeout','clearTimeout']});
    }});
  const w=dom.window,$=s=>w.document.querySelector(s);
  return {w,$,posted,native,
    flush:()=>new Promise(r=>setImmediate(()=>setImmediate(r))),tick:ms=>w.clock.tick(ms),
    ban:()=>{const b=$('#ban');return b.style.display==='flex'?b.className:'нет'},
    click:t=>{const b=[...w.document.querySelectorAll('button')].find(x=>x.textContent.includes(t));if(!b)throw new Error('нет кнопки '+t);b.click()},
    has:t=>[...w.document.querySelectorAll('button')].some(x=>x.textContent.includes(t)),
    ev:c=>w.eval(c),calls:m=>posted.filter(p=>p.m===m)};
};
