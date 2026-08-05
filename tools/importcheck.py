#!/usr/bin/env python3
"""Hiányzó import-ok keresése a UI-osztályokban.

Az egységtesztek csak a tiszta Java logikát fordítják; az Activity-k
Android-osztályokat használnak, és azok csak a CI-ben fordulnak le. Egy
hiányzó import így hét percbe kerül – ez a szkript másodpercekbe.

A módszer szándékosan szűk: néhány gyakori Android-típusra keres VALÓDI
kódmintát (`new EditText(`, `EditText et`, `EditText[]`, `(EditText)`), és
csak azt kérdezi, van-e hozzá import. A teljes névvel írt hivatkozás
(android.widget.EditText) rendben van, azt a pont-tiltó előfeltétel kizárja.

Szándékosan NEM próbáljuk a szöveges literálokat kiszűrni: egy elrontott
literál-minta némán elnyelné a fél fájlt, és a szkript hallgatna arról, ami
miatt megírtuk. Helyette a sor `//` utáni részét hagyjuk figyelmen kívül.

Használat a repó gyökeréből:  python3 tools/importcheck.py
"""

import os
import re
import sys

SRC = "app/src/main/java/com/edzo/idozito"

# Rövid név → a hozzá tartozó import. Csak azok, amiket tényleg használunk.
TYPES = {
    "EditText": "android.widget.EditText",
    "Button": "android.widget.Button",
    "TextView": "android.widget.TextView",
    "ImageView": "android.widget.ImageView",
    "ScrollView": "android.widget.ScrollView",
    "HorizontalScrollView": "android.widget.HorizontalScrollView",
    "LinearLayout": "android.widget.LinearLayout",
    "FrameLayout": "android.widget.FrameLayout",
    "Switch": "android.widget.Switch",
    "SeekBar": "android.widget.SeekBar",
    "Toast": "android.widget.Toast",
    "GradientDrawable": "android.graphics.drawable.GradientDrawable",
    "Typeface": "android.graphics.Typeface",
    "Canvas": "android.graphics.Canvas",
    "Paint": "android.graphics.Paint",
    "Gravity": "android.view.Gravity",
    "InputType": "android.text.InputType",
    "Uri": "android.net.Uri",
    "Handler": "android.os.Handler",
    "Looper": "android.os.Looper",
    "JSONObject": "org.json.JSONObject",
    "JSONArray": "org.json.JSONArray",
}


def code_lines(src):
    """A sorok kód-része: a `//` utáni farok és a blokk-megjegyzések nélkül."""
    src = re.sub(r"/\*.*?\*/", " ", src, flags=re.S)
    for line in src.split("\n"):
        cut = line.find("//")
        yield line if cut < 0 else line[:cut]


def used_short(src, short):
    """Van-e valódi, rövid néven írt hivatkozás a típusra?"""
    pats = [
        r"(?<![\w.])new\s+" + short + r"\s*[(\[]",        # new EditText(  /  new EditText[
        r"(?<![\w.])" + short + r"\s*\[\s*\]",            # EditText[]
        r"(?<![\w.])" + short + r"\s+[A-Za-z_]\w*\s*[=;,)]",  # EditText et =
        r"\(\s*" + short + r"\s*\)\s*[A-Za-z_(]",         # (EditText) v
        r"(?<![\w.])" + short + r"\s*\.\s*[A-Z_]",        # Gravity.CENTER, InputType.TYPE_
    ]
    for line in code_lines(src):
        for p in pats:
            if re.search(p, line):
                return True
    return False


# API 24 a legalacsonyabb támogatott szint: ezek a hívások csak újabb
# Androidon léteznek, és a régin futásidőben dobnak (a fordítás átmegy).
API_BANNED = {
    r"(?<![\w.])String\.join\s*\(": "String.join (API 26) – írj kézi összefűzést",
    r"(?<![\w.])Objects\.requireNonNullElse\s*\(": "Objects.requireNonNullElse (API 30)",
    r"(?<![\w.])List\.of\s*\(": "List.of (API 30) – használj Arrays.asList-et",
    r"(?<![\w.])Map\.of\s*\(": "Map.of (API 30)",
    r"(?<![\w.])Set\.of\s*\(": "Set.of (API 30)",
    r"\.stream\s*\(\s*\)": "Stream API (API 24+ csak részben) – írj ciklust",
}


def main():
    problems = []
    for name in sorted(os.listdir(SRC)):
        if not name.endswith(".java"):
            continue
        raw = open(os.path.join(SRC, name), encoding="utf-8").read()
        imports = set(re.findall(r"import\s+([\w.]+);", raw))
        for short, full in TYPES.items():
            if full in imports or full.rsplit(".", 1)[0] + ".*" in imports:
                continue
            if used_short(raw, short):
                problems.append(f"{name}: {short} rövid néven, de nincs import ({full})")
        for line in code_lines(raw):
            for pat, why in API_BANNED.items():
                if re.search(pat, line):
                    problems.append(f"{name}: {why}")

    if problems:
        print("Hiányzó import:")
        for p in problems:
            print("  " + p)
        return 1
    print("Rendben: minden rövid néven használt Android-típushoz van import.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
