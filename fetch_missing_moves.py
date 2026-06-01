"""
포켓몬DB에서 참조되는 기술 ID 중 moves_db.json에 없는 것들을
PokeAPI에서 가져와서 보충하는 스크립트
"""
import json, time, urllib.request, urllib.error

POKEMON_DB = 'app/src/main/assets/pokemon_db.json'
MOVES_DB   = 'app/src/main/assets/moves_db.json'

# 현재 보유한 기술
with open(MOVES_DB, encoding='utf-8') as f:
    existing = json.load(f)

have_ids = {m['id'] for m in existing}
moves_map = {m['id']: m for m in existing}

# pokemon_db에서 참조하는 전체 기술 ID
with open(POKEMON_DB, encoding='utf-8') as f:
    pdb = json.load(f)

all_needed = set()
for p in pdb:
    all_needed.update(p.get('learnable_moves', []))

missing_ids = sorted(all_needed - have_ids)
print(f"현재 보유: {len(have_ids)}개  /  필요: {len(all_needed)}개  /  누락: {len(missing_ids)}개")

# 한국어 카테고리 매핑
def parse_category(damage_class):
    if damage_class == 'physical': return 'physical'
    if damage_class == 'special':  return 'special'
    return 'status'

# 접촉기 여부 가져오기
CONTACT_FLAG = 'contact'

def fetch_move(move_id):
    url = f'https://pokeapi.co/api/v2/move/{move_id}/'
    for attempt in range(3):
        try:
            req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(req, timeout=10) as resp:
                data = json.loads(resp.read().decode('utf-8'))
            
            # 한국어 이름 찾기
            name_ko = ''
            for entry in data.get('names', []):
                if entry['language']['name'] == 'ko':
                    name_ko = entry['name']
                    break
            if not name_ko:
                # ko 없으면 ja 또는 en 사용
                for entry in data.get('names', []):
                    if entry['language']['name'] == 'en':
                        name_ko = entry['name']
                        break

            power = data.get('power') or 0
            move_type = data.get('type', {}).get('name', 'normal')
            category = parse_category(data.get('damage_class', {}).get('name', 'status'))
            
            flags = [f['name'] for f in data.get('meta', {}).get('flags', [])] if 'meta' in data else []
            # 실제 flags 필드 위치
            is_contact = CONTACT_FLAG in [f['name'] for f in data.get('flags', [])]

            return {
                'id': move_id,
                'name_ko': name_ko,
                'power': power,
                'type': move_type,
                'category': category,
                'is_contact': is_contact
            }
        except Exception as e:
            print(f"  재시도 {attempt+1}/3 (move {move_id}): {e}")
            time.sleep(1)
    return None

# 누락 기술 가져오기
fetched = []
fail_ids = []
for i, mid in enumerate(missing_ids):
    move = fetch_move(mid)
    if move:
        # power==0이고 status인 기술은 스킵하지 않고 포함 (일단 다 넣음)
        fetched.append(move)
        print(f"[{i+1}/{len(missing_ids)}] ID={mid} {move['name_ko']} (위력:{move['power']}, {move['category']})")
    else:
        fail_ids.append(mid)
        print(f"[{i+1}/{len(missing_ids)}] ID={mid} FAILED")
    time.sleep(0.15)  # API rate limit 방지

print(f"\n성공: {len(fetched)}개, 실패: {len(fail_ids)}개")
if fail_ids:
    print("실패 ID:", fail_ids)

# power > 0인 것만 추가 (status기술 제외 — 계산기 목적상)
added = [m for m in fetched if m['power'] > 0]
print(f"위력 있는 기술(추가 대상): {len(added)}개")

# 기존과 합치기
all_moves = list(moves_map.values()) + added
all_moves.sort(key=lambda m: m['id'])

with open(MOVES_DB, 'w', encoding='utf-8') as f:
    json.dump(all_moves, f, ensure_ascii=False, indent=2)

print(f"\n완료! moves_db.json: {len(all_moves)}개 기술")
