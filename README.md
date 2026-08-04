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
- Gyors sablonok (**HIIT / Tempó / Tabata**) és **10 beépített program**
  (törzs, teljes test, láb, kar, zsírégető HIIT, 7 perces edzés…), saját
  programokkal és gyakorlat-leírásokkal
- 🔊 Hangválasztás, 🗣️ hangos bemondás (TTS), 🎛️ vezérlés az értesítésből,
  🌙 fut kikapcsolt képernyővel is (foreground service + wake lock)

### 🏃 Futás & mérés
- **GPS táv, tempó, lépések, kalória**, átlag/max sebesség
- Útvonal, kör-splitek és sebesség-diagram az edzés részleteinél

### 📝 Kézi edzés-felvétel
- **Olyan edzés is naplózható, amit nem a telefon mért**: kézilabda, úszás,
  kondi, foci, tenisz, jóga… – 19 mozgásforma, sportág szerinti
  kalóriabecsléssel (MET × testsúly × idő)
- **Egy mondatból akár többet is**: „az elmúlt 3 nap alatt 3 futó edzés és
  6 kézi edzés", „10 km futás", „tegnap 1,5 óra bringa", „a héten minden
  nap futottam", „hétvégén 1-1 túra", „hétfőn és szerdán kondi", „kétszer úsztam", „leúsztam 1500
  métert", „tegnap este kondi", „ma 10000 lépés", „július 28-án
  futottam", „100 fekvőtámasz", „júl. 28-án 6x1 km", „napi 20 perc jóga
  egész héten" – mentés előtt megmutatja, mit értett; a tervekre („jövő
  héten…") őszintén szól, hogy a napló a megtörtént edzéseké, a tagadást
  is érti („ma nem futottam", „kondi helyett futás"), és a pihenőnapot
  is elfogadja
- **Sorozatos mondat esetén** („3x10 fekvenyomás 60 kg") felajánlja az
  Erősítő naplót, ahol a súly és az ismétlés is megmarad
- A kézi bejegyzés **mindenben egyenrangú a mérttel**: számít a szériába, az
  XP-be, a jelvényekbe, a heti visszatekintőbe és a statisztikába
- Elérhető a kezdőlap **„Edzés pótlása"** csempéjéről és az Előzmények
  „+" gombjáról; utólagos (múltbeli) felvételnél is a helyére kerül

### 🍽️ Étrend
- **Írd le, mit ettél** – az app felismeri az ételeket a mondatból:
  „rántott hús rizzsel", „150 g csirkemell 200 g rizs", „2 tojás", „fél alma"
- **Érti a tagadást is**: a „chips helyett almát ettem" csak almát naplóz,
  a „csoki nélkül kértem a kávét" csak kávét, a „ma nem ettem csokit" semmit
- Felismeri a **grammot** (g / gr / gramm / dkg / deka), a **darabszámot**
  (számjeggyel és kiírva: „két tojás", „negyvenöt gramm", „két és fél deci"),
  a poharas/korsós italokat, a mérőszavakat (tányér, bögre, kanál,
  marék, tábla, kupica) és az **adagot** („fél adag gyros", „grillcsirke fél adag")
- **330 étel** kcal- és fehérje-értékkel (a magyar konyha klasszikusaitól
  az italokig), kereshető és lapozható **kalóriatáblázattal**,
  amelyből egy koppintással naplózhatsz
- **Saját ételek** felvétele – a felismerés is megtalálja őket
- **Napi kcal- és fehérje-cél** haladássávval; javaslat a BMR-ből vagy a
  Profilban beállított fogyási célból
- **Napi mérleg**: a mai kártyán az edzéssel elégetett kalória és a
  nettó bevitel is látszik
- **Étkezési ablak**: az első és az utolsó mai étkezés között eltelt idő
  (időszakos böjthöz)
- **„Mit egyek még?"**: a maradék kalóriára és a hiányzó fehérjére három
  konkrét ötlet adaggal együtt („Görög joghurt 150 g · 180 kcal · 14 g
  fehérje"), egy koppintással naplózható – naponta más választék; a napi
  emlékeztető is konkrét ötletet ad a maradékra
- **Fotó** az étkezéshez (kamera vagy galéria), és **arány-csúszkák**: a kép
  alapján utólag pontosítható, miből mennyi volt
- **Utólagos pótlás mondatból**: a „tegnap este pizzát ettem" a tegnapi
  napra kerül, a napszaknak megfelelő órával
- **Kedvencek** és gyakori étkezések gyors csipjei, keresés a naplóban,
  napi bontás, napi részletek és megosztás

### 💧 Víz
- Pohár-alapú (2,5 dl) számláló napi céllal, haladássávval
- **Gyorsgomb a widgeten** – app megnyitása nélkül
- **A mondatból is**: ha az étrendbe azt írod, „ittam fél liter vizet",
  az a vízcélba is beszámít
- Heti átlag a statisztikákban és a heti összefoglalóban

### 🏋️ Erősítő edzésnapló
- **Sorozatok rögzítése** gyakorlatonként (ismétlés × súly), a legutóbbi alkalom
  automatikus előtöltésével, kereséssel a naplóban
- **Sorozatok mondatból**: „3x10 fekvenyomás 60 kg", „guggolás 5x5 80 kg",
  „húzódzkodás 3x8" (saját testsúly), „bicepsz 12-10-8 15 kg"
  (sorozatonként más ismétlés), „guggolás 3x10x60", „vállból nyomás
  3 sorozat 12 ismétlés 20 kg" – akár több gyakorlat egy mondatban,
  kötőszó nélkül is. 29 gyakorlat és gép, a jelzős változatokkal
  („román felhúzás", „bolgár kitörés", „kábeles tricepsz"). Mentés előtt
  megmutatja, mit értett, és **odaírja a legutóbbi alkalmat is**
  („↳ múltkor 57,5 kg · ▲ +2,5 kg · 3 napja")
- **Rekordok**: max súly és becsült **1RM** (Epley), **súly-fejlődési grafikon**,
  heti és összesített volumen
- **Progresszió-javaslat**: mit nyomj ma? Dupla progresszió szerint előbb az
  ismétlésszám kúszik fel a sáv tetejéig (8–12), utána lép a súly (20 kg alatt
  1,25 kg, felette 2,5 kg). A tempót a leggyengébb sorozat szabja meg, a
  bemelegítő sorozatok nem számítanak bele. Három egyforma alkalom után
  visszavételt javasol. Egy koppintás, és beírja a sorozatokat
- **Mai ajánlat**: a héten kimaradt izomcsoportokból egy-egy gyakorlat a
  progresszió-javaslattal együtt („🎯 Evezés · 3 × 10 · 52,5 kg") – csak
  olyat ajánl, amit már csináltál, hogy legyen mihez mérni a súlyt
- **„Mikor csináltad utoljára"** minden gyakorlatnál, és figyelmeztetés arra,
  ami két hete kimaradt
- **Izomcsoport-egyensúly**: hány napon volt láb / hát / mell / váll / kar /
  törzs az elmúlt héten, és mi maradt ki
- ⏱️ Pihenő-időzítő, 🧮 súlytárcsa-kalkulátor, 📈 1RM & százalék kalkulátor
- **Gyakorlat-könyvtár**: technikai tipp minden beépített és felismert
  gyakorlathoz – a súlyzós alapoktól a gépekig és variációkig

### 🐺 Blaze, a kabalafigura
- **Helyzet-tudatos köszöntés** belépéskor (veszélyben lévő széria, félbehagyott
  kihívás, mai eredmény, hátralévő fehérje)
- Napi értesítés, ha még nem edzettél; **heti visszatekintő** vasárnap,
  **havi visszatekintő** minden hónap 1-jén – a súlyzós munka (sorozatok,
  volumen, csúcssúly), az étrend és a víz is benne
- **Mozgó widget** a kezdőképernyőn: állapot, mai kcal és víz, gyorsgombok
  (edzés indítása, erősítő napló, +1 pohár víz)

### 🏅 Motiváció
- **Szintek + XP** (edzés-percek, táv, napi kihívás, étrend napi első bejegyzése)
- **Napi kihívás** 11 típusból, a szokásaidhoz igazodva (perc, kör, két edzés,
  km, ismétlés, étkezés, fehérje-cél, vízcél, lépésgyűjtés, kalóriaégetés, súlyzós volumen)
- **Gyűjthető jelvények** (edzésszám, táv, széria, kihívás, étrend, víz és
  súlyzós mérföldkövek: 10 gyakorlat, 10 tonna volumen, 4 izomcsoport egy
  héten), napi és heti sorozat (óraátállás-biztos, terv-tudatos:
  a pihenőnap nem töri meg), konfetti, hangulat-napló

### 📊 Statisztika & előzmények
- Heti / havi / összes összesítők, 8-hetes diagram, havi naptár,
  **12 hetes aktivitás-hőtérkép**, terv-teljesítés
- **Az idei éved**: éves madártávlat – aktív napok, heti átlag, össz idő
  és táv, az év sportja, a leghosszabb edzés és a legaktívabb hónap
- **Terhelés-figyelés**: az elmúlt hét edzésperce a megelőző négy hét heti
  átlagához mérve („⚠️ 1,9× a szokásos") – a sérülések többsége nem a sok
  edzésből, hanem a hirtelen többől jön; nagy ugrásnál a heti összegzés is szól
- **Sportágankénti bontás** (elmúlt 30 nap): alkalmak és össz-idő
  sportáganként, arány-sávval – a mért és a kézzel felvett edzés egy sorban
- **Súlyzós szekció** (elmúlt 30 nap): edzésnapok, gyakorlatok, sorozatok,
  ismétlések, **volumen** és napi átlag, a legtöbb munkát kapott gyakorlattal
  és izomcsoporttal
- **Étrend-szekció**: 7 napos átlagok, cél-tartás, 30 napos csík, átlagos
  **étkezési ablak** és napszak-jellemző („🌙 Este eszed a kalóriáid 52%-át")
- **Profil / BMI / BMR**, testadatok és változás-diagram **testsúly-tendenciával**
  (kg/hét, lineáris illesztéssel az összes mérésre) és a fogyási célhoz mért
  becsléssel: „a célig még 3,2 kg (~7 hét ezzel az ütemmel)"

### ⏰ Emlékeztetők
- Több emlékeztető tetszőleges időpontra, saját üzenettel
- **Napválasztás**: minden nap, csak hétköznap, csak hétvégén vagy pontosan
  a kiválasztott napokon – óraátállás-biztos ütemezéssel

### 📤 Megosztás & adatok
- Edzés / haladás / jelvények / statisztika / hőtérkép / napi étrend megosztása
- **Biztonsági mentés / visszaállítás** fájlba – a GPS-útvonalakkal együtt;
  az étkezés-fotók a telefonon maradnak –,
  **CSV export** (előzmények, erősítő napló, étrend a vízzel együtt)

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
bash tools/gyorsteszt.sh      # ~347 teszt: ételfelismerés, időzítő-számítások,
                              # mondat-alapú edzésfelvétel, progresszió…
```

Tiszta natív Android app (Java, `Activity` + programozott felület), külső
függőségek nélkül (a JUnit csak teszthez). `minSdk 24`, `targetSdk 33`.
