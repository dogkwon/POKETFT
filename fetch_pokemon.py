"""
POKETFT 데이터 추출 스크립트 v2
PokeAPI에서 197종 실전 포켓몬 + 기술 + 특성(Ability) 추출

사용법: python fetch_pokemon.py
출력: pokemon_db.json, moves_db.json
- 중단 후 재실행 시 이미 수집된 ID는 스킵
- 종마다 pokemon_db.json 저장 (체크포인트)
"""

import requests
import json
import time
import os

target_ids = [
    3, 6, 9, 15, 18, 24, 25, 26, 36, 38, 59, 65, 68, 71, 80, 94, 115, 121, 127, 128,
    130, 132, 134, 135, 136, 142, 143, 149, 154, 157, 160, 168, 181, 184, 186, 196,
    197, 199, 205, 208, 212, 214, 227, 229, 248, 279, 282, 302, 306, 308, 310, 319,
    323, 324, 334, 350, 351, 354, 358, 359, 362, 389, 392, 395, 405, 407, 409, 411,
    428, 442, 445, 448, 450, 454, 460, 461, 464, 470, 471, 472, 473, 475, 478, 479,
    497, 500, 503, 505, 510, 512, 514, 516, 530, 531, 534, 547, 553, 563, 569, 571,
    579, 584, 587, 609, 614, 618, 623, 635, 637, 652, 655, 658, 660, 663, 666, 670,
    671, 675, 676, 678, 681, 683, 685, 693, 695, 697, 699, 700, 701, 702, 706, 707,
    709, 711, 713, 715, 724, 727, 730, 733, 740, 745, 748, 750, 752, 758, 763, 765,
    766, 778, 780, 784, 823, 841, 842, 844, 855, 858, 866, 867, 869, 877, 887, 899,
    900, 901, 903, 904, 907, 908, 911, 914, 916, 923, 934, 936, 941, 944, 945, 950,
    951, 952, 953, 954, 955, 956, 957, 959, 960, 962, 967, 968, 970, 975, 980, 984, 1000
]

BASE = "https://pokeapi.co/api/v2"
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
POKEMON_PATH = os.path.join(SCRIPT_DIR, "pokemon_db.json")

_species_cache = {}
_ability_cache = {}


def load_existing():
    if not os.path.isfile(POKEMON_PATH):
        return []
    try:
        with open(POKEMON_PATH, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return []


def save_pokemon_list(pokemon_list):
    with open(POKEMON_PATH, "w", encoding="utf-8") as f:
        json.dump(pokemon_list, f, ensure_ascii=False, indent=1)


def get_ko_name(species_url):
    if species_url in _species_cache:
        return _species_cache[species_url]
    try:
        data = requests.get(species_url, timeout=15).json()
        for n in data.get("names", []):
            if n["language"]["name"] == "ko":
                _species_cache[species_url] = n["name"]
                return n["name"]
        _species_cache[species_url] = data["name"]
        return data["name"]
    except Exception:
        return "???"


def get_ability_ko(ability_url):
    if ability_url in _ability_cache:
        return _ability_cache[ability_url]
    try:
        data = requests.get(ability_url, timeout=15).json()
        name_ko = data["name"]
        for n in data.get("names", []):
            if n["language"]["name"] == "ko":
                name_ko = n["name"]
                break
        _ability_cache[ability_url] = name_ko
        return name_ko
    except Exception:
        return "???"


def fetch_pokemon(pid):
    data = requests.get(f"{BASE}/pokemon/{pid}", timeout=15).json()

    stat_map = {"hp": 0, "attack": 1, "defense": 2, "special-attack": 3, "special-defense": 4, "speed": 5}
    stats = [0] * 6
    for s in data["stats"]:
        idx = stat_map.get(s["stat"]["name"])
        if idx is not None:
            stats[idx] = s["base_stat"]

    types = [t["type"]["name"] for t in data["types"]]

    move_ids = []
    for m in data["moves"]:
        mid = int(m["move"]["url"].rstrip("/").split("/")[-1])
        move_ids.append(mid)

    abilities = []
    for a in data["abilities"]:
        ab_name_ko = get_ability_ko(a["ability"]["url"])
        abilities.append({
            "name_ko": ab_name_ko,
            "name_en": a["ability"]["name"],
            "is_hidden": a["is_hidden"]
        })

    name_ko = get_ko_name(data["species"]["url"])

    return {
        "id": data["id"],
        "dex_no": data["id"],
        "name_ko": name_ko,
        "stats": stats,
        "types": types,
        "abilities": abilities,
        "learnable_moves": move_ids
    }


def fetch_move(mid):
    data = requests.get(f"{BASE}/move/{mid}", timeout=15).json()
    name_ko = data["name"]
    for n in data.get("names", []):
        if n["language"]["name"] == "ko":
            name_ko = n["name"]
            break
    return {
        "id": data["id"],
        "name_ko": name_ko,
        "power": data.get("power") or 0,
        "type": data["type"]["name"],
        "category": data["damage_class"]["name"]
    }


def main():
    print(f"=== POKETFT v2 데이터 추출 ===")
    print(f"대상: {len(target_ids)}종\n")

    pokemon_list = load_existing()
    done_ids = {p["id"] for p in pokemon_list}
    print(f"기존 {len(pokemon_list)}종 로드됨, {len(target_ids) - len(done_ids)}종 남음\n")

    all_move_ids = set()
    for p in pokemon_list:
        all_move_ids.update(p.get("learnable_moves", []))

    for i, pid in enumerate(target_ids):
        if pid in done_ids:
            continue
        print(f"[{len(done_ids)+1}/{len(target_ids)}] #{pid} ...", end=" ", flush=True)
        try:
            p = fetch_pokemon(pid)
            pokemon_list.append(p)
            done_ids.add(pid)
            all_move_ids.update(p["learnable_moves"])
            save_pokemon_list(pokemon_list)
            print(f"{p['name_ko']} (특성 {len(p['abilities'])}개)")
            time.sleep(0.2)
        except Exception as e:
            print(f"FAIL {e}")
            time.sleep(1)

    print(f"\n포켓몬 {len(pokemon_list)}종 저장!")

    moves_path = os.path.join(SCRIPT_DIR, "moves_db.json")
    print(f"\n기술 {len(all_move_ids)}개 추출...")
    move_list = []
    for i, mid in enumerate(sorted(all_move_ids)):
        if (i + 1) % 100 == 0:
            print(f"  [{i+1}/{len(all_move_ids)}]")
        try:
            m = fetch_move(mid)
            if m["category"] in ("physical", "special"):
                move_list.append(m)
            time.sleep(0.08)
        except Exception:
            time.sleep(0.3)

    with open(moves_path, "w", encoding="utf-8") as f:
        json.dump(move_list, f, ensure_ascii=False, indent=1)

    print(f"기술 {len(move_list)}개 저장!")

    assets = os.path.join(SCRIPT_DIR, "app", "src", "main", "assets")
    if os.path.isdir(assets):
        import shutil
        shutil.copy(POKEMON_PATH, assets)
        shutil.copy(moves_path, assets)
        print(f"\nassets/ 폴더에 자동 복사 완료!")
    else:
        print(f"\nassets/ 없음 — 수동 복사: {assets}")


if __name__ == "__main__":
    main()
