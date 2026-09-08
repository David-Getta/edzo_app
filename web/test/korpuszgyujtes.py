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
    return True


def main():
    if not os.path.isdir(TEST_DIR):
        print("Nem találom a tesztek könyvtárát: " + TEST_DIR, file=sys.stderr)
        return 1
    seen, out = set(), []
    for name in sorted(os.listdir(TEST_DIR)):
        if not name.endswith(".java"):
            continue
        text = open(os.path.join(TEST_DIR, name), encoding="utf-8").read()
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
