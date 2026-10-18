#!/usr/bin/env python3
"""Regressão estática alpha.23, não substitui Android build/device."""
from pathlib import Path
from xml.etree import ElementTree as ET
root = Path(__file__).resolve().parents[1]
ns = '{http://schemas.android.com/apk/res/android}'
view = ET.parse(root/'app/src/main/res/layout/activity_chat.xml').getroot()
items = ET.parse(root/'app/src/main/res/layout/item_conversation.xml').getroot()
def id_find(parent, value):
    return next((v for v in parent.iter() if v.get(ns+'id') == '@+id/'+value), None)
assert id_find(view, 'exportChat') is not None
assert id_find(view, 'performanceOverview') is not None
assert id_find(view, 'performanceMinimize') is not None
assert id_find(view, 'performanceToggle') is not None
assert id_find(view, 'performanceDetails') is not None
assert id_find(view, 'messages') is not None
assert id_find(items, 'historySelectMode').tag.endswith('MaterialButton')
assert id_find(items, 'historySelectAll').tag.endswith('MaterialButton')
chat = (root/'app/src/main/java/br/com/adriedson/nexora/ChatActivity.kt').read_text()
exports = (root/'app/src/main/java/br/com/adriedson/nexora/ConversationExport.kt').read_text()
assert 'setTitle("Exportar conversa e diagnóstico?")' in chat
assert 'setPositiveButton("Exportar")' in chat
assert 'Intent.FLAG_GRANT_READ_URI_PERMISSION' in chat
assert 'put("content", item.text)' in exports
assert 'put("scope", "somente_ultima_resposta_desta_sessao_se_disponivel")' in exports
assert 'pdfs(' not in exports and 'PdfAttachment' not in exports
assert 'performance_panel_minimized' in chat
assert 'performanceOverview.visibility = if (performanceIsMinimized) View.GONE else View.VISIBLE' in chat
assert '0.1.0-alpha.23' in (root/'app/build.gradle.kts').read_text()
print('OK: 14 verificações de exportação, painel, controles e versão')
