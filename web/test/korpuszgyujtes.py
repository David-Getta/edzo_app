#!/usr/bin/env python3
"""Egyezés-korpusz gyűjtése a meglévő egységtesztekből.

Miért ezekből? Mert ezek a mondatok VALÓDI hibákból születtek: mindegyik
mögött ott van egy bejegyzés, ami egyszer elveszett vagy rosszul került a
naplóba. Ha a webes (iPhone-os) mag ezeken egyezik az Androidossal, akkor
nem egy kézzel válogatott, hízelgő mintán egyezik.

A kimenet szándékosan a verziókövetőbe kerül: így a webes teszt akkor is
ugyanazt méri, ha a tesztfájlok közben átrendeződnek.
"""

import os
import re
import sys

sys.path.insert(0, os.path.join(
    os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), "tools"))

from regexgyorsito import kod_maszk  # noqa: E402

TEST_DIR = "app/src/test/java/com/edzo/idozito"

# Egymás után álló, „+" jellel összefűzött Java sztringek – a tesztek így
# tördelik a hosszabb mondatokat.
LITERAL = re.compile(r'"((?:[^"\\]|\\.)*)"(?:\s*\+\s*"((?:[^"\\]|\\.)*)")*')
PIECE = re.compile(r'"((?:[^"\\]|\\.)*)"')


def unescape(s):
    out, i = [], 0
    while i < len(s):
        c = s[i]
        if c != "\\":
            out.append(c)
            i += 1
            continue
        n = s[i + 1] if i + 1 < len(s) else ""
        if n == "u":
            out.append(chr(int(s[i + 2:i + 6], 16)))
            i += 6
        elif n == "n":
            out.append("\n")
            i += 2
        elif n == "t":
            out.append("\t")
            i += 2
        elif n == "r":
            out.append("\r")
            i += 2
        else:
            out.append(n)
            i += 2
    # A Java forrásban az emoji két \uXXXX egységként áll; külön-külön
    # érvénytelen karakter, párba fűzve viszont egyetlen jel.
    joined = "".join(out)
    try:
        joined.encode("utf-8")
    except UnicodeEncodeError:
        joined = joined.encode("utf-16", "surrogatepass").decode("utf-16")
    return joined


def is_sentence(s):
    """Mondat-e, vagy csak egy minta / azonosító / mértékegység."""
    if len(s) < 8 or len(s) > 240:
        return False
    if " " not in s:
        return False
    # Reguláris kifejezések, formátumok, kódrészletek nem mondatok.
    for bad in ("\\", "(?", "%s", "%d", "{", "}", "  ·  ", "==", "\n",
                ".contains(", ".get(", ".size(", "assert", "->", "::",
                "[]", " + ", "()"):
        if bad in s:
            return False
    # Legyen benne betű és ne csak ASCII-technikai szöveg.
    if not re.search(r"[a-zA-ZáéíóöőúüűÁÉÍÓÖŐÚÜŰ]{3}", s):
        return False
    # A tesztek üzenetei („várt: …”) nem felhasználói mondatok.
    if s.strip().endswith(":") or s.startswith("A ") and s.endswith(" hibás"):
        return False
    # A tesztek VÁRT ÉRTÉKEI sem azok: az „1d+0 h12: 1×futas/50/10.0km"
    # nem egy naplósor, hanem a felismerés kimenete.
    if re.match(r"^\d+d[+-]\d+", s) or re.search(r"\d+×[a-z]+/\d", s):
        return False
    return True


def csak_kod(src):
    """A megjegyzések kiürítve, a hosszak megtartásával.

    Egy megjegyzésbe tévedt idézőjel („a \\s" alak) különben elcsúsztatná a
    literálok párosítását, és egész tesztfájlnyi mondat kimaradna a
    korpuszból – csendben, mert a kimenet így is hihetőnek látszik.
    """
    tipus = kod_maszk(src)
    return "".join(" " if tipus[i] == "m" else c for i, c in enumerate(src))


def main():
    if not os.path.isdir(TEST_DIR):
        print("Nem találom a tesztek könyvtárát: " + TEST_DIR, file=sys.stderr)
        return 1
    seen, out = set(), []
    for name in sorted(os.listdir(TEST_DIR)):
        if not name.endswith(".java"):
            continue
        text = csak_kod(open(os.path.join(TEST_DIR, name), encoding="utf-8").read())
        for m in LITERAL.finditer(text):
            joined = "".join(unescape(p) for p in PIECE.findall(m.group(0)))
            if is_sentence(joined) and joined not in seen:
                seen.add(joined)
                out.append(joined)
    for line in out:
        print(line)
    print("%d mondat" % len(out), file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
