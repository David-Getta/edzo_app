# Grit 🐺🔥

Teljes értékű **edzőtárs Androidra**: intervallum (HIIT) időzítő, futáskövetés,
erősítő napló, **étrend- és vízkövetés**, nyújtás & mobilitás, játékos motiváció
és részletes statisztikák. Karmazsin/bordó megjelenés, saját gyártású
kabalafigurával (**Blaze**), generált háttérképekkel és finom animációkkal.
Tiszta natív Android (Java, programozott felület, külső függőségek nélkül).

## Funkciók

### ⏱️ Edzés & időzítő
- Testre szabható **Bemelegítés / Előkészület / Futás / Pihenő / Körök /
  Levezetés** – másodpercre pontosan
- **Élő folyamatsáv**, világító körgyűrű, 3-2-1 visszaszámlálás, ünneplő pipa,
  egygombos újraindítás ugyanarra az edzésre
- **Beállítás mondatból**: „3 kör 40 mp munka 20 mp pihenő”, „8x20/10",
  „tabata”, „emom 10 perc”, „norvég 4x4”, „20 perc alatt 40/20”, „négy kör 30 másodperc”,
  „20/10 nyolcszor”, „2 perc bemelegítés, 6 kör 40/20”,
  „1:30 munka 0:30 pihenő 6 kör” (edzőtermi tábla-írásmód),
  „hiit 20 perc, 30/30” és „15 perc, 40 mp munka 20 mp pihenő” (az egyetlen
  perc-adat a terv hossza – a körszám kijön belőle),
  „40 mp / 20 mp, 10 kör”, „emom 12”, „10x(40s/20s)”,
  „8 rounds 20 sec work 10 sec rest” (az internetről másolt angol terv),
  „négyszer negyven másodperc munka húsz másodperc pihenő” – a teremben kapott edzésterv szövegként érkezik, nem
  csúszkaállásként; egy koppintással sablonként is elmenthető
- 📅 **Edzésnap megosztása**: „Lábnap: Guggolás, Lábtolás, Kitörés" – az
  edzőtől kapott nap egy koppintással bekerül a saját edzésnapjaid közé
- 📤 **Bejegyzés megosztása**: az erősítő napló egy bejegyzése („guggolás
  3x10 60 kg") és az időzítő-sablon is küldhető szövegként – a másik telefonon
  ugyanez a felismerő teszi a helyére
- 📤 **Sablon megosztása**: a mentett sablon szövegként küldhető tovább
  („Tabata: 8 kör 20 mp munka 10 mp pihenő"), és a másik telefonon ugyanez a
  felismerő állítja vissza – a Grit ott van a megosztás-listában
- Gyors sablonok (**HIIT / Tempó / Tabata**) és **10 beépített program**
  (törzs, teljes test, láb, kar, zsírégető HIIT, 7 perces edzés…), saját
  programokkal és gyakorlat-leírásokkal – a saját programhoz a gyakorlatok
  a könyvtárból is választhatók, így mindig lesz mellettük technikai tipp
- 🔊 Hangválasztás, 🗣️ hangos bemondás (TTS), 🎛️ vezérlés az értesítésből,
  🌙 fut kikapcsolt képernyővel is (foreground service + wake lock)

### 🏃 Futás & mérés
- **GPS táv, tempó, lépések, kalória**, átlag/max sebesség
- Útvonal, kör-splitek és sebesség-diagram az edzés részleteinél

### 🧾 Amit NEM ír be a naplóba
Egy fitnesz-app legcsendesebb hibája, ha olyat rögzít, ami meg sem történt:
azt senki nem veszi észre, de a széria, az XP, a heti terhelés, a kalóriacél
és a progresszió-javaslat mind abból számol tovább. Ezért mindegyik felismerő
ugyanazt az elvet követi – **a mondatnak MEGTÖRTÉNTRŐL kell szólnia**:
- a **terv** nem napló: „holnap futok", „el kéne menni futni", „a terv:
  guggolás 5x5 100 kg", „ha lesz idő, futok egyet"
- a **vágy** nem vacsora: „szeretnék egy pizzát", „jó lenne egy sör"
- a **bevásárlás** és a **kuka** sem étkezés: „vettem két kiló almát",
  „kidobtam a maradék rizst"
- a **cél** nem mérés: „a cél 75 kg", „szeretném, ha 50 lenne a nyugalmi
  pulzusom"
- az **időpont** nem tevékenység: az „edzés után ittam egy turmixot" a
  turmixról szól, nem egy edzésről
- a **szokás** nem egy alkalom: „hetente háromszor járok kondiba", „minden
  másodnap futok", „szoktam futni reggelente" a heti rendet írja le. A múlt
  idő viszont napló, gyakorisággal együtt is: „hetente kétszer futottam"
- a **felszólítás** terv: „csináljunk egy tabatát", „menjünk futni" – az
  időzítő-terv elkészül, a bejegyzés nem
- a **cél** és a **nevezés** sem edzés: „a heti célom 4 edzés", „célom a
  100 kg-os fekvenyomás", „beneveztem egy félmaratonra"
- a **feltételes mondat** múlt idejű ige nélkül terv: „ha esik, futópadon
  futok". A múlt idő megvédi a valódi bejegyzést: „ha jól emlékszem, 5 km-t
  futottam tegnap"
- a **kimondott akarat** sem edzés: „erősíteni akarom a bokám", „el akarok
  kezdeni futni", „remélem tudok futni 30 percet", „készülök elmenni a terembe"
- a **bevásárlólista** és a **kifogyott étel** sem vacsora: „bevásárlólista:
  pizza, tej", „kifogyott a tej", „el kell tennem a maradékot", „receptet
  keresek", „megkívántam a csokit", „rendelnék egy pizzát"
- ami **nem az én mozgásom**, az nem az én naplóm: „a gyerek edzésén voltam",
  „home office, alig mozogtam" (az „alig" viszont csak a mozgás-igével együtt
  tagadás – az „alig bírtam végigcsinálni a 30 perc futást" megtörtént edzés)
- ami **másé**, az nem az enyém, akkor sem, ha a tagadás a mondat másik
  felében áll: „a fiam focizott, én csak néztem", „a párom jógázott, én
  addig főztem". Harmadik személyű alany + első személyű ige nélküli
  tagmondat = nem az én naplóm. A „fiammal futottam" és „a fiam és én
  futottunk" viszont megtörtént – velem
- a **panasz** nem gyakorlat: a „kaptam egy húzódást a vádlimba" nem
  vádliemelés. Ami mégis megtörtént, az marad: „fájt a vállam, mégis
  fekvenyomás 3x8 60 kg"
- a **szokás kezdete** nem egy edzés: a „három hónapja kezdtem el edzeni"
  arról szól, mióta sportol az ember – de a „két hete kezdtem el futni,
  **azóta 40 km-t futottam**" negyven kilométere valódi
- a **heti beosztás** terv: a „hétfő mell és tricepsz, kedd hát és bicepsz"
  azt írja le, mikor mit edz az ember – eddig egy tricepsz-gyakorlat lett
  belőle hat ismétléssel (a „hát" számnévként hattá vált). A „push pull legs,
  heti 6 edzés" hat bejegyzést csinált. Kimondott sorozat, súly vagy múlt idő
  megvédi a valódi naplót: „hétfőn guggolás 3x5, szerdán 4x8 60 kg"
- a **kihagyás beszámolója** sem étkezés: a „3 hetet bírtam ki cukor nélkül"
  büszkeség, nem adag – eddig háromszázharminc gramm cukormentes étel lett
  belőle
- a **tagadott evés-ige** nem menti fel a bevásárlást: a „vettem egy kiflit,
  **de nem ettem meg**" mondatban ott az „ettem", és eddig ettől bejegyzés lett
- a **tagadás** végig érvényes: „ma nem ettem csokit", „nem fáj már a térdem" –
  a **„mégsem"** ugyanígy: „mégsem futottam ma", „mégsem ettem a csokit"

Az evés-ige mindig felülír: a „megkívántam a csokit **és megettem**" valódi
bejegyzés. Tizenkilenc szándék-alakot ötféle edzéssel és tizenöt alakot húsz
étellel végigpróbálva egyikből sem lesz napló-sor.

### 🧩 Ami a mondat MÁSIK felében van
Az ellenkező hiba ugyanolyan csendes: a mondat egyik fele bekerül, a másik
nyomtalanul eltűnik. A „futottam 30 percet **és ettem egy banánt**" banánja
eddig sehol nem jelent meg – az app most megkérdezi, viszi-e az Étrendbe,
és fordítva is: a mentett étkezés után felajánlja az edzést. A Profil
mondat-mezője pedig egyszerre menti a súlyt, az alvást és a pulzust.

A mennyiség is a másik felében lehet: az „ebédre töltött káposzta volt,
**két adag**" eddig egy adagot mentett kettő helyett. Az utolsó, csupán
mennyiséget tartalmazó tagmondat nem lehet másé – de csak akkor lép be, ha a
mondat elején pontosan egy étel áll („csirkemell rizzsel, 200 g" marad
kétértelmű, ott nem találgatunk).

A **sorozat mellé írt táv** ugyanígy: a „reggel **5 km futás**, utána 20
fekvőtámasz" mondatra eddig választani kellett – vagy az erősítő napló kapta
meg (és a kilométerek eltűntek), vagy sima edzésként ment be (és a súly meg az
ismétlés veszett el). Most van harmadik gomb: mindkettő. Az edzés-előzményekbe
csak a kimondott táv és lépésszám megy át, mert a fekvőtámaszból becsült
„kondi" perc kétszer számítana.

A kimondott **összeg** nem külön edzés: a „ma 90 percet edzettem összesen:
30 perc kondi, 60 perc futás" kilencvenese a másik két szám összege – eddig
harmadikként állt sorba, és százötven perc mozgás került a naplóba kilencven
helyett.

Az **ismétlésszám sem alkalomszám**: a „20 kettlebell swing és 10 burpee"
húszasa a lendítések száma, a kettlebell viszont kondi-szótő is – eddig húsz
darab hatvanperces edzés lett belőle, húsz napra szétosztva. Ha a mondatban
felismert sorozat is van, és a szám pontosan annak az ismétlésszáma, a szám
azé. A „2 fekvőtámasz edzés" viszont marad két edzés.

A **tiltó szó** is csak a saját tagmondatát viszi el: a „ma 2100 kcal-t
ettem, **elégettem 600-at**" mindkét számot kimondja, de az „elégettem" eddig
az egész mondatot elnémította – a kétezer-száz sehol nem jelent meg. És a
puszta fehérje is étkezés: a „120 g fehérjét vittem be ma" mondatban egyetlen
ismert étel sincs, mégis a naplóba való.

Az edzés és az étkezés mellé a reggeli **mérés** is odaférhet: a „10 km futás,
**78,5 kg a mérlegen**", az „aludtam 7 órát **és futottam 10 km-t**" és az
„ettem egy pizzát **és aludtam 9 órát**" második fele eddig nyomtalanul
eltűnt.

Szándékosan szűk a lista: csak azok a párok, amelyek nem eshetnek egymás
rovására. Huszonkét valódi edzés-mondaton az étel-felismerő egyetlen ételt
sem talált, tehát ha talál, az tényleg ott van.

### 📝 Kézi edzés-felvétel
- **Olyan edzés is naplózható, amit nem a telefon mért**: kézilabda, úszás,
  kondi, foci, tenisz, jóga, kenu, búvárkodás, salsa, barlangászat… –
  20 mozgásforma, sportág szerinti
  kalóriabecsléssel (MET × testsúly × idő)
- **Egy mondatból akár többet is**: „az elmúlt 3 nap alatt 3 futó edzés és
  6 kézi edzés", „10 km futás", „tegnap 1,5 óra bringa", „a héten minden
  nap futottam", „hétvégén 1-1 túra", „hétfőn és szerdán kondi", „kétszer úsztam",
  „futottam háromszor a héten" (a szorzószám hátul is állhat),
  „múlt kedden kondi", „tegnap hajnalban futás", „5 napja futottam",
  „két hete kondi" (a birtokos alak IDŐPONT, nem időszak), „kétórás túra",
  „reggel 5 km futás, este 8 km futás" (két külön edzés), „leúsztam 1500
  métert", „tegnap este kondi", „ma 10000 lépés", „10k lépés" és „10 000
  lépés" (a rövidítés és a szóközös ezres tagolás is), „10k futás" (tíz
  kilométer), „ma reggel 6-kor futottam"
  (a kimondott óra pontosabb a napszaknál), „július 28-án
  futottam", „100 fekvőtámasz", „júl. 28-án 6x1 km", „napi 20 perc jóga
  egész héten", „1h20 futás", „45p nyújtás", „hetvenöt perc kondi",
  „száz fekvőtámasz", „huszonöt kilométer bringa", „40 hosszt úsztam"
  (egy hossz 25 méter), „10x400 métert futottam" (össztáv, EGY edzés),
  „három negyed óra kondi" (45 perc, nem három edzés),
  „10 km-t futottam 5:30-as tempóval" (a kimondott tempó pontosabb, mint a
  becslés), „futás 1:05:23" (óra-kijelző alak), „kondi és futás, összesen
  másfél óra" (az összesen OSZTJA az időt), „10-15 perc futás" és „5-8 km
  futás" (a tartomány közepe), „reggel és este 30-30 perc kondi" (két edzés),
  „futás és úszás 30-30 perc" (mindkettő harminc perc – órában is: „2-2 óra"),
  „reggel és este is futottam 5-5 km-t" (két ötkilométeres futás),
  „18:00-19:30 foci" és „18:00-tól 19:30-ig kondi" (a naptárból másolt
  óra-tartomány másfél óra), „01.15 futás 8 km" (a sor elején álló szám dátum),
  „a héten futottam 3 alkalommal" (a darabszám hátul is állhat),
  „ma volt az első edzésem 3 hét után, 30 perc futás" (a kihagyás nem időszak)
  – mentés előtt megmutatja, mit értett; a tervekre („jövő
  héten…") őszintén szól, hogy a napló a megtörtént edzéseké, a tagadást
  is érti („ma nem futottam", „kondi helyett futás"; a „nem futottam és
  kondiztam" és a „nem futottam a kondi mellett" kondija viszont
  megmarad), és a pihenőnapot is elfogadja
- **Sorozatos mondat esetén** („3x10 fekvenyomás 60 kg") felajánlja az
  Erősítő naplót, ahol a súly és az ismétlés is megmarad
- A kézi bejegyzés **mindenben egyenrangú a mérttel**: számít a szériába, az
  XP-be, a jelvényekbe, a heti visszatekintőbe és a statisztikába
- Elérhető a kezdőlap **„Edzés pótlása"** csempéjéről és az Előzmények
  „+" gombjáról; utólagos (múltbeli) felvételnél is a helyére kerül

### 🍽️ Étrend
- **Írd le, mit ettél** – az app felismeri az ételeket a mondatból:
  „rántott hús rizzsel", „150 g csirkemell 200 g rizs", „2 tojás", „fél alma"
- **Elgépelésre tipp**: a „joghrut" és a „csirkemel" nem „ezt még nem
  ismerem", hanem „erre gondoltál: Joghurt?" – a felcserélt betű a
  telefonon a leggyakoribb hiba, és egy koppintással javítható
- **A vágy nem vacsora**: a „szeretnék egy pizzát", a „vettem két kiló almát"
  és a „kidobtam a maradék rizst" nem kerül a naplóba – szándék-szó mellett
  evés-ige kell hozzá, így a „vettem egy kávét és megittam" továbbra is
  bejegyzés
- **Érti a tagadást is**: a „chips helyett almát ettem" csak almát naplóz,
  a „csoki nélkül kértem a kávét" csak kávét, a „ma nem ettem csokit" semmit.
  A tagadás az „és"-en át is ér („nem ettem csokit és chipset"), de megáll
  az írásjelnél és egy állító igénél („nem ettem semmit, de ittam kávét"),
  és nem viszi el a kísérőt sem („nem kértem sültkrumplit a hamburger mellé")
- Felismeri a **grammot** (g / gr / gramm / dkg / deka), a **darabszámot**
  (számjeggyel és kiírva – az összetett számnevek is: „két tojás",
  „negyvenöt gramm", „százhuszonöt gramm", „nyolcvanöt kiló",
  „két és fél deci"),
  a poharas/korsós italokat, a mérőszavakat (tányér, bögre, kanál,
  marék, tábla, szelet, szem, karéj, szál, kupica, üveg, kancsó, korty) és az **adagot** („fél adag gyros", „grillcsirke fél adag").
  A **negyed** és a **dupla adag** is számít, a mérőszó szám nélkül egy
  darabot jelent („tábla csoki" = egy tábla), a **tartomány** közepe megy be
  („2-3 szelet kenyér" = két és fél), és a birtokos tört is tört
  („az alma fele", „a fele adag rizs", „a pizza negyede", „a szendvics
  harmada", „a pizza háromnegyede")
- **A mennyiség a név után is állhat**: „banán 2 db", „tojás (3 db)",
  „kenyér (2 szelet)" – a bevásárlólista-szórend legalább olyan gyakori,
  mint a fordítottja
- **A kötőjel is számít**: a „sonkás-sajtos szendvics" három tétel (a magyar
  melléknévi kötőjel két hozzávalót köt össze), a „túró-rudi" viszont egy
- **352 étel** kcal- és fehérje-értékkel (a magyar konyha klasszikusaitól
  az italokig), kereshető és lapozható **kalóriatáblázattal**,
  amelyből egy koppintással naplózhatsz
- **Saját ételek** felvétele – a felismerés is megtalálja őket
- **Napi kcal- és fehérje-cél** haladássávval; javaslat a BMR-ből vagy a
  Profilban beállított fogyási célból
- **Napi mérleg**: a mai kártyán az edzéssel elégetett kalória és a
  nettó bevitel is látszik
- **Edzés-kalória beszámítása** (kapcsolható): aki sokat edz, annál a fix cél
  napokon át nagy mínuszt jelentene – bekapcsolva a napi cél az elégetett
  kalóriával nő, legfeljebb napi 800-zal
- **Étkezési ablak**: az első és az utolsó mai étkezés között eltelt idő
  (időszakos böjthöz)
- **„Mit egyek még?"**: a maradék kalóriára és a hiányzó fehérjére három
  konkrét ötlet adaggal együtt („Görög joghurt 150 g · 180 kcal · 14 g
  fehérje"), egy koppintással naplózható – naponta más választék; a napi
  emlékeztető is konkrét ötletet ad a maradékra
- **Fotó** az étkezéshez (kamera vagy galéria), és **arány-csúszkák**: a kép
  alapján utólag pontosítható, miből mennyi volt
- **Utólagos pótlás mondatból**: a „tegnap este pizzát ettem" a tegnapi
  napra kerül, a napszaknak megfelelő órával – a „3 napja", a „két napja",
  az „egy hete", a „hétfőn", a „múlt kedden", a konkrét dátum („július
  30-án", „aug 1-jén") és a kimondott óra („19 órakor", „délután 4-kor")
  is megy
- **A szokásos étkezésed**: ha egy napszakban háromszor ugyanazt naplóztad,
  az app magától felajánlja egy koppintásra (a sorrend és a mennyiség nem
  számít, csak az, hogy ugyanazok az ételek); az esti emlékeztető is
  megnevezi, ha aznap még nem naplóztál
- **Kedvencek** és gyakori étkezések gyors csipjei, keresés a naplóban,
  napi bontás, napi részletek és megosztás

### 💧 Víz
- Pohár-alapú (2,5 dl) számláló napi céllal, haladássávval
- **Gyorsgomb a widgeten** – app megnyitása nélkül
- **A mondatból is**: ha az étrendbe azt írod, „ittam fél liter vizet",
  az a vízcélba is beszámít
- Heti átlag a statisztikákban és a heti összefoglalóban

### 🩹 Megelőzés & rehab
- **15 testtáj, 81 gyógytornász-ihletésű gyakorlat** adagolással, technikai
  tippel és videóval: boka-stabilitás, térd, IT-szalag (a térd külső oldala),
  derék (McGill-hármas), váll, golf- és teniszkönyök, csukló és kéz (egérkéz),
  nyak, csípő, combhajlító, talp (plantar fasciitis), sípcsont, Achilles,
  **háti gerinc (ülőmunka ellen)** – kész, 10–15 perces megelőző sorok
- **A háti gerinc saját sort kapott**: a napi nyolc óra ülés a mellkasi
  szakaszt merevíti be, és ennek árát a nyak és a váll fizeti meg – oda
  vándorol a mozgás, ami innen hiányzik. A „merev a felső hátam", a „fáj a
  lapockám között" és a „görbe a hátam az ülőmunkától" ide fut
- **A panasz-mondat is ajtó**: a „fáj a vállam", a „beállt a derekam" vagy a
  „kificamodott a bokám" bármelyik beviteli mezőből a megfelelő sorhoz visz.
  Egy szóban is elég, ahogy a magyar mondja: „derékfájás", „csípőfájdalom" –
  és a diagnózis neve is („golfkönyök", „teniszkönyök", „futótérd",
  „sarkantyú", „gerincferdülés"). A HANG is panasz: „ropog a térdem",
  „recseg a vállam", „kattog a csípőm". És nem csak a „fáj": „húz a vádlim",
  „görcsöl a lábam", „szúr a derekam", „feszül a nyakam", „reggelre
  elgémberedik a kezem" – ahogy a magyar tényleg mondja. A tagadott panasz
  („már nem fáj", „fájdalommentes") nem nyit sort
- **Piros zászló**: zsibbadásra, duzzanatra, éjszakai vagy sugárzó
  fájdalomra az app nem gyakorlatsort ad, hanem kimondja, hogy ezt meg kell
  nézetni – a hallgatás rosszabb válasz lenne, mint a nemleges
- **Mellkasi panasz: külön ág.** A „fáj a mellkasom futás közben" és a
  „szorít a mellkasom edzés közben" nem gyógytorna-ügy: az app abbahagyást,
  sürgős orvosi segítséget és 112-t mond. Itt a tévedés ára aszimmetrikus –
  egy felesleges figyelmeztetés kellemetlen, egy elmaradó nem javítható
- **A cél-mondat is**: „boka stabilitás", „váll mobilizálás", „derék rehab" –
  nem kell megvárni, hogy fájjon. És ahogy az ember tényleg kéri: „váll
  gyakorlatok", „mit csináljak a vállamra", „nyak lazítás", „boka erősítés",
  „erősebb bokát szeretnék", „stabilabb térdet akarok", „jobb tartás"
- **Vezetett mód**: a telefon időzíti és bemondja a gyakorlatokat, a
  kétoldalasokat bal/jobb bontásban, a kör-szám a 10–20 perces kerethez áll be
- **Heti fókusz**: kitűzhetsz egy testtájat, az app heti 3 alkalmat számol
  (hétfőnként nulláz), sorozat-számlálóval („🔥 3 hete sorban") – az állás a
  kezdőlap csempéjén, a Statisztikában és a heti összegzésben is látszik
- **Mikorra várható javulás**: minden testtáj lapján ott a reális időtáv –
  az egyensúly-munka 2–3 hét, az ín-panaszok (golfkönyök, Achilles, talp)
  6–12 hét. Aki két nap után nem érez semmit, különben abbahagyja
- **Fokozatosság testtájra szabva**: hat elvégzett sor után a lap kimondja a
  KÖVETKEZŐ SZINTET – és az területenként más: a bokánál instabil felület és
  szökdelés, a golfkönyöknél nehezebb súly lassabb leengedéssel, a deréknál
  tartás helyett terhelés. A sor szövegként **megosztható**
- **Jelentés a gyógytornásznak**: négy hét története egy üzenetben –
  testtájanként az elvégzett sorok, az utolsó tíz fájdalom-érték és az irány
- **Fájdalom-napló**: napi 0–10-es érték testtájanként, egy koppintással (a
  sor elvégzése után magától kérdez). A lap kimondja az irányt – „javul",
  „rosszabbodik", „nem sokat mozdult" –, és görbét is rajzol; a mondat maga
  is hozhatja az értéket („fáj a vállam 6/10"). Négy bejegyzés alatt nem
  mond irányt, a 8 fölötti friss érték pedig szakemberhez küld. CSV-be
  exportálható, a heti összegzés és a Statisztika is mutatja
- Minden lapon ott a figyelmeztetés: ez megelőzés, nem orvoslás

### 😴 Alvás & nyugalmi pulzus
- **„Aludtam 8 órát"** – egy mondat vagy egy koppintás a Profilban: napi egy
  érték, heti átlag, két hét görbe, minősítés; a vasárnapi összegzésben is
- **Két időpont is elég**: „este 11-kor feküdtem, reggel 7-kor keltem",
  „22:30-tól 6:15-ig aludtam", „lefeküdtem 23 órakor, felkeltem 7 órakor",
  „este 10-re ágyban voltam, reggel 6-kor keltem" (a lefekvést nem csak
  „feküdtem"-mel mondjuk) –
  a kivonást ne a felhasználó végezze el. A magyar „fél tizenegy" tíz harminc,
  és ha a délelőtti óraszámból képtelen hossz jönne ki, este értendő; az
  „alvás 6:30" pedig hossz, nem időpont. Az ébredés állhat elöl is („ma
  reggel 6:30-kor keltem, 22:45-kor feküdtem le") – a lefekvés- és
  ébredés-szó helye dönti el a sorrendet
- **A -ról/-re pár második száma a mai érték**: a „derékbőségem 90-ről 86-ra
  csökkent" és a „testzsír 22-ről 18 százalékra" mondatból eddig a RÉGI szám
  került be – pont az, ami már nem igaz
- **„Nyugalmi pulzus 52"** – ugyanígy: napi egy érték, heti átlag, görbe és
  léptetős gyorsbevitel; az edzés-adat („átlagpulzus 165") nem téveszti meg
- **A szokásosnál jóval magasabb reggeli pulzusnál** a kezdőlap szól:
  kímélő nap, sok víz, korai lefekvés – a pihenőpulzus a túlterhelés és a
  kezdődő betegség legkorábbi jele
- **Alvás ↔ edzés és alvás ↔ pulzus a saját adatból**: a Statisztika kimondja,
  ha edzésnapokon kevesebbet alszol, és hogy a rövid éjszakák utáni reggeleken
  mennyivel magasabb a pulzusod
- **Három adat egy mondatból**: a „78,4 kg, aludtam 7 órát, nyugalmi pulzus 52"
  mindhármat elmenti – eddig csak az elsőt, a többit az app szó nélkül eldobta
- Mindkét napló **CSV-be exportálható**, és jelvény is jár értük

### 🏋️ Erősítő edzésnapló
- **Sorozatok rögzítése** gyakorlatonként (ismétlés × súly), a legutóbbi alkalom
  automatikus előtöltésével, kereséssel a naplóban
- **Érzett terhelés (RPE 6–10)**, elhagyható: a súly és az ismétlés nem mondja
  meg, mennyi maradt a tankban. Könnyű napnál (≤7) a javaslat rögtön súlyt
  emel, a határon (10) pedig megismételteti ugyanazt. Mondatból is megy
  („guggolás 3x10 100 kg rpe 8", „rir 2" – RIR 2 = RPE 8 –, „90 kg @8"), és a CSV exportban is szerepel
- **Sorozatok mondatból**: „3x10 fekvenyomás 60 kg", „guggolás 5x5 80 kg",
  „húzódzkodás 3x8" (saját testsúly), „bicepsz 12-10-8 15 kg"
  (sorozatonként más ismétlés, per-jellel is: „5/5/5"), „guggolás 3x10x60", „vállból nyomás
  3 sorozat 12 ismétlés 20 kg", „4 sorozat 8 fekvenyomás" (az „ismétlés"
  szót el lehet hagyni), „guggolás ötször ötöt 100 kg",
  „fekvenyomás 60x10, 70x8, 80x6", „fekvenyomás: 60/10, 70/8, 80/6" és
  „húzódzkodás 12, 10, 8" (szóközzel is)
  (súly × ismétlés, sorozatonként – szorzójellel és perjellel is),
  „3 kör 10 fekvőtámasz", „5 kör 10 fekvőtámasz 15 guggolás 20 hasizom"
  (vessző nélkül is: a magyar felsorolásban a szám maga tagol),
  „leguggoltam 140-et" (rúddal terhelt
  gyakorlatnál a magában álló háromjegyű szám kiló, nem ismétlés – de a
  „csináltam 100 guggolást" darabszám marad) – akár több
  gyakorlat egy mondatban,
  kötőszó nélkül is; a mondat időpontot is mondhat („tegnap húzódzkodás 4x8").
  51 gyakorlat és gép, a jelzős változatokkal
  („kábeles tricepsz", „elöl guggolás") és a termi anglicizmusokkal
  („leg curl", „chest press", „skull crusher", „hammer curl"). Ahol a súly nagyságrendben más, ott
  külön gyakorlat: a „román felhúzás", a „bolgár kitörés" és a „ferde
  fekvenyomás" saját néven él, saját rekorddal és progresszióval – egy
  vödörben a nehezebbik súlya kerülne a könnyebbik javaslatába. Mentés előtt
  megmutatja, mit értett, és **odaírja a legutóbbi alkalmat is**
  („↳ múltkor 57,5 kg · ▲ +2,5 kg · 3 napja")
- 🔥 **Bemelegítő rámpa**: ott van a mai javaslat kártyáján is („🔥 Bemelegítés:
  20×10 · 50×5 · 70×3 · 85×2"), és külön kalkulátorként a tárcsabontással
  együtt. A mai munkasúlyhoz üres rúd → 50% → 70% → 85%,
  mindegyik lépcső **felrakható** súlyon (2,5 kg-os osztás), a tárcsabontással
  együtt – a rámpát fejben is ki lehet számolni, csak a végén nem kerek szám
  jön ki, amit rá lehetne rakni a rúdra
- ⏱ **Tartások másodpercben**: a plank, a fal-ülés és a holt függés nem
  ismétlés, hanem idő. A napló „1:00"-t ír „0 kg × 60" helyett, a mondat is
  érti („plank 3x1 perc", „alkartámasz 3x60", „fal ülés 3x40 mp"), a
  progresszió pedig tíz másodperccel lép, két percnél megáll, és onnan a
  sorozatszám, majd a nehezebb változat visz tovább. Testsúlyos sorozatnál
  nem kerül nulla kiló a súlymezőbe
- **Rekordok**: max súly és becsült **1RM** (Epley), **súly-fejlődési grafikon**,
  heti és összesített volumen. Testsúlyra is: **legtöbb ismétlés** és
  **leghosszabb tartás** – aki csak fekvőtámaszozik és plankol, annak eddig
  egyetlen erő-rekordja sem volt
- **Progresszió-javaslat**: mit nyomj ma? Dupla progresszió szerint előbb az
  ismétlésszám kúszik fel a sáv tetejéig (8–12), utána lép a súly (20 kg alatt
  1,25 kg, felette 2,5 kg). A tempót a leggyengébb sorozat szabja meg, a
  bemelegítő sorozatok nem számítanak bele. Három egyforma alkalom után
  visszavételt javasol. Egy koppintás, és beírja a sorozatokat
- 📅 **Edzésnapok (sablonok)**: hat beépített nap (Tolónap, Húzónap, Lábnap,
  Felsőtest, Alsótest, Teljes test) és saját napok. A nap megnyitva
  gyakorlatonként ott a mai progresszió-javaslat és a bemelegítő rámpa,
  saját nap felvételénél pedig gyakorlat-csipekkel (gépelés helyett); a
  beépített nap egy koppintással lemásolható sajátnak és úgy szerkeszthető,
  egy koppintással beírható – eddig minden gyakorlatot külön kellett
  kikeresni, és a napot fejben tartani. Ami ma már megvan, az **ki van
  pipálva** („2 / 5 megvan ma", a végén „🏁 Kész az edzésnap"), és mentés
  után rögtön vissza is lép a listára a következő gyakorlathoz, és a **heti fókuszhoz illő nap** meg van
  jelölve („🎯 Lábnap · ma ez jön"), és minden nap mellett ott van, mikor
  csináltad utoljára („legutóbb 4 napja" – akkor számít megcsináltnak, ha
  a gyakorlatai legalább fele megvolt aznap). Ha van a mai fókuszhoz illő nap, az
  az erősítő képernyő tetején is ott van – egy kész nap teljes, sorrendbe
  rakott terv, nem három különálló gyakorlat. A kezdőlap csempéje is ezt
  írja ki („🏋️ Erősítő · ma Lábnap"), és az esti emlékeztető is megnevezi
  a mai nap első gyakorlatait
- **Mai ajánlat**: a héten kimaradt izomcsoportokból egy-egy gyakorlat a
  progresszió-javaslattal együtt – ha a heti fókuszban áll valami mára, az
  megy elöl („🎯 Evezés · 3 × 10 · 52,5 kg") – csak
  olyat ajánl, amit már csináltál, hogy legyen mihez mérni a súlyt
- **„Mikor csináltad utoljára"** minden gyakorlatnál, és figyelmeztetés arra,
  ami két hete kimaradt
- **Izomcsoport-egyensúly**: hány napon volt láb / hát / mell / váll / kar /
  törzs az elmúlt héten, és mi maradt ki
- ⏱️ **Pihenő-időzítő**: mentés után magától indul (ha már használtad), és
  javaslatot ad a legutóbbi sorozatod ismétlésszámából – nehéz sorozathoz
  hosszabb, tömegépítő sávhoz rövidebb pihenő
- 🧮 súlytárcsa-kalkulátor, 📈 1RM & százalék kalkulátor
- **Könyvtár**: egy helyen az összes mondat-forma, amit az app ért, és amit
  megosztani lehet (étrend,
  edzés-előzmény, erősítő sorozat, időzítő, mérés, edzésnap) friss példákkal, és technikai tipp
  minden beépített és felismert gyakorlathoz – a súlyzós alapoktól a gépekig –,
  valamint az összes felismert sportág a szokásos alkalom-hosszal
- 📨 **Megosztott szöveg**: bármelyik appból (jegyzetek, üzenet, böngésző)
  megoszthatsz egy szöveget a Grittel – az edzésterv, a recept vagy a
  baráttól kapott ötlet a megfelelő naplóba kerül, nem kell átgépelni
- 🧭 **A mondat megtalálja a helyét**: ha az étkezés-mezőbe írod, hogy „30 perc
  futás", vagy az edzés-mezőbe, hogy „ebédre rántott hús", az app nem azt
  mondja, hogy nem érti – megmondja, melyik napló érti, és **átviszi oda a
  mondatot** is, hogy ne kelljen újragépelni

### 🐺 Blaze, a kabalafigura
- **Heti szokás**: „kedd van – ilyenkor általában úszás szokott lenni" (csak
  ha a nap tényleg egyértelműen egy sportághoz kötődik)
- **Élő köszöntés**: belépéskor Blaze beugrik, integet, kacsint, majd halkan
  „lélegzik"; a kezdőlapi kártyáján koppintásra is integet egyet
  (a díszítő animációk kapcsolója ezt is némítja)
- **Helyzet-tudatos köszöntés** belépéskor (veszélyben lévő széria, félbehagyott
  kihívás, mai eredmény, hátralévő fehérje)
- Napi értesítés, ha még nem edzettél; **heti visszatekintő** vasárnap,
  **havi visszatekintő** minden hónap 1-jén – a súlyzós munka (sorozatok,
  volumen, csúcssúly), az étrend és a víz is benne. Ha a héten
  **mindenkori rekord** dőlt meg, azt külön kiírja („🏆 Új csúcs:
  Guggolás 120 kg") – a hét csúcssúlyából magától nem derülne ki,
  hogy az „ez volt a hét" vagy „ilyet még soha". A havi visszatekintő
  ugyanezt hozza, ott legfeljebb hármat felsorolva
- **Mozgó widget** a kezdőképernyőn: állapot, mai kcal és víz, gyorsgombok
  (edzés indítása, erősítő napló, +1 pohár víz)

### 🏅 Motiváció
- **Szintek + XP** (edzés-percek, táv, napi kihívás, étrend napi első bejegyzése)
- **Napi kihívás** 11 típusból, a szokásaidhoz igazodva (perc, kör, két edzés,
  km, ismétlés, étkezés, fehérje-cél, vízcél, lépésgyűjtés, kalóriaégetés, súlyzós volumen)
- **Gyűjthető jelvények** (edzésszám, táv, széria, kihívás, étrend, víz és
  súlyzós mérföldkövek: 10 gyakorlat, 10 tonna volumen, 4 izomcsoport egy
  héten; rehab-, alvás- és pulzus-mérföldkövek), napi és heti sorozat (óraátállás-biztos, terv-tudatos:
  a pihenőnap nem töri meg), konfetti, hangulat-napló

### 📊 Statisztika & előzmények
- Heti / havi / összes összesítők, 8-hetes diagram, havi naptár,
  **12 hetes aktivitás-hőtérkép**, terv-teljesítés
- **Melyik napokon edzel**: a hét napjainak bontása 12 hétből – melyik nap a
  tiéd, és melyik marad rendre ki
- **Ez a hónap**: edzések, idő, táv, kalória és volumen az előző hónap
  UGYANANNYI napjához mérve – a folyó hónap nem tűnik visszaesésnek attól,
  hogy még nem ért véget
- **Csúcsaid**: leghosszabb edzés, leghosszabb táv, leggyorsabb tempó,
  legtöbb lépés, legtöbb elégetett kalória, a legnagyobb napi volumen,
  valamint a **legnehezebb emelés** és a **legjobb becsült 1RM** a
  gyakorlat nevével („💪 Legnehezebb emelés · Guggolás — 120 kg × 1") –
  mindegyik dátummal
- **Az idei éved**: éves madártávlat – aktív napok, heti átlag, össz idő
  és táv, az év sportja, a leghosszabb edzés és a legaktívabb hónap
- **Heti mozgás-cél**: az egészségügyi ajánlás heti 150 perce haladássávval,
  és a hiányzó percek emberi nyelven („még 40 perc – ez két rövid edzés");
  a cél átállítható; a kezdőlap, a widget, a heti összegzés és a napi
  emlékeztető is mutatja a hét állását, és két jelvény jár érte (egy hét,
  majd négy hét sorban)
- **Terhelés-figyelés**: az elmúlt hét edzésperce a megelőző négy hét heti
  átlagához mérve („⚠️ 1,9× a szokásos") – a sérülések többsége nem a sok
  edzésből, hanem a hirtelen többől jön; nagy ugrásnál a heti összegzés is szól
- **Keresés az előzményekben**: sportág, program neve és jegyzet szerint –
  a szótövekkel együtt, tehát a „bringa" a kerékpáros edzéseket is megtalálja
- **Sportágankénti bontás** (elmúlt 30 nap): alkalmak és össz-idő
  sportáganként, arány-sávval – a mért és a kézzel felvett edzés egy sorban
- **Súlyzós szekció** (elmúlt 30 nap): edzésnapok, gyakorlatok, sorozatok,
  ismétlések, **volumen** és napi átlag, a legtöbb munkát kapott gyakorlattal
  és izomcsoporttal, valamint az **átlagos érzett terhelés** (RPE); és hogy
  **merre tartasz** a fő gyakorlatokban („📈 Fejlődés · 90 nap – Guggolás
  82,5 → 98,8 kg (+20%)"; a rekord a plafont mutatja, ez az irányt), és hogy
  **melyik edzésnapot hányszor** csináltad meg („📅 Lábnap 4× · Tolónap 3×")
  – egy sablon, amit sosem csinálsz meg, nem terv, hanem jókívánság
- **Étrend-szekció**: 7 napos átlagok, cél-tartás, 30 napos csík, átlagos
  **étkezési ablak** és napszak-jellemző („🌙 Este eszed a kalóriáid 52%-át"),
  valamint a **legnehezebb hétköznap** („📈 Szombat a legnehezebb nap: átlag
  2600 kcal, a többi napon 2050") – a havi átlag ezt elrejtené
- **Profil / BMI / BMR**, testadatok és változás-diagram **testsúly-tendenciával**
  (kg/hét, lineáris illesztéssel az összes mérésre) és a fogyási célhoz mért
  becsléssel: „a célig még 3,2 kg (~7 hét ezzel az ütemmel)". Ha rég volt mérés,
  szól is érte („⚖️ 12 napja mérted magad utoljára") – a tendencia annyit ér,
  amennyi adat van mögötte
- **Mentett mérések listája**: az utolsó nyolc mérés dátummal, és bármelyik
  külön törölhető – egy elgépelt szám miatt eddig az egész görbét fel kellett
  áldozni
- ⚖️ **Mérés mondatból** (a Profil „✍️ Mérés mondatból" gombjáról, vagy bárhonnan,
  ahol mondatot írsz): „ma reggel 78,4 kg", „78 kiló vagyok", „mérleg: 81,2",
  „78,4 kg és 18% testzsír" – a számokat beírja a mezőkbe, a mentést te nyomod
  meg. A kiló a legterheltebb mértékegység az appban, ezért csak akkor mérés,
  ha a mondat kimondja („vagyok", „mérleg", „testsúly"), vagy ha a számon és
  egy napszakon kívül nincs is más benne: a „fekvenyomás 80 kg" és a „vettem
  2 kg almát" nem testsúly. A kiírt számnév is megy: „hetvennyolc kiló vagyok"

### 📋 Heti terv
- **Edzésnapok** kijelölése (Blaze csak ezeken emlékeztet)
- **Heti fókusz**: melyik napon mit edzel („H: Láb · Sze: Hát · P: Mell”) –
  a kezdőlap és a napi emlékeztető is mutatja, és ha ma nincs fókusz,
  a holnapit írja ki
- **Fókusz-teljesülés a heti összegzésben**: a hát-napon tényleg hát volt-e –
  elnézően, mert a terv nem tiltólista

### ⏰ Emlékeztetők
- Több emlékeztető tetszőleges időpontra, saját üzenettel
- **Napválasztás**: minden nap, csak hétköznap, csak hétvégén vagy pontosan
  a kiválasztott napokon – óraátállás-biztos ütemezéssel

### 📤 Megosztás & adatok
- Edzés / haladás / jelvények / statisztika / hőtérkép / napi étrend megosztása
- **Biztonsági mentés / visszaállítás** fájlba – a GPS-útvonalakkal együtt;
  az étkezés-fotók a telefonon maradnak –,
  **CSV export** (előzmények, erősítő napló RPE-vel, étrend a vízzel együtt,
  testsúly-mérések BMI-vel)

## Automatikus frissítés (ajánlott) — Obtainium

Az appot a GitHub Actions minden változtatásnál lebuildeli és egy új
**Release**-be tölti. Az [Obtainium](https://github.com/ImranR98/Obtainium)
ingyenes app ezt figyeli, és **magától frissít**:

1. Telepítsd az **Obtainium**-ot (Play Store / F-Droid / GitHub Release).
2. Add hozzá a repó URL-jét: `https://github.com/David-Getta/edzo_app`
3. Innentől az Obtainium jelzi és telepíti az új verziókat.

Minden build **egyedi taggel** (`build-N`) és **növekvő `versionCode`-dal**
készül, így az Obtainium és az Android is frissítésként ismeri fel.

## Kézi letöltés (telefonra)

1. Nyisd meg a **[legfrissebb Release](../../releases/latest)** oldalt.
2. Töltsd le a **`My-Trainer.apk`** fájlt (a fájlnév a korábbi névből maradt).
3. Nyisd meg — ha rákérdez, engedélyezd az „ismeretlen forrásból" való
   telepítést, majd telepítsd.

> Az APK fix, saját aláírással készül, hogy a frissítés ütközés nélkül menjen.

## Fejlesztői build

```bash
./gradlew testDebugUnitTest   # egységtesztek (ételfelismerés, szintek, víz,
                              # szériák, progresszió, izomcsoportok)
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
```

A CI a build előtt lefuttatja a teszteket: ha elhasalnak, nem készül APK.

Android SDK nélkül (bármilyen JDK-val) a tiszta Java logika tesztjei helyben
is futtathatók, másodpercek alatt:

```bash
bash tools/gyorsteszt.sh      # ~878 teszt: ételfelismerés, időzítő-számítások,
                              # mondat-alapú edzésfelvétel, progresszió…
bash tools/szosopres.sh       # hétköznapi magyar szavak az összes felismerő
                              # ellen: melyikben lakik egy rövid szótő.
                              # A saját kommentjeink mellé egy ötvenezer
                              # szavas gyakorisági listát is letölt
```

A **szósöprés** a leghalkabb hibafajtát keresi: amikor egy rövid szótő
beleakad egy hétköznapi szóba, és a bejegyzés létrejön – csak épp nem arról,
amit az ember írt (a KÉPERNYŐben az eper, a TARTALMAzban az alma, a „150
graMMAl"-ban a harcművészet). Korpuszt csinál a forrás magyar kommentjeiből,
és az összes felismerőn átfuttatja; a találatokat végig kell nézni, mert a
java részük jogos.
A söprés mind a nyolc felismerőt nézi: étel, mozgás, sorozat, időzítő,
mérés, alvás, pulzus és a rehab panasz/cél-mondatai.

**Véletlen mondatok.** A tesztek azt őrzik, amire gondoltunk; a maradékra a
fuzz való. Egymillió véletlenül összerakott mondat fut végig az összes
felismerőn, és minden eredményre ugyanaz a kérdés: életszerű-e? (Legfeljebb
ötven kör, négy óránál rövidebb edzés, húsz kiló alatti adag, harminc és
kétszázötven kiló közti testsúly, két és tizenhat óra közti alvás.) Ami
ezekből kilóg, az mind valódi hiba volt – így került elő a „8x 60 km"
négyszáznyolcvan kilométeres futása és a „8x 60 perc" nyolcórás időzítője.

Tiszta natív Android app (Java, `Activity` + programozott felület), külső
függőségek nélkül (a JUnit csak teszthez). `minSdk 24`, `targetSdk 33`.
