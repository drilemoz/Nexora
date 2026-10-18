#!/usr/bin/env python3
"""Regressions for conversation history UI and bulk deletion wiring."""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
NS='{http://schemas.android.com/apk/res/android}'
def res(root,name):
    result=[el for el in root.iter() if el.get(NS+'id')=='@+id/'+name]
    assert len(result)==1,(name,len(result))
    return result[0]

def main():
    home=ET.parse(ROOT/'app/src/main/res/layout/activity_main.xml').getroot()
    item=ET.parse(ROOT/'app/src/main/res/layout/item_conversation.xml').getroot()
    title=[x for x in home.iter() if x.get(NS+'text')=='Nexora']
    assert len(title)==1
    model=res(home,'activeModelName')
    assert list(home.iter()).index(title[0]) < list(home.iter()).index(model)
    row=res(item,'conversationSectionRow')
    assert res(item,'conversationSection') in list(row.iter())
    for name in ['historySelectMode','historySelectAll','historyBulkDelete']:
        assert res(item,name) in list(row.iter())
    for name in ['conversationSelected','conversationIcon','conversationTitle','conversationModel','conversationMenu']:
        res(item,name)
    store=(ROOT/'app/src/main/java/br/com/adriedson/nexora/ChatStore.kt').read_text()
    activity=(ROOT/'app/src/main/java/br/com/adriedson/nexora/MainActivity.kt').read_text()
    date=(ROOT/'app/src/main/java/br/com/adriedson/nexora/ConversationDateGroups.kt').read_text()
    assert 'db.beginTransaction()' in store and 'db.setTransactionSuccessful()' in store
    assert 'fun deleteMany(' in store and 'fun deleteAll()' in store
    assert 'withContext(Dispatchers.IO)' in activity and 'NexoraDialogs.confirmDelete' in activity
    assert 'ConversationDateGroups.section' in activity and 'fun section(' in date
    assert 'Nenhum modelo carregado' in activity
    assert 'selectedIds' in activity and 'toggleAllSelection' in activity
    print('OK: cabeçalho do modelo, ações em seção de histórico, seleção e exclusão transacional')
if __name__=='__main__': main()
