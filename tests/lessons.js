const fs=require('fs'),vm=require('vm');
const html=fs.readFileSync(__dirname+'/../www/index.html','utf8');
const js=html.match(/<script>([\s\S]*)<\/script>/)[1];
const el=()=>({style:{},innerHTML:'',textContent:'',classList:{add(){}},getContext:()=>({}),addEventListener(){}});
const ctx={window:{addEventListener(){}},document:{querySelector:el,querySelectorAll:()=>[],addEventListener(){},hidden:false},localStorage:{getItem:()=>null,setItem(){}},Notification:{permission:'denied'},setInterval(){},performance:{now:()=>0},requestAnimationFrame(){},alert(){},console};
vm.createContext(ctx);vm.runInContext(fs.readFileSync(__dirname+'/../www/theory.js','utf8'),ctx);
vm.runInContext(js,ctx);
const out=vm.runInContext(`(()=>{
const bad=[],info=[];
for(const id of ids){const L=D[id].L;
 for(let k=0;k<L.length;k++){
  if(!THEORY[id+k]&&!L[k][1])bad.push(id+k+': нет теории');
  let minN=99,min3=0;
  for(let rep=0;rep<150;rep++){
   const qs=genQs(id,k);minN=Math.min(minN,qs.length);
   for(const q of qs){
    const txt=JSON.stringify(q);
    if(/NaN|undefined|Infinity|null/.test(txt))bad.push(id+k+': мусор '+txt);
    if(!Array.isArray(q[1])||q[1].length<2)bad.push(id+k+': <2 вариантов '+q[0]);
    else{
     if(new Set(q[1]).size!==q[1].length)bad.push(id+k+': дубли '+txt);
     if(!(q[2]>=0&&q[2]<q[1].length))bad.push(id+k+': индекс '+txt);
     if(q[1].length<3)min3++;
    }
    if(typeof q[3]!=='string'||!q[3])bad.push(id+k+': нет решения '+q[0]);
   }
  }
  info.push(id+k+' '+L[k][0]+' | вопросов мин: '+minN);
 }}
return {bad:[...new Set(bad)].slice(0,40),nbad:bad.length,info};
})()`,ctx);
console.log(out.info.join('\n'));
console.log('ПРОБЛЕМ:',out.nbad);console.log(out.bad.join('\n'));if(out.nbad)process.exitCode=1;
const s=vm.runInContext(`(()=>{const r=[];
for(const [id,ks] of [['math',[2,3,4,5,6,7]],['inf',[2,3,4,5,6]],['phy',[2,3,4,5,6,7]],['py',[2,3,4,6]]])
 for(const k of ks){const q=genQs(id,k)[0];r.push(id+k+' | '+q[0]+' => '+q[1][q[2]]+' ('+q[3]+')')}
// прогон целого урока
for(const id of ids)for(let p=0;p<8;p++){S.p[id]=p;S.total=0;begin(id);let g=0;while(LS.i<LS.qs.length+1&&g++<50){step();if(LS.i>=1&&LS.i<=LS.qs.length){}}}
r.push('прогон begin/step: ок');return r})()`,ctx);
console.log(s.join('\n'));
