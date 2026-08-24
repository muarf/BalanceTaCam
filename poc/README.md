# PoC Routage offline — Phase 0 (validée)

Preuve de concept : itinéraire piéton anti-caméras **sans aucun réseau**, moteur
GraphHopper 5.3 embarqué dans l'app, graphe généré au préalable depuis OSM.

## Résultats (redroid Android 12 ARM, extrait Île-de-France 116 Mo)

| Mesure | Valeur |
|---|---|
| Chargement du graphe | ~500 ms (MMAP) |
| Route directe République → Montparnasse | 4813 m, traverse la zone caméra |
| Meilleur détour (6 candidats) | 4993 m (+179 m), zone évitée |
| Calcul total | < 1 s |

## Méthode d'évitement

Les Custom Models de GraphHopper (zones bloquées `in_area`) ne fonctionnent PAS
sur Android : le compilateur d'expressions utilise javax.tools/javac, absent
d'ART. L'évitement est donc calculé côté app par **points de passage** :

1. Route directe A→B
2. Détection des clusters de caméras proches du tracé (rayon configurable)
3. Génération de candidats perpendiculaires ± (offsets croissants)
4. Re-routage via chaque candidat, sélection du plus court sans caméra

Même mécanique que le fallback detour existant côté ORS (`RoutingRepository`).

## Générer le graphe (vps-oracle)

```bash
wget https://download.geofabrik.de/europe/france/ile-de-france-latest.osm.pbf
wget https://repo1.maven.org/maven2/com/graphhopper/graphhopper-web/5.3/graphhopper-web-5.3.jar
java -Xmx5g -jar graphhopper-web-5.3.jar import config.yml   # ~1 min
# graph-cache/ produit -> à servir en HTTP pour téléchargement par région
```

`config.yml` : profil `foot` standard (`weighting: fastest`, sans CH/LM).

## Tester dans l'app

```bash
adb install app-debug.apk
adb push graph-cache /data/local/tmp/gh-poc
adb shell "run-as com.osmcamera.mapper mkdir -p files/gh-poc/graph-cache"
# copier les fichiers puis :
adb shell am start -n com.osmcamera.mapper/.offline.PocOfflineActivity
adb logcat -s PocOffline
```
