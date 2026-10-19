const {make}=require('./bridge_harness');
let fails=0;const ok=(n,c,i)=>{console.log((c?'✅':'❌')+' '+n+(i?'  → '+i:''));if(!c)fails++};
const d0=new Date('2026-10-13T10:00:00'),t0=d0.getTime();
const base={subs:{math:5,py:3},time:'17:00',n:1,p:{},total:0,xp:0,streak:0,last:'',day:d0.toDateString(),cnt:0,setup:true};
(async()=>{
 // 1. Настоящий мост: приложение видит плагин и показывает карточку
 {const a=make(base,t0);await a.flush();await a.flush();
  ok('мост Capacitor определился как нативная платформа',a.ev('Capacitor.isNativePlatform()')===true);
  ok('плагин BitReminder доступен в Capacitor.Plugins',a.ev('typeof Capacitor.Plugins.BitReminder.hasOverlay')==='function');
  ok('приложение нашло плагин (NB() не null)',a.ev('NB()!==null'));
  const t=a.$('#perm').textContent;
  ok('на главной показана карточка «Напоминания в фоне»',t.includes('Напоминания в фоне'),t.slice(0,80));
  ok('когда всё в порядке — компактная карточка «Напоминания в фоне работают»',t.includes('Напоминания в фоне работают')&&a.ev('nat.overlay')===true&&a.ev('nat.run')===true);
  ok('состояние отправлено в нативную сторону (sync)',a.calls('sync').length>0,JSON.stringify((a.calls('sync')[0]||{}).o));}
 // 2. Разрешения нет → в карточке кнопка, нажатие вызывает системные настройки
 {const a=make(base,t0,{native:{overlay:false,notifications:false}});await a.flush();await a.flush();
  ok('без разрешения: кнопка «Разрешить показ поверх приложений» есть',a.has('Разрешить показ поверх'));
  a.click('Разрешить показ поверх');await a.flush();
  ok('нажатие реально дошло до нативной стороны (requestOverlay)',a.calls('requestOverlay').length===1);}
 // 3. Нативная сторона отвечает ошибкой → причина видна в карточке (а не молчаливое отсутствие)
 {const a=make(base,t0,{native:{failHasOverlay:'boom: тестовая ошибка'}});await a.flush();await a.flush();
  const t=a.$('#perm').textContent;
  ok('при ошибке hasOverlay карточка объясняет причину',t.includes('Нативный модуль не отвечает')&&t.includes('boom'),t.slice(0,140));}
 // 4. Плагина нет вообще (например, не та сборка) → карточка это говорит
 {const a=make(base,t0,{noPlugin:true,native:{unregistered:true}});await a.flush();await a.flush();
  const t=a.$('#perm').textContent;
  ok('плагин не зарегистрирован в Android: карточка показывает ошибку и версию',t.includes('Нативный модуль не отвечает')&&t.includes('not implemented')&&t.includes('0.3.0'),t.slice(0,170));}
 // 4b. Внедрённого JS нет, но плагин в Android есть → запасной путь через nativePromise тоже работает
 {const a=make(base,t0,{noPlugin:true});await a.flush();await a.flush();
  ok('без внедрённого JS приложение всё равно достучалось до плагина (запасной путь)',a.ev('nat.ok')===true&&a.calls('sync').length>0);}
 // 5. Обычный браузер (не приложение) — карточки нет
 {const a=make(base,t0,{web:true});await a.flush();
  ok('в браузере (не Capacitor) карточка не показывается',a.$('#perm').textContent.trim()==='');}

 // 6. Новое в 0.3.0: блок наверху, автозапрос разрешений, тревога, возврат в приложение
 {const a=make(base,t0);await a.flush();await a.flush();
  const perm=a.$('#perm'),start=[...a.w.document.querySelectorAll('button')].find(b=>b.textContent.includes('Начать урок'));
  ok('блок «Напоминания в фоне» стоит ВЫШЕ кнопки «Начать урок»',!!(perm.compareDocumentPosition(start)&4));
  ok('на главной видна версия приложения',a.$('#app').textContent.includes('Бит v0.3.0'));
  a.w.document.querySelector('#perm [onclick="togglePerm()"]').click();
  ok('«Подробнее» раскрывает карточку (модель телефона, версия)',a.$('#perm').textContent.includes('Redmi Note 12')&&a.$('#perm').textContent.includes('версия 0.3.0'));
  a.click('Тревога-уведомления');await a.flush();
  const last=a.calls('sync').slice(-1)[0].o;
  ok('выключатель тревоги: в службу ушло alarm:false',last.alarm===false,JSON.stringify(last));
  ok('в карточке написано «выкл»',a.$('#perm').textContent.includes('каждые 0,7 с): выкл'));
  a.click('Тревога-уведомления');await a.flush();
  ok('включили обратно: alarm:true',a.calls('sync').slice(-1)[0].o.alarm===true);}
 {const a=make(base,t0,{native:{overlay:false,notifications:false}});await a.flush();await a.flush();a.tick(3000);await a.flush();
  ok('при запуске приложение САМО запросило уведомления',a.calls('requestNotifications').length===1);
  ok('при запуске приложение САМО предложило разрешение «поверх приложений» и открыло настройки',a.calls('confirm').length===1&&a.calls('requestOverlay').length===1,'confirm='+a.calls('confirm').length+' requestOverlay='+a.calls('requestOverlay').length);
  a.w.dispatchEvent(new a.w.Event('focus'));await a.flush();a.tick(3000);await a.flush();
  ok('повторно за один запуск не навязывается',a.calls('confirm').length===1&&a.calls('requestNotifications').length===1);}
 {const a=make(base,t0,{confirmAns:false,native:{overlay:false}});await a.flush();await a.flush();a.tick(3000);await a.flush();
  ok('отказался от разрешения — настройки не открываются, кнопка в карточке остаётся',a.calls('requestOverlay').length===0&&a.has('Разрешить показ поверх'));}
 {const a=make(base,t0,{native:{alarming:true}});await a.flush();await a.flush();
  a.w.dispatchEvent(new a.w.Event('focus'));await a.flush();await a.flush();
  ok('открыл приложение во время тревоги — сразу выбор урока',a.$('h2')&&a.$('h2').textContent.includes('Как выберем'),a.$('h2')&&a.$('h2').textContent);
  ok('и в службу ушло busy:true (тревога замолкает)',a.calls('sync').some(c=>c.o.busy===true));}
 {const a=make(base,t0,{native:{overlay:false}});await a.flush();await a.flush();a.tick(3000);await a.flush();
  a.click('Демо');await a.flush();a.tick(6500);await a.flush();
  ok('без разрешения запасной веб-баннер работает',a.ban()==='s1',a.ban());}
 console.log(fails?`\nПРОВАЛОВ: ${fails}`:'\nВСЁ ОК');process.exit(fails?1:0);
})();
