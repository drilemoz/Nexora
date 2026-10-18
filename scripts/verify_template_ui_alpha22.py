#!/usr/bin/env python3
"""Regressão local: integração da UI, guardas de privacidade e versionamento."""
from pathlib import Path
import xml.etree.ElementTree as ET
r=Path(__file__).resolve().parent.parent
xmls=list((r/'app/src/main/res').rglob('*.xml'))+[r/'app/src/main/AndroidManifest.xml']
for p in xmls: ET.parse(p)
manifest=(r/'app/src/main/AndroidManifest.xml').read_text()
settings=(r/'app/src/main/res/layout/activity_settings.xml').read_text()
view=(r/'app/src/main/res/layout/activity_model_compatibility.xml').read_text()
activity=(r/'app/src/main/java/br/com/adriedson/nexora/ModelCompatibilityActivity.kt').read_text()
session=(r/'app/src/main/java/br/com/adriedson/nexora/ModelSession.kt').read_text()
boot=(r/'scripts/bootstrap.sh').read_text()
checks={
 '47 XML':len(xmls)>=47,
 'manifest':'.ModelCompatibilityActivity' in manifest,
 'settings route':'openModelCompatibility' in settings,
 'fields': all(x in view for x in ['templateModel','templateName','templateOrigin','templateApplication','templateEngine','templateCompatibility']),
 'buttons':'verifyTemplateButton' in view and 'copyTemplateButton' in view,
 'manual restricted':'android:enabled="false"' in view and 'Personalizado (avançado)' in view,
 'native inspection':'.templateReport(true)' in session and '.templateReport(false)' in session,
 'private logs':'"template.detect"' in session and '"template.verify"' in session and '"template.apply"' in session,
 'clipboard':'ClipData.newPlainText' in activity,
 'bootstrap patch':'patch_llama_templates.py' in boot,
 'version':'versionCode = 22' in (r/'app/build.gradle.kts').read_text(),
 'workflow check':'verify_template_alpha22.py' in (r/'.github/workflows/android-apk.yml').read_text(),
}
for k,v in checks.items(): print(('OK  ' if v else 'FAIL ')+k)
if not all(checks.values()):raise SystemExit(1)
