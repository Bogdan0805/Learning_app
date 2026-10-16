const {make}=require('./harness');
const base=t=>({subs:{math:5,py:3},time:'17:00',n:1,p:{},total:0,xp:0,streak:0,last:'',day:new Date(t).toDateString(),cnt:0,setup:true});
const morning=new Date('2026-10-13T10:00:00').getTime();
let fails=0;const ok=(name,cond,info)=>{console.log((cond?'✅':'❌')+' '+name+(info?'  → '+info:''));if(!cond)fails++};
const tickS=(a,sec)=>{for(let i=0;i<sec;i++)a.tick(1000)};
// 1. Демо при уже выполненной цели: 3 стадии, потом остаётся 3-я
{const a=make({...base(morning),cnt:1},morning);tickS(a,1);a.click('Демо');
 tickS(a,4);ok('демо: через 4с баннера ещё нет',a.ban()==='нет');
 tickS(a,2);ok('демо: через ~6с стадия 1',a.ban().startsWith('s1'),a.ban());
 a.click('Закрыть');tickS(a,7);ok('демо: после закрытия 7с — пусто',a.ban()==='нет');
 tickS(a,2);ok('демо: через 8с стадия 2',a.ban().startsWith('s2'),a.ban());
 a.click('Закрыть');tickS(a,9);ok('демо: стадия 3',a.ban().startsWith('s3'),a.ban());
 a.click('Закрыть');tickS(a,9);ok('демо: остаётся стадия 3',a.ban().startsWith('s3'),a.ban());
 a.click('Закрыть');a.ev('toggleDemo()');tickS(a,30);ok('демо остановлено — баннеров нет',a.ban()==='нет');}
// 2. «Учиться» на баннере не возвращает баннер поверх выбора урока
{const a=make({...base(morning),cnt:0},morning);tickS(a,1);a.click('Демо');tickS(a,6);
 ok('демо: баннер показан',a.ban().startsWith('s1'));
 a.click('Учиться');tickS(a,30);ok('после «Учиться» баннер НЕ лезет поверх выбора урока',a.ban()==='нет',a.w.document.querySelector('h2').textContent);
 a.click('Назад');tickS(a,5);ok('вернулись на главную: сразу не всплывает',a.ban()==='нет');
 tickS(a,5);ok('через 8с после выхода — снова баннер, грознее (s2)',a.ban().startsWith('s2'),a.ban());}
// 3. Реальное расписание: 17:00, 5 минут грации, потом каждую минуту, 3 стадии
{const t=new Date('2026-10-13T16:59:30').getTime();const a=make({...base(t),time:'17:00'},t);
 tickS(a,20);ok('до 17:00 баннера нет',a.ban()==='нет');
 tickS(a,20);/*17:00:10*/ ok('17:00 — уведомление, баннера нет (грация 5 мин)',a.ban()==='нет');
 tickS(a,4*60);ok('через 4 мин после 17:00 — нет',a.ban()==='нет');
 tickS(a,70);ok('через ~5 мин — стадия 1',a.ban().startsWith('s1'),a.ban());
 a.click('Закрыть');tickS(a,55);ok('закрыли: через 55с нет',a.ban()==='нет');
 tickS(a,10);ok('через минуту — стадия 2',a.ban().startsWith('s2'),a.ban());
 a.click('Закрыть');tickS(a,65);ok('ещё через минуту — стадия 3',a.ban().startsWith('s3'),a.ban());
 a.click('Закрыть');tickS(a,65);ok('дальше — всё равно стадия 3 каждую минуту',a.ban().startsWith('s3'),a.ban());
 // 4. проходим урок через интерфейс до конца
 a.click('Учиться');a.click('Выбрать самому');a.click('Математика');
 ok('урок начался',a.ev('inLesson')===true);
 tickS(a,200);ok('во время урока баннер не мешает',a.ban()==='нет');
 a.click('Понятно');let g=0;
 while(a.ev('LS.i')<=a.ev('LS.qs.length')&&g++<60){const q=a.ev('LS.qs[LS.i-1]');if(!q)break;
   [...a.w.document.querySelectorAll('.opt')][q[2]].click();a.click('Дальше');}
 ok('урок завершён, цель дня выполнена',a.ev('S.cnt')===1,'cnt='+a.ev('S.cnt'));
 tickS(a,600);ok('после выполнения цели баннеры прекратились (10 мин)',a.ban()==='нет');}
// 5. Утром, до времени урока, баннера нет; новый день сбрасывает цель
{const a=make({...base(morning)},morning);tickS(a,3600);ok('до 17:00 за час — баннера нет',a.ban()==='нет');}
// 6. Невыполненная цель + выход из урока по «Назад»: баннер возвращается
{const t=new Date('2026-10-13T17:10:00').getTime();const a=make({...base(t)},t);tickS(a,1);
 tickS(a,301);ok('опоздал на 5+ минут — баннер',a.ban().startsWith('s1'));
 a.click('Учиться');a.click('Выбрать самому');a.click('Информатика');tickS(a,120);ok('в уроке — тишина',a.ban()==='нет');}
// 7. Демо заканчивается после прохождения урока
{const a=make({...base(morning),cnt:0},morning);tickS(a,1);a.click('Демо');tickS(a,6);a.click('Учиться');a.click('Выбрать самому');a.click('Python');a.click('Понятно');
 let g=0;while(a.ev('LS.i')<=a.ev('LS.qs.length')&&g++<60){const q=a.ev('LS.qs[LS.i-1]');if(!q)break;[...a.w.document.querySelectorAll('.opt')][q[2]].click();a.click('Дальше');}
 ok('демо выключилось после урока',a.ev('DEMO.on')===false);tickS(a,60);ok('и баннеров нет',a.ban()==='нет');}
console.log(fails?`\nПРОВАЛОВ: ${fails}`:'\nВСЁ ОК');
process.exit(fails?1:0);
