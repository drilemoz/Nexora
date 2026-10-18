#!/usr/bin/env python3
"""Regressao para namespace de insets MaterialButton: app:inset* falha no AAPT2."""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent
ANDROID = "{http://schemas.android.com/apk/res/android}"
APP = "{http://schemas.android.com/apk/res-auto}"
FILES = sorted((ROOT / "app/src/main/res/layout").glob("*.xml"))
assert FILES, "Layouts Android nao encontrados"
for path in FILES:
    root = ET.parse(path).getroot()
    for elem in root.iter():
        if elem.tag.endswith("MaterialButton"):
            for name in ("insetTop", "insetBottom", "insetLeft", "insetRight"):
                assert APP + name not in elem.attrib, f"{path}: atributo app:{name} invalido para MaterialButton"
layout = ET.parse(ROOT / "app/src/main/res/layout/item_conversation.xml").getroot()
for ident in ("historySelectMode", "historySelectAll"):
    nodes = [e for e in layout.iter() if e.attrib.get(ANDROID + "id") == "@+id/" + ident]
    assert len(nodes) == 1 and nodes[0].tag.endswith("MaterialButton"), ident
    for attr in ("insetTop", "insetBottom"):
        assert nodes[0].attrib.get(ANDROID + attr) == "0dp", f"{ident} android:{attr} nao configurado"
import re
build = (ROOT / "app/build.gradle.kts").read_text()
version = re.search(r'versionName\s*=\s*"([^"]+)"', build)
assert version is not None and version.group(1).startswith('0.1.0-alpha.') and int(version.group(1).rsplit('.', 1)[1]) >= 24, "versao Gradle incorreta"
export = (ROOT / "app/src/main/java/br/com/adriedson/nexora/ConversationExport.kt").read_text()
assert '.put("appVersion", "' + version.group(1) + '")' in export, "exportacao com versao desatualizada"
print(f"OK: {len(FILES)} layouts, insets MaterialButton validos, versao e exportacao coerentes.")
