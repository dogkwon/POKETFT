import json
import os

ASSETS_DIR = r"c:\Users\dokoo\Desktop\POKETFT\app\src\main\assets"
ROOT_DIR = r"c:\Users\dokoo\Desktop\POKETFT"

def update_db(dir_path):
    poke_path = os.path.join(dir_path, "pokemon_db.json")
    if not os.path.exists(poke_path):
        return

    with open(poke_path, "r", encoding="utf-8") as f:
        pokemons = json.load(f)
        
    for p in pokemons:
        name = p.get("name_ko", "")
        if "이어롭" in name:
            if 252 not in p["learnable_moves"]:
                p["learnable_moves"].append(252)
        if "마폭시" in name:
            if 252 not in p["learnable_moves"]:
                p["learnable_moves"].append(252)
            if 605 not in p["learnable_moves"]:
                p["learnable_moves"].append(605)
                
    with open(poke_path, "w", encoding="utf-8") as f:
        json.dump(pokemons, f, ensure_ascii=False, indent=2)

update_db(ASSETS_DIR)
update_db(ROOT_DIR)
print("Updated successfully.")
