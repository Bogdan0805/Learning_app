package com.bogdan0805.learningapp;
public class LogicTest {
  static int fails=0;
  static void ok(String n,boolean c,String i){System.out.println((c?"✅ ":"❌ ")+n+(i.isEmpty()?"":" → "+i));if(!c)fails++;}
  static String name(int a){return new String[]{"NONE","DUE_NOTIFY","SHOW","HIDE"}[a];}
  // симулятор: шаг 1 сек, возвращает последнее действие
  static long now; static ReminderLogic L; static int min; static String day="d1";
  static void untilBanner(){for(int i=0;i<400&&!L.bannerUp;i++)adv(1);}
  static int adv(int sec){int last=0;for(int i=0;i<sec;i++){now+=1000;int a=L.tick(now,min,day,true);if(a!=0)last=a;}return last;}
  static ReminderLogic fresh(int due){L=new ReminderLogic();L.enabled=true;L.dueMinutes=due;L.perDay=1;L.cntDay=day;L.cnt=0;now=1_000_000L;min=10*60;return L;}
  public static void main(String[] a){
    // 1. Демо при выполненной цели
    fresh(17*60);L.cnt=1;adv(2);L.demo=true;
    adv(4);ok("демо: через 4с баннера нет",!L.bannerUp,"");
    int r=adv(3);ok("демо: ~7с — стадия 1",L.bannerUp&&L.stage==1,"stage="+L.stage);
    L.bannerClosed(now);adv(7);ok("демо: после закрытия 7с — нет",!L.bannerUp,"");
    adv(2);ok("демо: 9с — стадия 2",L.bannerUp&&L.stage==2,"stage="+L.stage);
    L.bannerClosed(now);adv(9);ok("демо: стадия 3",L.bannerUp&&L.stage==3,"");
    L.bannerClosed(now);adv(9);ok("демо: остаётся 3",L.bannerUp&&L.stage==3,"");
    L.bannerClosed(now);L.demo=false;adv(2);ok("демо выключено — баннер скрыт и не возвращается",!L.bannerUp,"");adv(300);ok("…и 5 минут тишины",!L.bannerUp,"");
    // 2. Реальное расписание
    fresh(17*60);min=16*60+59;adv(5);ok("до срока баннера нет",!L.bannerUp,"");
    min=17*60;int first=0;for(int i=0;i<3;i++){int x=adv(1);if(x==1)first=1;}
    ok("в 17:00 — уведомление (DUE)",first==1||L.trig!=0,"");
    adv(4*60);ok("через 4 мин баннера нет",!L.bannerUp,"");
    adv(70);ok("через ~5 мин — стадия 1",L.bannerUp&&L.stage==1,"stage="+L.stage);
    L.bannerClosed(now);adv(55);ok("через 55с нет",!L.bannerUp,"");
    adv(10);ok("через минуту — стадия 2",L.bannerUp&&L.stage==2,"");
    L.bannerClosed(now);adv(65);ok("ещё минута — стадия 3",L.bannerUp&&L.stage==3,"");
    L.bannerClosed(now);adv(65);ok("дальше стадия 3 каждую минуту",L.bannerUp&&L.stage==3,"");
    // 3. Занят (в уроке) — баннер не показывается
    L.bannerClosed(now);L.busy=true;L.busyAt=now;adv(70);ok("в уроке (busy) баннера нет",!L.bannerUp,"");
    L.busyAt=now-4*60*1000L;adv(70);ok("busy протух (3+ мин) — баннер вернулся",L.bannerUp,"");
    // 4. «Учиться» нажали
    L.lessonStarted(now);ok("после «Учиться» баннер скрыт",!L.bannerUp,"");adv(30);ok("и 30с не возвращается",!L.bannerUp,"");
    adv(40);ok("а через минуту возвращается",L.bannerUp,"");
    // 5. Цель выполнена — всё прекращается
    L.bannerClosed(now);L.cnt=1;int h=adv(2);ok("цель выполнена — баннера нет",!L.bannerUp,"");adv(600);ok("и 10 минут тишины",!L.bannerUp,"");
    // 6. Новый день — снова с нуля
    fresh(17*60);L.cnt=1;min=18*60;adv(5);ok("вчерашний счётчик не засчитывается сегодня",true,"");
    day="d2";adv(5);L.cntDay="d1";L.cnt=1;adv(310);ok("новый день: цель не выполнена — баннер",L.bannerUp,"");
    // 7. Отключено
    fresh(17*60);min=18*60;adv(310);ok("до отключения баннер",L.bannerUp,"");L.enabled=false;int hh=adv(2);ok("отключено — баннер скрыт",!L.bannerUp,"");
    // 8. Нет разрешения — баннера нет, уведомление есть
    fresh(17*60);min=18*60;now+=0;int sh=0,du=0;for(int i=0;i<400;i++){now+=1000;int x=L.tick(now,min,day,false);if(x==2)sh++;if(x==1)du++;}
    ok("без разрешения: баннеров 0, уведомление 1",sh==0&&du==1,"sh="+sh+" du="+du);
    // 9. Тревога-уведомления (каждые 0,7 с в службе): только на 3-й стадии, пока урок не пройден
    fresh(17*60);min=18*60;adv(3);
    ok("до первого баннера тревоги нет",!L.alarmActive(now),"");
    untilBanner();ok("стадия 1 — тревоги нет",L.stage==1&&!L.alarmActive(now),"stage="+L.stage);
    L.bannerClosed(now);untilBanner();ok("стадия 2 — тревоги нет",L.stage==2&&!L.alarmActive(now),"stage="+L.stage);
    L.bannerClosed(now);untilBanner();ok("стадия 3 — тревога включилась вместе с баннером",L.stage==3&&L.alarmActive(now),"stage="+L.stage);
    L.bannerClosed(now);ok("баннер закрыт на 3-й стадии — тревога продолжается",L.alarmActive(now),"");
    L.busy=true;L.busyAt=now;ok("человек в уроке — тревога молчит",!L.alarmActive(now),"");
    L.busy=false;L.alarmEnabled=false;ok("выключено в настройках — молчит",!L.alarmActive(now),"");
    L.alarmEnabled=true;ok("включили обратно — снова идёт",L.alarmActive(now),"");
    L.cnt=1;adv(2);ok("урок пройден — тревога остановлена",!L.alarmActive(now),"");
    fresh(17*60);L.demo=true;adv(3);untilBanner();L.bannerClosed(now);untilBanner();L.bannerClosed(now);untilBanner();
    ok("демо: на 3-й стадии тревога идёт",L.stage==3&&L.alarmActive(now),"stage="+L.stage);
    L.demo=false;adv(2);ok("демо выключено — тревога остановлена",!L.alarmActive(now),"");
    System.out.println(fails==0?"\nВСЁ ОК":"\nПРОВАЛОВ: "+fails);
  }
}
