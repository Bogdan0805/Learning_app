const {JSDOM}=require('jsdom'),FT=require('@sinonjs/fake-timers'),fs=require('fs');
const TH=fs.readFileSync(__dirname+'/../www/theory.js','utf8');
const html=fs.readFileSync(__dirname+'/../www/index.html','utf8').replace('<script src="theory.js"></script>','<script>'+TH+'</script>');
let fails=0;const ok=(n,c,i)=>{console.log((c?'✅':'❌')+' '+n+(i?'  → '+i:''));if(!c)fails++};
// --- 1. полнота и аккуратность теории ---
const w0={};new Function('window',TH)(w0);const F=w0.THEORY_FULL;
const ids=['math','rus','inf','phy','eng','alg','geo','py'];let miss=[],bad=[],total=0,short=[];
for(const id of ids)for(let k=0;k<8;k++){const key=id+k,a=F[key];if(!a){miss.push(key);continue}
  if(a.length!==4||a.some(x=>typeof x!=='string'||x.trim().length<20))bad.push(key+': поля');
  const all=a.join('');total+=all.length;
  if(/undefined|\$\{|NaN/.test(all))bad.push(key+': мусор');
  for(const t of ['b','i'])if((all.match(new RegExp('<'+t+'>','g'))||[]).length!==(all.match(new RegExp('</'+t+'>','g'))||[]).length)bad.push(key+': не закрыт <'+t+'>');
  if(a[0].length<350)short.push(key+'('+a[0].length+')')}
ok('подробная теория есть для всех 64 уроков',miss.length===0,miss.join(','));
ok('все 4 поля заполнены, теги закрыты, нет мусора',bad.length===0,bad.join(' | '));
ok('правила в каждом уроке достаточно подробные (≥350 символов)',short.length===0,short.join(', '));
console.log('   всего текста теории:',total,'символов,',Math.round(total/64),'на урок');
// --- 2. неверный ответ в уроке ---
const t0=new Date('2026-10-13T10:00:00').getTime();
const base={subs:{math:5,py:3},time:'17:00',n:1,p:{},total:0,xp:0,streak:0,last:'',day:new Date(t0).toDateString(),cnt:0,setup:true};
function make(state){const dom=new JSDOM(html,{runScripts:'dangerously',url:'http://localhost/',pretendToBeVisual:true,beforeParse(w){w.localStorage.setItem('bit',JSON.stringify(state));w.alert=()=>{};w.HTMLCanvasElement.prototype.getContext=()=>new Proxy({},{get:()=>()=>{},set:()=>true});w.clock=FT.withGlobal(w).install({now:t0,toFake:['Date','setInterval','clearInterval','setTimeout','clearTimeout']})}});
 const w=dom.window,$=s=>w.document.querySelector(s);
 return {w,$,click:t=>{const b=[...w.document.querySelectorAll('button')].find(x=>x.textContent.includes(t));if(!b)throw new Error('нет кнопки '+t);b.click()},has:t=>[...w.document.querySelectorAll('button')].some(x=>x.textContent.includes(t)),ev:c=>w.eval(c),
  opts:()=>[...w.document.querySelectorAll('.opt')]}}
{const a=make(base);a.click('Начать урок');a.click('Выбрать самому');a.click('Математика');
 ok('в теории урока видно подробное описание и «Частую ошибку»',a.$('#app').textContent.includes('Частая ошибка')&&a.$('#app').textContent.includes('Примеры'));
 a.click('Понятно');
 const q=a.ev('LS.qs[LS.i-1]'),right=q[2],wrong=[0,1,2,3].filter(x=>x!==right&&a.opts()[x]);
 ok('до ошибки кнопки «Посмотреть тему» нет',!a.has('Посмотреть тему'));
 a.opts()[wrong[0]].click();
 const fb=a.$('#fb').textContent;
 ok('после неверного ответа: «Осталось попыток: 2 из 3»',fb.includes('Осталось попыток: 2 из 3'),fb.slice(0,70));
 ok('после неверного ответа есть кнопка «Посмотреть тему и правила»',a.has('Посмотреть тему и правила'));
 ok('сердечек стало 2',a.$('#hp').textContent==='❤️❤️🖤');
 a.click('Посмотреть тему и правила');
 ok('открылась панель с темой и правилами урока',a.$('#thv').style.display==='block'&&a.$('#thv').textContent.includes('Частая ошибка')&&a.$('#thv').textContent.includes(a.ev('LS.title')));
 ok('вопрос и выбранный неверный вариант не потерялись',a.opts()[wrong[0]].disabled===true&&a.$('#hp').textContent==='❤️❤️🖤');
 a.click('Вернуться к вопросу');
 ok('панель закрылась, можно отвечать дальше',a.$('#thv').style.display==='none'&&!a.opts()[right].disabled);
 a.opts()[wrong[1]].click();
 ok('вторая ошибка: осталась 1 попытка',a.$('#fb').textContent.includes('Осталось попыток: 1 из 3'));
 a.opts()[wrong[2]].click();
 ok('третья ошибка: показано решение и кнопка темы всё ещё доступна',a.$('#fb').textContent.includes('Решение')&&a.has('Посмотреть тему и правила')&&a.has('Дальше'));
 a.click('Дальше');
 ok('следующий вопрос: сердечки восстановились, подсказка скрыта',a.$('#hp').textContent==='❤️❤️❤️'&&!a.has('Посмотреть тему'));
 // правильный ответ со второй попытки
 const q2=a.ev('LS.qs[LS.i-1]'),r2=q2[2],w2=[0,1,2,3].find(x=>x!==r2&&a.opts()[x]);a.opts()[w2].click();a.opts()[r2].click();
 ok('правильный ответ со 2-й попытки засчитывается как «Верно», но не как «с первой попытки»',a.$('#fb').textContent.includes('Верно')&&a.ev('LS.ok')===0,'LS.ok='+a.ev('LS.ok'));}
// --- 3. повторение: без подсказок по теории ---
{const a=make({...base,p:{math:3},total:3});a.click('Начать урок');a.click('Выбрать самому');a.click('Математика');
 ok('это урок-повторение',a.ev('LS.review')===true);a.click('Понятно');
 const q=a.ev('LS.qs[LS.i-1]'),wr=[0,1,2,3].find(x=>x!==q[2]&&a.opts()[x]);a.opts()[wr].click();
 ok('в повторении после ошибки попытки есть, а кнопки темы нет (без подсказок)',a.$('#fb').textContent.includes('Осталось попыток')&&!a.has('Посмотреть тему'));}
console.log(fails?`\nПРОВАЛОВ: ${fails}`:'\nВСЁ ОК');
process.exit(fails?1:0);
