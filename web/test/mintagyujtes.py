#!/usr/bin/env python3
"""A felismerés összes reguláris kifejezésének összegyűjtése egy fájlba.

Miért? A webes változat egy gyorsítót használ (Rx.kihagyhato): ha a minta
kötelező szava nincs a mondatban, a keresést el se indítja. Ez a gyorsító
akkor helyes, ha SOHA nem mond nemet olyan mintára, ami mégis illeszkedne –
és ezt csak úgy lehet bizonyítani, ha minden mintát összevetünk minden
mondattal. Ez a szkript adja hozzá a mintákat; a mondatokat a korpusz.

A minták a Java forrás sztringliteráljaiból jönnek, a `+` jellel összefűzött
darabokat egybeolvasva. Ami nem fordul le reguláris kifejezésként (mert
csak egy szövegdarab volt), azt az ellenőrző kihagyja.

Használat:  python3 web/test/mintagyujtes.py <kimeneti fájl>
"""

import os
import re
import sys

GYOKER = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.insert(0, os.path.join(GYOKER, "tools"))

from regexgyorsito import kod_maszk  # noqa: E402


def literalok(src):
    """A `+` jellel összefűzött sztringliterálok összeolvasva."""
    tipus = kod_maszk(src)
    ki = []
    i, n = 0, len(src)
    while i < n:
        if tipus[i] == "s" and src[i] == '"':
            j = i + 1
            while j < n and not (src[j] == '"' and tipus[j] == "s"):
                j += 1
            darab = [src[i + 1:j]]
            k = j + 1
            while True:
                m = re.match(r"\s*\+\s*", src[k:])
                if not m:
                    break
                p = k + m.end()
                if p >= n or tipus[p] != "s" or src[p] != '"':
                    break
                q = p + 1
                while q < n and not (src[q] == '"' and tipus[q] == "s"):
                    q += 1
                darab.append(src[p + 1:q])
                k = q + 1
            ki.append("".join(darab))
            i = k
        else:
            i += 1
    return ki


def java_ertek(t):
    """A Java sztringliterál escape-jei feloldva."""
    return (t.replace('\\"', '"').replace("\\n", "\n")
             .replace("\\t", "\t").replace("\\\\", "\\"))


def main():
    if len(sys.argv) < 2:
        print("Használat: mintagyujtes.py <kimeneti fájl>", file=sys.stderr)
        return 2
    forras = os.path.join(GYOKER, "app", "src", "main", "java", "com", "edzo", "idozito")
    mintak = set()
    for f in sorted(os.listdir(forras)):
        if not f.endswith(".java"):
            continue
        src = open(os.path.join(forras, f), encoding="utf-8").read()
        for lit in literalok(src):
            m = java_ertek(lit)
            # Sortörés nincs egyetlen mintában sem; a fájl sorokból áll.
            if len(m) >= 3 and "\n" not in m and re.search(r"[\\\[(){}|*+?]", m):
                mintak.add(m)
    with open(sys.argv[1], "w", encoding="utf-8") as f:
        for m in sorted(mintak):
            f.write(m + "\n")
    print(len(mintak))
    return 0


if __name__ == "__main__":
    sys.exit(main())
