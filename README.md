# Edző Időzítő 🏃⏱️

Egyszerű **intervallum (HIIT) edző időzítő** Androidra. Beállíthatsz
előkészületet, futás- és pihenőidőt, valamint a körök számát. Minden
szakaszváltásnál **sípol és rezeg** a telefon, az utolsó 3 másodpercben pedig
visszaszámol.

## Funkciók

- ⏱️ Beállítható **Előkészület / Futás / Pihenő** idő és **Körök** száma
- 🔊 Külön **sípszó** a futás kezdetére (magas), a pihenőre (mély) és a
  3-2-1 visszaszámlálásra, plusz **rezgés**
- ⚡ Gyors sablonok (HIIT, Tempó, Tabata)
- 🟢 Nagy, színkódolt körkijelző és aktuális kör számláló
- 📱 A képernyő edzés közben **bekapcsolva marad**
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
