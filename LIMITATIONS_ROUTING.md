# 🚧 Limitations Actuelles du Routing Anti-Caméras

## 1️⃣ Limitations API (OpenRouteService)

### Quota Gratuit
- ✅ **2000 requêtes/jour** (généreux)
- ⚠️ Actuellement : **3-4 requêtes par calcul** → ~500 itinéraires/jour max

### Alternatives Routes
- ❌ ORS ne trouve pas toujours d'alternatives (surtout sur trajets courts)
- ❌ `alternative_routes` dépend de la topologie du réseau routier
- ❌ Si pas d'alternative évidente → retourne juste la route directe

### avoid_polygons
- ❌ **Ne marche PAS** (erreur 500 "Unable to compute route")
- ❌ Format GeoJSON complexe
- ❌ Limite probable sur le nombre de polygones

## 2️⃣ Limitations Actuelles du Code

### Détours Fallback
- ⚠️ **4 détours fixes** (N, S, E, O) de 300m
- ⚠️ **Pas intelligent** : ne regarde pas où sont les caméras
- ⚠️ Peut passer par plus de caméras que le direct

### Pas d'Optimisation
- ❌ Ne calcule pas le meilleur waypoint pour éviter les caméras
- ❌ Ne teste pas plusieurs distances de détour
- ❌ Ne considère pas la densité de caméras par zone

## 3️⃣ Améliorations Possibles

### 🔥 Option 1 : Détours Intelligents
```
1. Identifier les "zones chaudes" (clusters de caméras)
2. Générer waypoints qui contournent ces zones
3. Tester 8-12 détours stratégiques au lieu de 4 fixes
4. Varier la distance (200m, 400m, 600m)
```

### 🔥 Option 2 : Algorithme Personnalisé
```
1. Calculer plusieurs trajets avec waypoints multiples
2. Diviser le trajet en segments
3. Pour chaque segment, trouver le chemin évitant caméras
4. Recombiner en route complète
```

### 🔥 Option 3 : Algorithme A* Modifié
```
1. Implémenter A* avec coût modifié
2. Coût = distance + (nb_caméras × pénalité)
3. Trouver le chemin optimal évitant caméras
4. Plus complexe mais contrôle total
```

### 🔥 Option 4 : Heatmap + Détours Adaptatifs
```
1. Créer heatmap de densité de caméras
2. Identifier les "couloirs sans caméras"
3. Placer waypoints dans ces couloirs
4. Calculer routes via ces points
```

## 4️⃣ Ce Qui Marcherait le Mieux

**Option 1 (Détours Intelligents)** est le meilleur compromis :
- ✅ Utilise toujours ORS (pas de réinvention)
- ✅ Facile à implémenter
- ✅ Calcule waypoints basés sur position des caméras
- ✅ Teste plusieurs configurations

**Implémentation** :
```kotlin
// Au lieu de détours fixes :
val detourOffsets = listOf(N, S, E, O)

// Détours intelligents :
1. Trouver la zone avec le plus de caméras
2. Créer waypoint à l'opposé de cette zone
3. Générer 8 waypoints différents :
   - 4 à 200m
   - 4 à 500m
4. Tester chaque waypoint
5. Garder les 3-5 meilleures routes
```

## 5️⃣ Coût en Requêtes

- **v3.2 actuel** : 3-4 requêtes/calcul → ~500 calculs/jour
- **Détours intelligents** : 10-12 requêtes/calcul → ~150-200 calculs/jour
- **Toujours OK** pour usage personnel !

---

**Je peux implémenter les détours intelligents si vous voulez ! 🚀**
