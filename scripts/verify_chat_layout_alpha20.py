#!/usr/bin/env python3
"""Guard against the local performance panel accidentally returning to the chat footer."""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
FILE = ROOT / 'app/src/main/res/layout/activity_chat.xml'
ANDROID = '{http://schemas.android.com/apk/res/android}'


def android_id(element):
    return element.attrib.get(ANDROID + 'id', '')


def validate(path=FILE):
    root = ET.parse(path).getroot()
    assert root.tag == 'LinearLayout', 'Chat root must stay LinearLayout'
    elements = list(root.iter())
    identifiers = [android_id(node) for node in elements if android_id(node)]
    assert len(identifiers) == len(set(identifiers)), 'Duplicated layout id'

    def get(identifier):
        matches = [node for node in elements if android_id(node) == '@+id/' + identifier]
        assert len(matches) == 1, f'Missing or duplicate id: {identifier}'
        return matches[0]

    panel = get('performanceHeader')
    card = next((child for child in root if panel in list(child.iter())), None)
    assert card is not None and card.tag.endswith('MaterialCardView'), 'Card must be directly anchored in chat root'
    children = list(root)
    panel_pos = children.index(card)
    messages_pos = children.index(get('messages'))
    state_pos = children.index(get('stateLabel'))
    composer = next(child for child in root if get('promptInput') in list(child.iter()))
    composer_pos = children.index(composer)

    assert card is children[messages_pos - 1], 'Performance card should sit just before history'
    assert any(node.tag == 'TextView' and 'IA LOCAL ATIVA' in node.attrib.get(ANDROID + 'text','')
               for node in children[:panel_pos]), 'Local AI indicator must be above performance panel'
    assert 0 < panel_pos < messages_pos < state_pos < composer_pos, 'Performance must be at top; composer at bottom'
    assert get('messages').attrib[ANDROID + 'layout_height'] == '0dp', 'History must be weighted'
    assert get('messages').attrib[ANDROID + 'layout_weight'] == '1', 'History must fill remaining space'
    assert get('performanceDetails').attrib[ANDROID + 'visibility'] == 'gone', 'Details should start collapsed'
    assert get('performanceToggle').attrib[ANDROID + 'clickable'] == 'true', 'Expand/collapse control must be clickable'
    for i in ('performanceSpeed', 'performancePss', 'performanceBattery', 'performanceRamSegments',
              'performanceRamLabel', 'performanceThermalSummary', 'performanceDetails',
              'pdfAttachments', 'promptInput', 'sendMessage', 'stopGeneration'):
        get(i)
    return panel_pos, messages_pos, composer_pos


if __name__ == '__main__':
    print('OK: Desempenho local no topo, mensagens flexíveis e editor no rodapé:', validate())
