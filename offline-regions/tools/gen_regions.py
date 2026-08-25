#!/usr/bin/env python3
"""
Genere offline-regions/regions.yml depuis l'index Geofabrik mondial.

Politique :
- Tous les pays du monde au niveau pays
- Decoupage en sous-regions quand le pbf est trop gros pour un job CI /
  un telechargement mobile unique (seuil PBF_SPLIT_BYTES)
- La France est toujours detaillee en ses regions historiques Geofabrik,
  avec alias idf -> ile-de-france (compatibilite v4.0)
- USA : les 5 macro-regions us-* (midwest/northeast/pacific/south/west)
- Antarctique exclu

Usage : python3 gen_regions.py [index-v1.json] > ../regions.yml
"""
import json
import sys
import urllib.request

INDEX_URL = "https://download.geofabrik.de/index-v1.json"
PBF_SPLIT_BYTES = 1_200_000_000

CONTINENTS = {
    "africa": "AF",
    "asia": "AS",
    "central-america": "CA",
    "europe": "EU",
    "north-america": "NA",
    "south-america": "SA",
    "australia-oceania": "OC",
    # la Russie est un noeud racine a part dans l'index Geofabrik
    "russia": "AS",
}

EXCLUDE = {"antarctica"}

# Noeuds "regroupement" artificiels de Geofabrik : territoires deja couverts
# par les pays individuels, on les ignore purement et simplement.
GROUPING_SKIP = {
    "dach", "alps", "britain-and-ireland", "great-britain", "sea",
    "us", "us-midwest", "us-northeast", "us-pacific", "us-south", "us-west",
}

# Pays toujours remplaces par leurs enfants Geofabrik
ALWAYS_SPLIT = {"france", "russia", "canada", "brazil", "china",
                "india", "indonesia", "australia"}

NAME_OVERRIDES = {
    "ile-de-france": "Île-de-France",
    "rhone-alpes": "Rhône-Alpes",
    "provence-alpes-cote-d-azur": "Provence-Alpes-Côte d'Azur",
    "pays-de-la-loire": "Pays de la Loire",
    "nord-pas-de-calais": "Nord-Pas-de-Calais",
    "franche-comte": "Franche-Comté",
    "midi-pyrenees": "Midi-Pyrénées",
    "basse-normandie": "Basse-Normandie",
    "haute-normandie": "Haute-Normandie",
}


def load_index(path=None):
    if path:
        return json.load(open(path))
    req = urllib.request.Request(INDEX_URL, headers={"User-Agent": "balancetacam-gen"})
    return json.load(urllib.request.urlopen(req, timeout=60))


def geopath(feature):
    """Chemin geofabrik depuis l'URL pbf : europe/france/alsace"""
    url = feature["urls"]["pbf"]
    path = url.split("download.geofabrik.de/")[1]
    return path.replace("-latest.osm.pbf", "")


def head_size(url):
    req = urllib.request.Request(url, method="HEAD",
                                 headers={"User-Agent": "balancetcam-gen"})
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return int(r.headers.get("Content-Length") or 0)
    except Exception:
        return 0


def slug(geo_path):
    s = geo_path.replace("/", "-")
    if s == "europe-france-ile-de-france":
        return "idf"
    return s


def main():
    index_path = sys.argv[1] if len(sys.argv) > 1 else None
    idx = load_index(index_path)
    feats = [f["properties"] for f in idx["features"]]
    children = {}
    for p in feats:
        parent = p.get("parent")
        if parent:
            children.setdefault(parent, []).append(p)

    countries = []
    for p in feats:
        parent = p.get("parent")
        if parent is None and p["id"] != "russia":
            continue  # continents
        if parent in CONTINENTS or p["id"] == "russia":
            countries.append(p)

    rows = []
    n_split, n_kept = 0, 0

    def emit(feature):
        gp = geopath(feature)
        leaf = gp.split("/")[-1]
        name = NAME_OVERRIDES.get(leaf, feature["name"])
        code = CONTINENTS.get(gp.split("/")[0], "XX")
        rows.append({"id": slug(gp), "name": name, "country": code, "geopath": gp})

    # usa : les etats individuels (ids "us/<state>")
    us_states = [k for k in feats if k["id"].startswith("us/")]

    for c in sorted(countries, key=lambda f: f["id"]):
        cid = c["id"]
        if cid in EXCLUDE or cid in GROUPING_SKIP:
            continue
        kids = [k for k in children.get(cid, []) if k["id"] not in EXCLUDE]
        split = cid in ALWAYS_SPLIT and kids
        if not split:
            size = head_size(c["urls"]["pbf"])
            if size and size < PBF_SPLIT_BYTES:
                emit(c)
                n_kept += 1
                continue
            if size == 0:
                print(f"WARN: taille inconnue pour {cid}, pays garde", file=sys.stderr)
                emit(c)
                n_kept += 1
                continue
            if not kids:
                print(f"WARN: {cid} gros ({size} o) mais sans sous-regions, garde",
                      file=sys.stderr)
                emit(c)
                n_kept += 1
                continue
        if cid == "us":
            for k in sorted(us_states, key=lambda f: f["id"]):
                emit(k)
                n_split += 1
        else:
            for k in sorted(kids, key=lambda f: f["id"]):
                emit(k)
                n_split += 1

    seen = set()
    uniq = []
    for r in rows:
        if r["id"] in seen:
            print(f"WARN: doublon ignore {r['id']}", file=sys.stderr)
            continue
        seen.add(r["id"])
        uniq.append(r)
    uniq.sort(key=lambda r: (r["country"], r["geopath"]))

    print("# Genere par offline-regions/tools/gen_regions.py - NE PAS EDITER A LA MAIN")
    print("regions:")
    for r in uniq:
        nm = r["name"].replace('"', "'")
        print(f'  - {{ id: {r["id"]}, name: "{nm}", country: "{r["country"]}",'
              f' geopath: "{r["geopath"]}" }}')

    print(f"# {len(uniq)} regions ({n_split} issues de decoupage, "
          f"{n_kept} pays entiers)", file=sys.stderr)


if __name__ == "__main__":
    main()
