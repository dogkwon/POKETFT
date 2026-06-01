import json

pdb = json.load(open('app/src/main/assets/pokemon_db.json', encoding='utf-8'))
mdb = json.load(open('app/src/main/assets/moves_db.json', encoding='utf-8'))
moves_map = {m['id']: m for m in mdb}

charizard = next(x for x in pdb if x.get('name_ko','') and '리자몽' in x['name_ko'] and '메가' not in x['name_ko'])
learnable = charizard['learnable_moves']
found = [moves_map[i] for i in learnable if i in moves_map and moves_map[i]['power'] > 0]
print(f'learnable_moves IDs: {len(learnable)}')
print(f'found in moves_db with power>0: {len(found)}')
print('moves:')
for m in found:
    print(f"  id={m['id']} {m['name_ko']} power={m['power']} type={m['type']}")
