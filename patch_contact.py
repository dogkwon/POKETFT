import requests
import json
import os
import time

def main():
    assets_dir = r"C:\Users\dokoo\Desktop\POKETFT\app\src\main\assets"
    moves_path = os.path.join(assets_dir, "moves_db.json")

    with open(moves_path, "r", encoding="utf-8") as f:
        moves = json.load(f)

    # Fetch showdown data
    print("Fetching showdown moves...")
    showdown_data = requests.get("https://play.pokemonshowdown.com/data/moves.json").json()

    print(f"Loaded {len(moves)} moves to process.")
    for i, m in enumerate(moves):
        mid = m["id"]
        # get english name from pokeapi
        try:
            p_data = requests.get(f"https://pokeapi.co/api/v2/move/{mid}", timeout=10).json()
            en_name = p_data["name"].lower().replace("-", "").replace(" ", "")
            
            # Check showdown data
            is_contact = False
            sd_move = showdown_data.get(en_name)
            if sd_move:
                flags = sd_move.get("flags", {})
                if "contact" in flags:
                    is_contact = True
            m["is_contact"] = is_contact
        except Exception as e:
            print(f"Error on {mid}: {e}")
            m["is_contact"] = False
        
        time.sleep(0.05) # small delay to be nice to pokeapi
        
        if (i+1) % 50 == 0:
            print(f"Processed {i+1}/{len(moves)}...")

    with open(moves_path, "w", encoding="utf-8") as f:
        json.dump(moves, f, ensure_ascii=False, indent=1)
    
    print("Done! Patched moves_db.json with is_contact.")

if __name__ == "__main__":
    main()
