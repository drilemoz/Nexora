#!/usr/bin/env python3
from pathlib import Path
import re
r=Path(__file__).resolve().parents[1]
pdf=(r/'app/src/main/java/br/com/adriedson/nexora/PdfPrompt.kt').read_text()
assert 'if (pdfs.isEmpty() && history.isEmpty()) return question' in pdf
export=(r/'app/src/main/java/br/com/adriedson/nexora/ConversationExport.kt').read_text()
assert '.put("inferenceAtLoad",' in export
assert 'appliedInference: ModelInferenceSettings?' in export
model=(r/'app/src/main/java/br/com/adriedson/nexora/ModelSession.kt').read_text()
assert 'appliedInferenceSettings = inference' in model
assert 'appliedInferenceSettings = null' in model
assert re.search(r'versionCode\s*=\s*25', (r/'app/build.gradle.kts').read_text())
print('Nexora alpha.25: fluxo de pergunta e captura de parametros confirmados')
