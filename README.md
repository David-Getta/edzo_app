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

### 🍽️ Étrend
- **Írd le, mit ettél** – az app felismeri az ételeket a mondatból:
  „rántott hús rizzsel", „150 g csirkemell 200 g rizs", „2 tojás", „fél alma"
- Felismeri a **grammot** (g / gr / gramm / dkg) és a **darabszámot** is
  (számjeggyel és kiírva: „két tojás")
- **154 magyar étel** kcal- és fehérje-értékkel, kereshető **kalóriatáblázattal**,
  amelyből egy koppintással naplózhatsz
- **Saját ételek** felvétele – a felismerés is megtalálja őket
- **Napi kcal- és fehérje-cél** haladássávval; javaslat a BMR-ből vagy a
  Profilban beállított fogyási célból
- **Fotó** az étkezéshez (kamera vagy galéria), és **arány-csúszkák**: a kép
  alapján utólag pontosítható, miből mennyi volt
- **Kedvencek** és gyakori étkezések gyors csipjei, keresés a naplóban,
  napi bontás, napi részletek és megosztás

### 💧 Víz
- Pohár-alapú (2,5 dl) számláló napi céllal, haladássávval
- **Gyorsgomb a widgeten** – app megnyitása nélkül
- Heti átlag a statisztikákban és a heti összefoglalóban

### 🏋️ Erősítő edzésnapló
- **Sorozatok rögzítése** gyakorlatonként (ismétlés × súly), a legutóbbi alkalom
  automatikus előtöltésével, kereséssel a naplóban
- **Rekordok**: max súly és becsült **1RM** (Epley), **súly-fejlődési grafikon**,
  heti és összesített volumen
- ⏱️ Pihenő-időzítő, 🧮 súlytárcsa-kalkulátor, 📈 1RM & százalék kalkulátor

### 🐺 Blaze, a kabalafigura
- **Helyzet-tudatos köszöntés** belépéskor (veszélyben lévő széria, félbehagyott
  kihívás, mai eredmény, hátralévő fehérje)
- Napi értesítés, ha még nem edzettél; **heti visszatekintő** vasárnap
- **Mozgó widget** a kezdőképernyőn: állapot, mai kcal és víz, gyorsgombok
  (edzés indítása, erősítő napló, +1 pohár víz)

### 🏅 Motiváció
- **Szintek + XP** (edzés-percek, táv, napi kihívás, étrend napi első bejegyzése)
- **Napi kihívás** 8 típusból, a szokásaidhoz igazodva (perc, kör, két edzés,
  km, ismétlés, étkezés, fehérje-cél, vízcél)
- **Gyűjthető jelvények**, napi és heti sorozat (óraátállás-biztos, terv-tudatos:
  a pihenőnap nem töri meg), konfetti, hangulat-napló

### 📊 Statisztika & előzmények
- Heti / havi / összes összesítők, 8-hetes diagram, havi naptár,
  **12 hetes aktivitás-hőtérkép**, terv-teljesítés
- **Étrend-szekció**: 7 napos átlagok, cél-tartás, 30 napos csík
- **Profil / BMI / BMR**, testadatok és változás-diagram

### 📤 Megosztás & adatok
- Edzés / haladás / jelvények / statisztika / hőtérkép / napi étrend megosztása
- **Biztonsági mentés / visszaállítás** fájlba, **CSV export**
  (előzmények, erősítő napló, étrend a vízzel együtt)

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
./gradlew testDebugUnitTest   # egységtesztek (ételfelismerés, szintek, víz)
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
```

A CI a build előtt lefuttatja a teszteket: ha elhasalnak, nem készül APK.

Tiszta natív Android app (Java, `Activity` + programozott felület), külső
függőségek nélkül (a JUnit csak teszthez). `minSdk 24`, `targetSdk 33`.
