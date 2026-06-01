import json
import os
import requests
import time

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
POKEMON_PATH = os.path.join(SCRIPT_DIR, "pokemon_db.json")
BASE_URL = "https://pokeapi.co/api/v2"

_ability_cache = {}

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

def main():
    if not os.path.exists(POKEMON_PATH):
        print(f"Error: {POKEMON_PATH} not found!")
        return

    with open(POKEMON_PATH, "r", encoding="utf-8") as f:
        pokemons = json.load(f)

    # Build map of base pokemon species ID to their names and moves
    pokemon_map = {p["id"]: p for p in pokemons}
    
    mega_pokemons = []
    print(f"Loaded {len(pokemons)} base Pokemon. Scanning for Mega Evolutions...")

    for p in pokemons:
        # We only check original base pokemon (ID < 10000)
        if p["id"] >= 10000:
            continue
            
        pid = p["id"]
        species_url = f"{BASE_URL}/pokemon-species/{pid}"
        
        try:
            print(f"Checking species {p['name_ko']} (#{pid})...")
            species_data = requests.get(species_url, timeout=10).json()
            varieties = species_data.get("varieties", [])
            
            for var in varieties:
                var_name = var["pokemon"]["name"]
                var_url = var["pokemon"]["url"]
                
                # Check if it's a mega evolution
                is_mega = False
                suffix = ""
                if var_name.endswith("-mega"):
                    is_mega = True
                elif var_name.endswith("-mega-x"):
                    is_mega = True
                    suffix = "X"
                elif var_name.endswith("-mega-y"):
                    is_mega = True
                    suffix = "Y"
                    
                if is_mega:
                    mega_id = int(var_url.rstrip("/").split("/")[-1])
                    print(f"  Found Mega variety: {var_name} (ID: {mega_id})")
                    
                    # Fetch mega details
                    mega_data = requests.get(var_url, timeout=10).json()
                    
                    # Extract stats
                    stat_map = {"hp": 0, "attack": 1, "defense": 2, "special-attack": 3, "special-defense": 4, "speed": 5}
                    stats = [0] * 6
                    for s in mega_data["stats"]:
                        idx = stat_map.get(s["stat"]["name"])
                        if idx is not None:
                            stats[idx] = s["base_stat"]
                            
                    # Extract types
                    types = [t["type"]["name"] for t in mega_data["types"]]
                    
                    # Extract abilities
                    abilities = []
                    for a in mega_data["abilities"]:
                        ab_name_ko = get_ability_ko(a["ability"]["url"])
                        abilities.append({
                            "name_ko": ab_name_ko,
                            "name_en": a["ability"]["name"],
                            "is_hidden": a["is_hidden"]
                        })
                        
                    # Mega Pokemon's Korean Name
                    mega_name_ko = f"메가{p['name_ko']}{suffix}"
                    
                    # Construct Mega Pokemon object
                    mega_obj = {
                        "id": mega_id,
                        "dex_no": p["dex_no"],
                        "name_ko": mega_name_ko,
                        "stats": stats,
                        "types": types,
                        "abilities": abilities,
                        "learnable_moves": p["learnable_moves"] # Mega learns same moves as base form
                    }
                    mega_pokemons.append(mega_obj)
                    print(f"  -> Added {mega_name_ko} with stats: {stats}")
                    time.sleep(0.1)
                    
            time.sleep(0.1)
        except Exception as e:
            print(f"Error checking #{pid}: {e}")
            
    if mega_pokemons:
        print(f"\nSuccessfully fetched {len(mega_pokemons)} Mega Evolutions!")
        
        # Merge Megas into existing pokemon database
        # Avoid duplicate IDs
        existing_ids = {p["id"] for p in pokemons}
        new_additions = []
        for mega in mega_pokemons:
            if mega["id"] not in existing_ids:
                new_additions.append(mega)
                
        pokemons.extend(new_additions)
        
        # Sort by ID
        pokemons.sort(key=lambda x: x["id"])
        
        # Save back
        with open(POKEMON_PATH, "w", encoding="utf-8") as f:
            json.dump(pokemons, f, ensure_ascii=False, indent=1)
            
        print(f"Updated {POKEMON_PATH} with Mega Evolutions. Total Pokemon in DB: {len(pokemons)}")
        
        # Copy to assets if exists
        assets_dir = os.path.join(SCRIPT_DIR, "app", "src", "main", "assets")
        if os.path.isdir(assets_dir):
            import shutil
            shutil.copy(POKEMON_PATH, os.path.join(assets_dir, "pokemon_db.json"))
            print("Copied to app/src/main/assets/pokemon_db.json")
    else:
        print("No new Mega Evolutions found/added.")

if __name__ == "__main__":
    main()
