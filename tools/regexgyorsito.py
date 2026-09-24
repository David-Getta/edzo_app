#!/usr/bin/env python3
"""A regex-minták fordítását egyszerivé teszi – csak a webes fordításban.

Miért kell ez? A felismerés mondatonként több mint ezer reguláris kifejezést
használ. A JVM ezeket ezredmásodperc alatt lefordítja, a böngészőben futó
(JavaScriptre fordított) változat viszont mintánként 0,23 ezredmásodpercet
tölt VELE – így egyetlen mondat felismerése 700 ezredmásodpercig tartott.
Gépelés közben ez használhatatlan.

A minták viszont mindig ugyanazok: elég egyszer lefordítani őket. Ez a
szkript a KIVONATOLT másolatokban átírja a hívásokat egy gyorsítótárazó
segédosztályra:

    s.replaceAll(A, B)   ->  Rx.ra(s, A, B)
    s.matches(A)         ->  Rx.m(s, A)
    Pattern.compile(A)   ->  Rx.c(A)

Az Androidos forrás nem változik: ott a JVM úgyis gyors, és a kódot
olvasható formában akarjuk tartani. Hogy az átírás ne csússzon el, az
egyezés-teszt az EREDETI forrást futtatja a JVM-en, és az átírt-lefordított
változatot a JavaScripten – a kettőnek karakterre egyeznie kell.
"""

import os
import sys

# metódus -> (új hívás, elfogadott argumentumszámok)
CEL = {
    "replaceAll": ("Rx.ra", (2,)),
    "replaceFirst": ("Rx.rf", (2,)),
    "matches": ("Rx.m", (1,)),
    "split": ("Rx.sp", (1, 2)),
}

# Az azonosítóban használható karakterek – a visszafelé olvasáshoz.
AZONOSITO = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_$.")


def kod_maszk(src):
    """Karakterenkénti típus: 'k' kód, 's' sztring, 'm' megjegyzés.

    A megjegyzést külön kell tudni a sztringtől: a láncolt hívások közé
    ebben a kódbázisban gyakran kerül magyarázat, és azt a visszafelé
    olvasásnak úgy kell átlépnie, mint a szóközt. Enélkül a lánc kettétörik,
    és félbevágott kifejezés keletkezik.
    """
    tipus = ["k"] * len(src)
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if c in '"\'':
            j = i + 1
            while j < n:
                if src[j] == "\\":
                    j += 2
                    continue
                if src[j] == c:
                    break
                j += 1
            for k in range(i, min(j + 1, n)):
                tipus[k] = "s"
            i = j + 1
        elif c == "/" and i + 1 < n and src[i + 1] == "/":
            j = src.find("\n", i)
            j = n if j < 0 else j
            for k in range(i, j):
                tipus[k] = "m"
            i = j
        elif c == "/" and i + 1 < n and src[i + 1] == "*":
            j = src.find("*/", i)
            j = n if j < 0 else j + 2
            for k in range(i, j):
                tipus[k] = "m"
            i = j
        else:
            i += 1
    return tipus


def kod(tipus):
    """A régi, logikai maszk: igaz ott, ahol kód van."""
    return [t == "k" for t in tipus]


def zaro(src, maszk, nyito):
    """A `nyito` helyen álló zárójel párja, vagy -1."""
    melyseg = 0
    for i in range(nyito, len(src)):
        if not maszk[i]:
            continue
        if src[i] == "(":
            melyseg += 1
        elif src[i] == ")":
            melyseg -= 1
            if melyseg == 0:
                return i
    return -1


def argumentumok(src, maszk, nyito, veg):
    """A hívás argumentumai a legfelső szintű vesszők mentén."""
    darabok, melyseg, kezdet = [], 0, nyito + 1
    for i in range(nyito + 1, veg):
        if not maszk[i]:
            continue
        c = src[i]
        if c in "([{":
            melyseg += 1
        elif c in ")]}":
            melyseg -= 1
        elif c == "," and melyseg == 0:
            darabok.append(src[kezdet:i])
            kezdet = i + 1
    darabok.append(src[kezdet:veg])
    return [d for d in darabok] if src[nyito + 1:veg].strip() else []


def ws_vissza(src, i, tipus=None):
    """Visszalépés szóközökön – és a megjegyzéseken – át."""
    while i >= 0 and (src[i] in " \t\n\r" or (tipus is not None and tipus[i] == "m")):
        i -= 1
    return i


def nyito_parja(src, maszk, zar):
    """A `zar` helyen álló csukó zárójel párjának helye, vagy -1."""
    nyit = {")": "(", "]": "["}[src[zar]]
    melyseg = 0
    i = zar
    while i >= 0:
        if maszk[i]:
            if src[i] == src[zar]:
                melyseg += 1
            elif src[i] == nyit:
                melyseg -= 1
                if melyseg == 0:
                    return i
        i -= 1
    return -1


def vevo_kezdete(src, maszk, pont, tipus):
    """A `pont` (a hívás pontja) előtti kifejezés kezdete, vagy -1.

    Visszafelé olvassuk a Java „elsődleges kifejezést”: zárójeles hívásokat,
    tömbindexeket, azonosítókat és sztringeket, a köztük álló pontokkal
    együtt. Így a `sb.append(x).toString()` és a több sorba tördelt lánc is
    egyben marad – ez utóbbin bukott el az első változat.
    """
    i = ws_vissza(src, pont - 1, tipus)
    if i < 0:
        return -1
    kezdet = None
    while True:
        haladt = False
        if maszk[i] and src[i] in ")]":
            o = nyito_parja(src, maszk, i)
            if o < 0:
                return -1
            kezdet = o
            i = o - 1
            haladt = True
        if i >= 0 and maszk[i] and (src[i].isalnum() or src[i] in "_$"):
            j = i
            while j >= 0 and maszk[j] and (src[j].isalnum() or src[j] in "_$"):
                j -= 1
            kezdet = j + 1
            i = j
            haladt = True
        elif not haladt and i >= 0 and tipus[i] == "s" and src[i] == '"':
            j = i - 1
            while j >= 0 and not (src[j] == '"' and tipus[j] == "s"):
                j -= 1
            if j < 0:
                return -1
            kezdet = j
            i = j - 1
            haladt = True
        if not haladt:
            break
        j = ws_vissza(src, i, tipus)
        if j >= 0 and maszk[j] and src[j] == ".":
            i = ws_vissza(src, j - 1, tipus)
            if i < 0:
                break
            continue
        break
    return -1 if kezdet is None else kezdet


def atir(src):
    """Egy forrásfájl átírása. Visszaadja: (új forrás, hány hívás)."""
    darab = 0
    # 1) Pattern.compile – egyszerű, egyértelmű csere.
    for regi in ("java.util.regex.Pattern.compile(", "Pattern.compile("):
        while True:
            tipus = kod_maszk(src)
            maszk = kod(tipus)
            hely = -1
            kezd = 0
            while True:
                p = src.find(regi, kezd)
                if p < 0:
                    break
                if maszk[p] and (p == 0 or src[p - 1] not in AZONOSITO):
                    hely = p
                    break
                kezd = p + 1
            if hely < 0:
                break
            nyito = hely + len(regi) - 1
            veg = zaro(src, maszk, nyito)
            if veg < 0:
                break
            args = argumentumok(src, maszk, nyito, veg)
            if len(args) not in (1, 2):
                # Nem tudjuk értelmezni: hagyjuk békén, de ne ragadjunk be.
                src = src[:hely] + "Pattern . compile(" + src[nyito + 1:]
                continue
            src = src[:hely] + "Rx.c(" + src[nyito + 1:]
            darab += 1
    src = src.replace("Pattern . compile(", "Pattern.compile(")

    # 2) A vevős hívások.
    valtozott = True
    while valtozott:
        valtozott = False
        tipus = kod_maszk(src)
        maszk = kod(tipus)
        for nev, (uj, db) in CEL.items():
            jel = "." + nev + "("
            kezd = 0
            while True:
                p = src.find(jel, kezd)
                if p < 0:
                    break
                kezd = p + 1
                if not maszk[p]:
                    continue
                nyito = p + len(jel) - 1
                veg = zaro(src, maszk, nyito)
                if veg < 0:
                    continue
                args = argumentumok(src, maszk, nyito, veg)
                if len(args) not in db:
                    continue
                eleje = vevo_kezdete(src, maszk, p, tipus)
                if eleje < 0 or eleje >= p:
                    continue
                # A vevő VÉGE nem a hívás pontja: a kettő között megjegyzés
                # is állhat („…substring(…)\n // magyarázat\n .matches(…)”).
                # Ha a megjegyzést is a vevőbe vennénk, az argumentumok a
                # megjegyzés MÖGÉ kerülnének – vagyis egy `//` sor belsejébe,
                # és a másolat fordíthatatlan lenne. A magyarázat az eredeti
                # forrásban marad meg; ez a másolat úgyis csak a fordítóé.
                vevo = src[eleje:ws_vissza(src, p - 1, tipus) + 1].strip()
                if not vevo or vevo.endswith("."):
                    continue
                # Már átírt hívást ne írjunk át újra.
                if vevo.startswith("Rx.") and vevo.endswith(")") is False:
                    continue
                src = (src[:eleje] + uj + "(" + vevo + ", "
                       + ", ".join(a.strip() for a in args) + src[veg:])
                darab += 1
                valtozott = True
                break
            if valtozott:
                break
    return src, darab


def main():
    if len(sys.argv) < 2:
        print("Használat: regexgyorsito.py <forráskönyvtár>", file=sys.stderr)
        return 2
    konyvtar = sys.argv[1]
    osszes = 0
    for nev in sorted(os.listdir(konyvtar)):
        if not nev.endswith(".java") or nev in ("Rx.java", "WebApi.java", "Bench.java"):
            continue
        ut = os.path.join(konyvtar, nev)
        src = open(ut, encoding="utf-8").read()
        uj, db = atir(src)
        if db:
            open(ut, "w", encoding="utf-8").write(uj)
            osszes += db
    print("%d regex-hívás gyorsítva" % osszes)
    return 0


if __name__ == "__main__":
    sys.exit(main())
