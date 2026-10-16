const {JSDOM}=require('jsdom'),FT=require('@sinonjs/fake-timers'),fs=require('fs');
const THEORY_INLINE=fs.readFileSync(__dirname+'/../www/theory.js','utf8');
const html=fs.readFileSync(__dirname+'/../www/index.html','utf8').replace('<script src="theory.js"></script>','<script>'+THEORY_INLINE.replace(/<\/script>/g,'')+'</script>');
exports.make=(state,startMs)=>{
 const dom=new JSDOM(html,{runScripts:'dangerously',url:'http://localhost/',pretendToBeVisual:true,
  beforeParse(w){w.localStorage.setItem('bit',JSON.stringify(state));w.alert=()=>{};
   w.clock=FT.withGlobal(w).install({now:startMs,toFake:['Date','setInterval','clearInterval','setTimeout','clearTimeout']});}});
 const w=dom.window,$=s=>w.document.querySelector(s);
 return {w,$,tick:ms=>w.clock.tick(ms),
  ban:()=>{const b=$('#ban');return b.style.display==='flex'?b.className+' | '+b.querySelector('h1').textContent:'нет'},
  click:txt=>{const b=[...w.document.querySelectorAll('button')].find(x=>x.textContent.includes(txt));if(!b)throw new Error('нет кнопки '+txt);b.click()},
  ev:c=>w.eval(c)};
};
