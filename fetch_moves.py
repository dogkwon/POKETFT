"""
기술 추출 + 자동 복사 (포켓몬 데이터는 이미 완료됨)
"""
import requests, json, time, os, shutil

BASE = "https://pokeapi.co/api/v2"

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
    script_dir = os.path.dirname(os.path.abspath(__file__))
    pokemon_path = os.path.join(script_dir, "pokemon_db.json")

    # pokemon_db.json 에서 전체 기술 ID 추출
    with open(pokemon_path, "r", encoding="utf-8") as f:
        pokemons = json.load(f)
    print(f"Pokemon {len(pokemons)} loaded")

    all_move_ids = set()
    for p in pokemons:
        all_move_ids.update(p.get("learnable_moves", []))

    print(f"Total move IDs: {len(all_move_ids)}")
    print("Fetching moves (attack only)...")

    move_list = []
    for i, mid in enumerate(sorted(all_move_ids)):
        if (i+1) % 100 == 0:
            print(f"  [{i+1}/{len(all_move_ids)}]")
        try:
            m = fetch_move(mid)
            if m["category"] in ("physical", "special"):
                move_list.append(m)
            time.sleep(0.12)
        except Exception as e:
            if (i+1) % 50 == 0:
                print(f"  skip {mid}: {e}")
            time.sleep(0.5)

    moves_path = os.path.join(script_dir, "moves_db.json")
    with open(moves_path, "w", encoding="utf-8") as f:
        json.dump(move_list, f, ensure_ascii=False, indent=1)
    print(f"Saved {len(move_list)} attack moves")

    # assets 폴더에 복사
    assets = os.path.join(script_dir, "app", "src", "main", "assets")
    if os.path.isdir(assets):
        shutil.copy(pokemon_path, assets)
        shutil.copy(moves_path, assets)
        print(f"Copied to {assets}")
    else:
        os.makedirs(assets, exist_ok=True)
        shutil.copy(pokemon_path, assets)
        shutil.copy(moves_path, assets)
        print(f"Created and copied to {assets}")

    print("DONE!")

if __name__ == "__main__":
    main()
