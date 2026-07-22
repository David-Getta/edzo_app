# My trainer 🏃⏱️

Teljes értékű **edzőtárs Androidra** – intervallum (HIIT) időzítő, futáskövetés,
nyújtás & mobilitás, edzésnapló, játékos motiváció és részletes statisztikák.
Sötét, „cyber” cián-magenta megjelenés, sok látvánnyal és finom animációkkal.
Minden képernyőnek saját, generált látványos háttere van, a főképernyő háttere
pedig **naponta váltakozik**. Tiszta natív Android (Java, programozott felület,
külső függőségek nélkül).

## Funkciók

### ⏱️ Edzés & időzítő
- Testre szabható **Bemelegítés / Előkészület / Futás / Pihenő / Körök /
  Levezetés** – másodpercre pontosan (léptető gyorsulással vagy beírással)
- **Élő folyamatsáv** (hol tartasz az egész edzésben) + **teljes hátralévő idő**
- Látványos **világító körgyűrű** vezető ponttal, az utolsó 3 mp-ben lüktető
  számláló, a végén ünneplő pipa – és **egygombos újraindítás** ugyanarra az edzésre
- Gyors sablonok (**HIIT / Tempó / Tabata**) az aktív sablon kiemelésével
- **10 beépített program** (törzs, teljes test, láb, kar, zsírégető HIIT,
  mag & egyensúly, reggeli mobilitás, **klasszikus 7 perces edzés**,
  **kezdő teljes test**…) + saját programok, gyakorlat-leírásokkal
- 🔊 Hangválasztás (külön futás/pihenő), előhallgatás, 3-2-1 csipogás, rezgés
- 🗣️ Hangos bemondás (TTS), 🎛️ vezérlés az értesítésből, 🌙 fut kikapcsolt
  képernyővel is (foreground service + wake lock)

### 🏃 Futás & mérés
- **GPS táv, tempó (perc/km vagy km/h), lépések, kalória**, átlag/max sebesség
- Útvonal, kör-splitek és sebesség-diagram az edzés részleteinél

### 🧘 Nyújtás & mobilitás
- Külön képernyő **Bemelegítés / Nyújtás / Hengerezés** szekciókkal (színkódolt)
- Izmonként ≥2 nyújtás, deréktáj/gerinc, foam rolling – mind **videós útmutatóval**
- **Vezetett rutinok** (a telefon időzíti és bemondja a gyakorlatokat)

### 🏋️ Erősítő edzésnapló
- **Sorozatok rögzítése** gyakorlatonként (ismétlés × súly), gyors-választó
  gyakorlatnevekkel
- **Rekordok**: max súly és **becsült 1RM** (Epley) gyakorlatonként
- **Súly-fejlődési grafikon** minden gyakorlathoz, teljes volumen kijelzés

### 🏅 Motiváció & napló
- **Szintek + XP-sáv**, 20 gyűjthető **kitüntetés** (haladás-jelzéssel),
  személyes **rekordok**, **napi és heti sorozat** (óraátállás-biztos),
  sorozat-veszély figyelmeztetés, gyengéd pihenő-emlékeztető, **konfetti**
- **Edzésnapló**: edzés utáni **hangulat** (😣😐🙂💪) és **szöveges jegyzet**,
  utólag szerkeszthető
- **Heti cél** (edzésszám / perc / km) folyamatjelzővel

### 📊 Statisztika & előzmények
- Heti / havi / összes összesítők, **heti trend**, 8-hetes diagram,
  havi naptár, **12 hetes aktivitás-hőtérkép**, hangulat-eloszlás
- **Előzmények** típusszűrővel (futás / erő), egyedi edzés törlése
- **Profil / BMI** – testadatok, élő BMI + kategória, változás-diagram

### 📤 Megosztás & adatok
- Edzés / haladás / kitüntetések / statisztika / hőtérkép **megosztása képként**
- **Biztonsági mentés / visszaállítás** fájlba, **CSV export**
- Testre szabható színek, animáció-kapcsoló, emlékeztetők, heti visszatekintő

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
2. Töltsd le a **`My-Trainer.apk`** fájlt.
3. Nyisd meg — ha rákérdez, engedélyezd az „ismeretlen forrásból" való
   telepítést, majd telepítsd.

> Az APK egy *debug* aláírással készül, ami saját használatra tökéletes.

## Fejlesztői build (opcionális)

```bash
./gradlew assembleDebug
# eredmény: app/build/outputs/apk/debug/app-debug.apk
```

Tiszta natív Android app (Java, `Activity` + programozott felület), külső
függőségek nélkül. `minSdk 24`, `targetSdk 33`.
