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
- 📍 **GPS táv-mérés** (opcionális) – a lefutott távot menet közben és a
  végén is mutatja
- 📜 **Korábbi edzések** naplója (dátum, idő, táv, körök)
- ⚡ Gyors sablonok (HIIT, Tempó, Tabata)
- 🟢 Nagy, színkódolt körkijelző és aktuális kör számláló
- 💾 A beállítások megjegyzésre kerülnek

## Az APK letöltése (telefonra)

Az appot a GitHub Actions automatikusan lebuildeli, és felteszi a
**[Releases](../../releases/tag/latest)** oldalra:

1. Nyisd meg a telefonod böngészőjében a `latest` release-t.
2. Töltsd le az **`Edzo-Idozito.apk`** fájlt.
3. Nyisd meg — ha rákérdez, engedélyezd az „ismeretlen forrásból" való
   telepítést, majd telepítsd.

> Az APK egy *debug* aláírással készül, ami saját használatra tökéletes.

## Fejlesztői build (opcionális)

```bash
./gradlew assembleDebug
# eredmény: app/build/outputs/apk/debug/app-debug.apk
```

Tiszta natív Android app (Java, `Activity` + programozott felület), külső
függőségek nélkül. `minSdk 24`, `targetSdk 34`.
