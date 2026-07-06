# My trainer 🏃⏱️

Egyszerű **intervallum (HIIT) edző időzítő** Androidra. Beállíthatsz
előkészületet, futás- és pihenőidőt, valamint a körök számát. Minden
szakaszváltásnál **sípol és rezeg** a telefon, az utolsó 3 másodpercben pedig
visszaszámol.

## Funkciók

- ⏱️ Beállítható **Előkészület / Futás / Pihenő** idő és **Körök** száma –
  **másodpercre pontosan** (léptető nyomva tartva gyorsul, vagy a számra
  koppintva beírható a pontos érték)
- 🔊 **Hangválasztás**: több síphang közül, **külön** a futás és **külön** a
  pihenő kezdetére, előhallgatással; a **3-2-1 visszaszámláló csipogás
  ki/be kapcsolható**; plusz **rezgés**
- 🌙 **Fut kikapcsolt képernyővel is** – háttérszolgáltatás + wake lock,
  így a sípszó és az időzítő sötét képernyőnél is megy
- 🗣️ **Hangos bemondás (beszéd)** – a telefon kimondja a szakaszokat
  („Futás", „Pihenő", „Utolsó kör", „Edzés kész"), bekapcsolható
- 🎛️ **Vezérlés az értesítésből** – Szünet / Folytatás / Leállítás gombok
  a lezárt képernyőről is, az app megnyitása nélkül
- 📍 **GPS táv- és sebességmérés** (opcionális) – lefutott táv és **km/h**
  menet közben, valamint **átlag- és max sebesség** a naplóban
- 📜 **Korábbi edzések** naplója (dátum, idő, táv, sebesség, körök)
- 📊 **Profil / BMI** – magasság, testsúly, kor, testzsír; élő **BMI** +
  kategória, és a testsúly/BMI/testzsír **változása diagramon**
- ⚡ Gyors sablonok (HIIT, Tempó, Tabata)
- 🟢 Nagy, színkódolt körkijelző és aktuális kör számláló
- 💾 A beállítások megjegyzésre kerülnek

## Automatikus frissítés (ajánlott) — Obtainium

Az appot a GitHub Actions minden változtatásnál lebuildeli és egy új
**Release**-be tölti. Az [Obtainium](https://github.com/ImranR98/Obtainium)
ingyenes app ezt figyeli, és **magától frissít**:

1. Telepítsd az **Obtainium**-ot (Play Store / F-Droid / GitHub Release).
2. Add hozzá a repó URL-jét: `https://github.com/David-Getta/edzo_app`
3. Mivel a repó privát, add meg az Obtainium beállításaiban a GitHub
   fiókodat / egy olvasó (repo-hozzáférésű) personal access tokent.
4. Innentől az Obtainium jelzi és telepíti az új verziókat.

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
