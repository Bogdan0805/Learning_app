const {JSDOM}=require('jsdom'),FT=require('@sinonjs/fake-timers'),fs=require('fs');
const THEORY_INLINE=fs.readFileSync(__dirname+'/../www/theory.js','utf8');
const html=fs.readFileSync(__dirname+'/../www/index.html','utf8').replace('<script src="theory.js"></script>','<script>'+THEORY_INLINE.replace(/<\/script>/g,'')+'</script>');
const flush=()=>new Promise(r=>setImmediate(r));
function make(state,startMs,{overlay=false,confirmAns=true,running=true,err='',maker='xiaomi',device='Xiaomi Redmi Note 12, Android 14 (API 34)'}={}){
  const m={calls:[],pending:false,overlay,notif:true,log:[],running,err};
  let W=null;const plugin={sync:o=>{m.calls.push(o);return Promise.resolve()},
    hasOverlay:()=>{const n=(W?W.Date:Date).now();return Promise.resolve({overlay:m.overlay,notifications:m.notif,battery:false,running:m.running,lastTick:m.running?n-1000:0,now:n,lastErr:m.err,device,maker})},
    openAutostart:()=>{m.log.push('openAutostart');return Promise.resolve({opened:''})},
    getLog:()=>Promise.resolve({log:'13.10 12:00:01  служба запущена\n13.10 12:00:06  баннер показан, стадия 1'}),
    openAppSettings:()=>{m.log.push('openAppSettings');return Promise.resolve()},
    requestOverlay:()=>{m.log.push('requestOverlay');return Promise.resolve()},
    requestNotifications:()=>{m.log.push('requestNotifications');return Promise.resolve()},
    requestBattery:()=>{m.log.push('requestBattery');return Promise.resolve()},
    getPending:()=>{const r={start:m.pending};m.pending=false;return Promise.resolve(r)}};
  const dom=new JSDOM(html,{runScripts:'dangerously',url:'http://localhost/',pretendToBeVisual:true,
    beforeParse(w){w.localStorage.setItem('bit',JSON.stringify(state));w.alert=()=>{m.log.push('alert')};w.confirm=()=>{m.log.push('confirm');return confirmAns};
      w.HTMLCanvasElement.prototype.getContext=()=>new Proxy({},{get:()=>()=>{},set:()=>true});
      w.Capacitor={isNativePlatform:()=>true,Plugins:{BitReminder:plugin}};
      w.clock=FT.withGlobal(w).install({now:startMs,toFake:['Date','setInterval','clearInterval','setTimeout','clearTimeout']});}});
  const w=dom.window,$=s=>w.document.querySelector(s);W=w;
  return {m,w,$,tick:ms=>w.clock.tick(ms),
    ban:()=>{const b=$('#ban');return b.style.display==='flex'?b.className:'нет'},
    click:t=>{const b=[...w.document.querySelectorAll('button')].find(x=>x.textContent.includes(t));if(!b)throw new Error('нет кнопки '+t);b.click()},
    has:t=>[...w.document.querySelectorAll('button')].some(x=>x.textContent.includes(t)),
    ev:c=>w.eval(c),last:()=>m.calls[m.calls.length-1]};
}
const d0=new Date('2026-10-13T10:00:00'),t0=d0.getTime(),dk='2026-10-13';
const base={subs:{math:5,py:3},time:'17:00',n:1,p:{},total:0,xp:0,streak:0,last:'',day:d0.toDateString(),cnt:0,setup:true};
let fails=0;const ok=(n,c,i)=>{console.log((c?'✅':'❌')+' '+n+(i?'  → '+i:''));if(!c)fails++};
(async()=>{
 // 1. Разрешение на оверлей ЕСТЬ
 {const a=make(base,t0,{overlay:true});await flush();a.tick(1500);await flush();
  const s=a.last();ok('синхронизация с нативной службой отправлена',!!s,JSON.stringify(s));
  ok('в синхронизации: расписание, цель, день',s&&s.enabled===true&&s.time==='17:00'&&s.n===1&&s.cnt===0&&s.dayKey===dk&&s.busy===false&&s.demo===false);
  ok('карточка разрешений: всё включено, кнопки «Разрешить» нет',!a.has('Разрешить показ'));
  a.click('Демо');await flush();ok('демо передано в службу (demo:true)',a.last().demo===true);
  a.tick(30000);await flush();ok('при нативных баннерах веб-баннер НЕ показывается',a.ban()==='нет');
  a.m.pending=true;a.tick(1100);await flush();
  ok('нажали «Учиться» на нативном баннере → открылся выбор урока',a.w.document.querySelector('h2')&&a.w.document.querySelector('h2').textContent.includes('Как выберем'));
  ok('на время выбора урока передан busy:true',a.m.calls.some(c=>c.busy===true));
  a.click('Выбрать самому');a.click('Python');a.click('Понятно');let g=0;
  while(a.ev('LS.i')<=a.ev('LS.qs.length')&&g++<60){const q=a.ev('LS.qs[LS.i-1]');if(!q)break;[...a.w.document.querySelectorAll('.opt')][q[2]].click();a.click('Дальше');}
  await flush();const f=a.last();
  ok('после урока в службу ушло cnt=1 и demo=false',f.cnt===1&&f.demo===false&&f.busy===false,JSON.stringify(f));}
 // 2. Разрешения НЕТ → запасной веб-баннер работает, кнопка разрешения показана
 {const a=make(base,t0,{overlay:false});await flush();a.tick(1500);await flush();
  ok('без разрешения: на главной есть кнопка «Разрешить показ поверх приложений»',a.has('Разрешить показ'));
  a.click('Демо');await flush();a.tick(6500);await flush();
  ok('без разрешения: работает запасной веб-баннер',a.ban()==='s1',a.ban());
  a.m.overlay=true;a.w.dispatchEvent(new a.w.Event('focus'));await flush();a.tick(1100);await flush();
  ok('разрешили в настройках и вернулись: веб-баннер убран, дальше работает нативный',a.ban()==='нет');}
 // 3. Первичная настройка: запрос разрешения
 {const a=make({...base,setup:false,subs:{}},t0,{overlay:false,confirmAns:true});await flush();
  a.ev("S.subs={math:5};S.setup=false;setup2()");a.click('Начать первый урок');await flush();
  ok('после настройки предложено разрешение (confirm) и открыты настройки',a.m.log.includes('confirm')&&a.m.log.includes('requestOverlay'),a.m.log.join(','));
  ok('и открылся выбор урока',a.w.document.querySelector('h2').textContent.includes('Как выберем'));}
 {const a=make({...base,setup:false,subs:{}},t0,{overlay:false,confirmAns:false});await flush();
  a.ev("S.subs={math:5};setup2()");a.click('Начать первый урок');await flush();
  ok('отказ от разрешения: настройки не открываются, приложение работает',!a.m.log.includes('requestOverlay')&&a.w.document.querySelector('h2').textContent.includes('Как выберем'));}
 // 4. Уведомления запрашиваются один раз
 {const a=make(base,t0,{overlay:true});a.m.notif=false;await flush();a.tick(1500);await flush();a.w.dispatchEvent(new a.w.Event('focus'));await flush();
  ok('уведомления запрошены ровно один раз',a.m.log.filter(x=>x==='requestNotifications').length===1,a.m.log.join(','));}

 // 5. Служба не работает (убита системой), а разрешение есть → запасной веб-баннер, диагностика в карточке
 {const a=make(base,t0,{overlay:true,running:false,err:'java.lang.IllegalStateException: test'});await flush();a.tick(1500);await flush();
  const txt=()=>a.w.document.querySelector('#perm').textContent;
  ok('карточка: «Служба напоминаний: не отвечает» + причина',txt().includes('не отвечает')&&txt().includes('IllegalStateException'),txt().slice(0,120));
  a.click('Демо');await flush();a.tick(6500);await flush();
  ok('служба мертва, приложение открыто: работает запасной веб-баннер',a.ban()==='s1',a.ban());
  a.m.running=true;a.m.err='';a.tick(31000);await flush();
  ok('служба ожила: карточка «работает»',txt().includes('работает')&&!txt().includes('не отвечает'));
  ok('служба ожила: веб-баннер убран, дальше баннеры нативные',a.ban()==='нет');
  a.click('Настройки приложения');await flush();
  ok('кнопка «Настройки приложения» открывает системные настройки',a.m.log.includes('openAppSettings'));}

 // 6. Диагностика: модель телефона, подсказка для производителя, автозапуск, журнал службы
 {const a=make(base,t0,{overlay:true});await flush();a.tick(1500);await flush();
  const txt=()=>a.w.document.querySelector('#perm').textContent;
  ok('карточка показывает модель телефона',txt().includes('Xiaomi Redmi Note 12'),txt().slice(-160));
  ok('для Xiaomi показана своя инструкция (автозапуск, всплывающие окна)',txt().includes('Автозапуск')&&txt().includes('Всплывающие окна'));
  a.click('Автозапуск и питание');await flush();
  ok('кнопка «Автозапуск и питание» вызывает нативный экран',a.m.log.includes('openAutostart'));
  a.click('Журнал службы');await flush();
  const ta=a.w.document.querySelector('#logta');
  ok('журнал открывается в текстовом поле с моделью телефона и событиями',!!ta&&ta.value.includes('Xiaomi Redmi Note 12')&&ta.value.includes('баннер показан, стадия 1'));
  a.tick(31000);await flush();
  ok('при обновлении состояния открытый журнал не сбрасывается',!!a.w.document.querySelector('#logta'));
  a.click('Скрыть журнал');await flush();
  ok('журнал скрывается',!a.w.document.querySelector('#logta'));}
 {const a=make(base,t0,{overlay:true,maker:'samsung',device:'samsung SM-S911B, Android 14 (API 34)'});await flush();a.tick(1500);await flush();
  ok('для Samsung — инструкция про «Никогда не спящие»',a.w.document.querySelector('#perm').textContent.includes('Никогда не спящие'));}
 console.log(fails?`\nПРОВАЛОВ: ${fails}`:'\nВСЁ ОК');
process.exit(fails?1:0);
})();
