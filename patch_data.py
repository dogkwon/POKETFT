"""
POKETFT 데이터 한국어 패치 스크립트
- 특성 12건, 기술 26건의 누락된 한국어명을 수동 패치
"""
import json
import shutil
import os

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
POKEMON_PATH = os.path.join(SCRIPT_DIR, "pokemon_db.json")
MOVES_PATH = os.path.join(SCRIPT_DIR, "moves_db.json")
ASSETS_DIR = os.path.join(SCRIPT_DIR, "app", "src", "main", "assets")

# ── 특성 한국어 패치 (name_en → name_ko) ──
ABILITY_PATCHES = {
    "sharpness": "예리함",
    "lingering-aroma": "감도는향기",
    "purifying-salt": "깨끗한소금",
    "wind-power": "풍력발전",
    "anger-shell": "분노의껍질",
    "opportunist": "편승",
    "rocky-payload": "바위나르기",
    "earth-eater": "흙먹기",
    "toxic-debris": "독치장",
    "protosynthesis": "고대활성",
    "good-as-gold": "황금몸",
}

# ── 기술 한국어 패치 (ID → name_ko) ──
MOVE_PATCHES = {
    827: "비장의발톱",
    828: "사이코배리어러시",
    830: "돌도끼",
    833: "대분노",
    834: "웨이브태클",
    838: "맹돌진",
    839: "독침난사",
    851: "테라버스트",
    853: "도끼차기",
    855: "루미나콜리전",
    861: "아이스스피너",
    864: "소금절이",
    866: "목숨걸기회전",
    870: "플라워트릭",
    871: "플레어송",
    872: "아쿠아스텝",
    873: "레이징불",
    874: "골드러시",
    884: "덤벼들기",
    885: "풀묻히기",
    886: "찬물끼얹기",
    890: "아머캐논",
    892: "전기방출",
    893: "기가톤해머",
    894: "분함의역습",
    895: "아쿠아커터",
}

def main():
    # 포켓몬 로드 & 특성 패치
    with open(POKEMON_PATH, "r", encoding="utf-8") as f:
        pokemons = json.load(f)

    ab_count = 0
    for p in pokemons:
        for ab in p.get("abilities", []):
            if ab["name_en"] in ABILITY_PATCHES:
                old = ab["name_ko"]
                ab["name_ko"] = ABILITY_PATCHES[ab["name_en"]]
                print(f"  특성 패치: #{p['dex_no']} {p['name_ko']} | {old} → {ab['name_ko']}")
                ab_count += 1

    with open(POKEMON_PATH, "w", encoding="utf-8") as f:
        json.dump(pokemons, f, ensure_ascii=False, indent=1)
    print(f"\n특성 {ab_count}건 패치 완료!\n")

    # 기술 로드 & 패치
    with open(MOVES_PATH, "r", encoding="utf-8") as f:
        moves = json.load(f)

    mv_count = 0
    for m in moves:
        if m["id"] in MOVE_PATCHES:
            old = m["name_ko"]
            m["name_ko"] = MOVE_PATCHES[m["id"]]
            print(f"  기술 패치: ID {m['id']} | {old} → {m['name_ko']}")
            mv_count += 1

    with open(MOVES_PATH, "w", encoding="utf-8") as f:
        json.dump(moves, f, ensure_ascii=False, indent=1)
    print(f"\n기술 {mv_count}건 패치 완료!\n")

    # assets 폴더에 복사
    if os.path.isdir(ASSETS_DIR):
        shutil.copy(POKEMON_PATH, ASSETS_DIR)
        shutil.copy(MOVES_PATH, ASSETS_DIR)
        print("assets/ 폴더에 자동 복사 완료!")
    else:
        print(f"assets/ 없음 — 수동 복사 필요: {ASSETS_DIR}")

if __name__ == "__main__":
    main()
