"""
POKETFT 데이터 추출 스크립트
PokeAPI에서 197종 실전 포켓몬 데이터 + 기술 데이터를 추출합니다.

사용법:
  pip install requests
  python fetch_pokemon.py

출력:
  pokemon_db.json  — 포켓몬 데이터
  moves_db.json    — 기술 데이터

추출 후 두 파일을 POKETFT/app/src/main/assets/ 폴더에 복사하세요.
"""

import requests
import json
import time

# 챔피언 & 실전 포켓몬 도감 번호 197개
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

BASE_URL = "https://pokeapi.co/api/v2"

def get_korean_name(species_url):
    """한국어 이름 추출"""
    try:
        resp = requests.get(species_url, timeout=10)
        data = resp.json()
        for name_entry in data.get("names", []):
            if name_entry["language"]["name"] == "ko":
                return name_entry["name"]
        return data["name"]
    except Exception:
        return f"??#{species_url.split('/')[-2]}"

def fetch_pokemon(poke_id):
    """포켓몬 1마리 데이터 추출"""
    url = f"{BASE_URL}/pokemon/{poke_id}"
    resp = requests.get(url, timeout=10)
    data = resp.json()

    # 종족값 [HP, Atk, Def, SpA, SpD, Spe]
    stats = [0] * 6
    stat_order = {"hp": 0, "attack": 1, "defense": 2,
                  "special-attack": 3, "special-defense": 4, "speed": 5}
    for s in data["stats"]:
        idx = stat_order.get(s["stat"]["name"])
        if idx is not None:
            stats[idx] = s["base_stat"]

    # 타입
    types = [t["type"]["name"] for t in data["types"]]

    # 배울 수 있는 기술 ID
    move_ids = []
    for m in data["moves"]:
        move_url = m["move"]["url"]
        move_id = int(move_url.rstrip("/").split("/")[-1])
        move_ids.append(move_id)

    # 한국어 이름
    name_ko = get_korean_name(data["species"]["url"])

    return {
        "id": data["id"],
        "dex_no": data["id"],
        "name_ko": name_ko,
        "stats": stats,
        "types": types,
        "learnable_moves": move_ids
    }

def fetch_move(move_id):
    """기술 1개 데이터 추출"""
    url = f"{BASE_URL}/move/{move_id}"
    resp = requests.get(url, timeout=10)
    data = resp.json()

    # 한국어 이름
    name_ko = data["name"]
    for name_entry in data.get("names", []):
        if name_entry["language"]["name"] == "ko":
            name_ko = name_entry["name"]
            break

    power = data.get("power") or 0
    move_type = data["type"]["name"]
    category = data["damage_class"]["name"]  # physical, special, status

    return {
        "id": data["id"],
        "name_ko": name_ko,
        "power": power,
        "type": move_type,
        "category": category
    }

def main():
    print(f"=== POKETFT 데이터 추출 시작 ===")
    print(f"대상: {len(target_ids)}종 포켓몬\n")

    # 1단계: 포켓몬 데이터 추출
    pokemon_list = []
    all_move_ids = set()

    for i, poke_id in enumerate(target_ids):
        print(f"[{i+1}/{len(target_ids)}] 포켓몬 #{poke_id} 추출 중...")
        try:
            poke = fetch_pokemon(poke_id)
            pokemon_list.append(poke)
            all_move_ids.update(poke["learnable_moves"])
            time.sleep(0.3)  # API 레이트 리밋 대응
        except Exception as e:
            print(f"  ❌ 오류: {e}")
            time.sleep(1)

    # 중간 저장
    with open("pokemon_db.json", "w", encoding="utf-8") as f:
        json.dump(pokemon_list, f, ensure_ascii=False, indent=2)
    print(f"\n✅ 포켓몬 {len(pokemon_list)}종 저장 완료!")

    # 2단계: 기술 데이터 추출 (위력이 있는 기술만 우선)
    print(f"\n기술 {len(all_move_ids)}개 추출 시작...")
    move_list = []

    for i, move_id in enumerate(sorted(all_move_ids)):
        if (i + 1) % 50 == 0:
            print(f"  [{i+1}/{len(all_move_ids)}] 진행 중...")
        try:
            move = fetch_move(move_id)
            if move["category"] in ("physical", "special"):  # 변화기 제외
                move_list.append(move)
            time.sleep(0.2)
        except Exception:
            time.sleep(0.5)

    with open("moves_db.json", "w", encoding="utf-8") as f:
        json.dump(move_list, f, ensure_ascii=False, indent=2)

    print(f"\n✅ 기술 {len(move_list)}개 저장 완료!")
    print(f"\n=== 추출 완료! ===")
    print(f"pokemon_db.json → POKETFT/app/src/main/assets/ 에 복사하세요")
    print(f"moves_db.json   → POKETFT/app/src/main/assets/ 에 복사하세요")

if __name__ == "__main__":
    main()
