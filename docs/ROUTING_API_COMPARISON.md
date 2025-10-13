# 🗺️ Comparaison des API de Routing pour Anti-Caméras

## Vue d'Ensemble

| API | Gratuit | Limite | Complexité | Qualité Routes | Support Android |
|-----|---------|--------|------------|----------------|-----------------|
| **GraphHopper** | ✅ | 500/jour | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ✅ Excellent |
| **OSRM** | ✅ | Illimité* | ⭐⭐ | ⭐⭐⭐⭐ | ✅ Bon |
| **Mapbox** | ⚠️ | 100k/mois | ⭐⭐ | ⭐⭐⭐⭐⭐ | ✅ Excellent |
| **Google Maps** | ❌ | Payant | ⭐ | ⭐⭐⭐⭐⭐ | ✅ Excellent |
| **OpenRouteService** | ✅ | 2000/jour | ⭐⭐ | ⭐⭐⭐⭐ | ✅ Bon |

---

## 1️⃣ GraphHopper

### 🎯 Résumé
API de routing **open source** basée sur OSM, très flexible.

### ✅ Avantages
- **Gratuit** : 500 requêtes/jour sans API key
- **Open source** : Peut être auto-hébergé
- **Flexible** : Supporte les poids personnalisés sur les routes
- **Android natif** : Bibliothèque Java/Kotlin disponible
- **Alternatives multiples** : Renvoie plusieurs routes
- **Profils variés** : Voiture, vélo, piéton, etc.

### ❌ Inconvénients
- **Limite gratuite** : 500/jour (suffisant pour usage personnel)
- **Taille** : Bibliothèque assez lourde (~5 MB)
- **Serveur public** : Peut être lent aux heures de pointe

### 💻 Utilisation

```kotlin
// Gradle
implementation("com.graphhopper:graphhopper-core:8.0")

// Code
val hopper = GraphHopper()
val request = GHRequest(startLat, startLon, endLat, endLon)
    .setProfile("car")
    .setAlgorithm("alternative_route")
    .setAlternativeRouteMaxPaths(3)

val response = hopper.route(request)
val routes = response.all // Liste de routes alternatives
```

### 🔧 Pour Éviter les Caméras

```kotlin
// Ajouter des pénalités sur les routes avec caméras
routes.forEach { route ->
    route.points.forEach { point ->
        val nearbyCameras = cameras.filter { 
            distance(point, it) < 50 // 50 mètres
        }
        route.penalty += nearbyCameras.size * 100 // Pénalité par caméra
    }
}
```

### 📊 API Gratuite
```
Endpoint: https://graphhopper.com/api/1/route
Limite: 500 requêtes/jour
API Key: Optionnelle (gratuit sans key)
```

### ⭐ Note : 9/10
**Recommandé** pour BalanceTaCam ! Open source, flexible, gratuit.

---

## 2️⃣ OSRM (OpenStreetMap Routing Machine)

### 🎯 Résumé
Moteur de routing **ultra-rapide** et gratuit d'OpenStreetMap.

### ✅ Avantages
- **100% gratuit** : Serveur public illimité
- **Très rapide** : Optimisé pour la vitesse
- **Open source** : Peut être auto-hébergé
- **Pas d'API key** : Aucune inscription requise
- **Routes alternatives** : Supporte plusieurs chemins
- **Léger** : API REST simple

### ❌ Inconvénients
- **Moins flexible** : Difficile de personnaliser les poids
- **Pas de SDK Android** : Seulement API REST
- **Serveur public** : Peut être surchargé
- **Pas de pénalités customs** : Dur d'éviter spécifiquement les caméras

### 💻 Utilisation

```kotlin
// Retrofit
interface OSRMService {
    @GET("route/v1/driving/{coordinates}")
    suspend fun getRoute(
        @Path("coordinates") coords: String, // "lon1,lat1;lon2,lat2"
        @Query("alternatives") alternatives: Int = 3,
        @Query("overview") overview: String = "full"
    ): Response<OSRMResponse>
}

// Appel
val coords = "$startLon,$startLat;$endLon,$endLat"
val response = osrmService.getRoute(coords, alternatives = 3)
```

### 🔧 Pour Éviter les Caméras

```kotlin
// Analyser les routes après réception
routes.forEach { route ->
    var cameraScore = 0
    route.geometry.coordinates.forEach { point ->
        cameraScore += camerasNear(point, radius = 50).size
    }
    route.cameraScore = cameraScore
}
// Choisir la route avec le score le plus bas
```

### 📊 API Gratuite
```
Endpoint: https://router.project-osm.org/route/v1/
Limite: Aucune (usage raisonnable)
API Key: Aucune
```

### ⭐ Note : 7/10
Bon mais moins flexible pour les modifications de poids.

---

## 3️⃣ Mapbox Directions API

### 🎯 Résumé
API commerciale très performante avec SDK Android complet.

### ✅ Avantages
- **SDK Android excellent** : Intégration facile
- **Qualité routes** : Excellent
- **Navigation turn-by-turn** : Incluse
- **Interface belle** : Composants UI fournis
- **Personnalisation** : Waypoints, évitement zones
- **Support** : Documentation parfaite

### ❌ Inconvénients
- **Payant après 100k requêtes/mois** : Gratuit au début mais...
- **API key obligatoire** : Inscription requise
- **Pas open source** : Service commercial
- **Taille SDK** : ~15 MB supplémentaires

### 💻 Utilisation

```kotlin
// Gradle
implementation("com.mapbox.navigation:android:2.17.0")

// Code
val mapboxNavigation = MapboxNavigation(...)
val routeOptions = RouteOptions.builder()
    .coordinatesList(listOf(start, end))
    .alternatives(true)
    .build()

mapboxNavigation.requestRoutes(routeOptions) { routes ->
    // Analyser et filtrer par caméras
}
```

### 📊 Tarifs
```
Gratuit: 100,000 requêtes/mois
Après: 0.50$ / 1000 requêtes
API Key: Obligatoire
```

### ⭐ Note : 8/10
Excellent mais commercial. Bon pour une app populaire.

---

## 4️⃣ Google Maps Directions API

### 🎯 Résumé
L'API de Google, très puissante mais payante.

### ✅ Avantages
- **Meilleure qualité** : Routes optimales
- **Trafic en temps réel** : Inclus
- **SDK complet** : Très facile à utiliser
- **Fiable** : Infrastructure Google

### ❌ Inconvénients
- **Payant** : Dès le début (0.50$ / 1000 requêtes)
- **API key obligatoire** : Compte Google Cloud
- **Pas open source** : Commercial
- **Contre philosophie OSM** : On utilise OSM, pas Google

### ⭐ Note : 5/10
Excellent techniquement mais **pas adapté** pour une app OSM open source.

---

## 5️⃣ OpenRouteService (ORS)

### 🎯 Résumé
Service basé sur OSM avec API gratuite généreuse.

### ✅ Avantages
- **Gratuit** : 2000 requêtes/jour
- **Basé sur OSM** : Philosophie alignée
- **API moderne** : REST API propre
- **Routes alternatives** : Supporte plusieurs chemins
- **Avoid areas** : Peut éviter des zones spécifiques !
- **Open source** : Peut être auto-hébergé

### ❌ Inconvénients
- **API key obligatoire** : Inscription requise
- **Pas de SDK Android natif** : Seulement REST
- **Serveur public** : Peut être lent

### 💻 Utilisation

```kotlin
// Retrofit
interface ORSService {
    @GET("v2/directions/driving-car")
    suspend fun getRoute(
        @Query("api_key") apiKey: String,
        @Query("start") start: String, // "lon,lat"
        @Query("end") end: String,
        @Query("alternative_routes") alternatives: Int = 3
    ): Response<ORSResponse>
}
```

### 🔧 Éviter les Caméras

ORS supporte **avoid_polygons** - on peut définir des zones à éviter !

```kotlin
// Créer des polygones autour des caméras
val avoidZones = cameras.map { camera ->
    createCirclePolygon(camera, radius = 50) // 50m autour
}

// Passer à l'API
val request = ORSRequest(
    coordinates = listOf(start, end),
    avoidPolygons = avoidZones
)
```

### 📊 API Gratuite
```
Endpoint: https://api.openrouteservice.org/
Limite: 2000 requêtes/jour
API Key: Gratuite après inscription
```

### ⭐ Note : 8/10
Très bon choix ! Gratuit, OSM-based, supporte l'évitement de zones.

---

## 📊 Tableau Comparatif Détaillé

### Fonctionnalités

| Fonctionnalité | GraphHopper | OSRM | Mapbox | Google | ORS |
|----------------|-------------|------|--------|--------|-----|
| Routes alternatives | ✅ | ✅ | ✅ | ✅ | ✅ |
| Poids personnalisés | ✅✅ | ⚠️ | ✅ | ❌ | ✅✅ |
| Évitement zones | ✅ | ❌ | ✅ | ✅ | ✅✅ |
| SDK Android | ✅ | ❌ | ✅✅ | ✅✅ | ❌ |
| Open source | ✅ | ✅ | ❌ | ❌ | ✅ |
| Gratuit | ✅ | ✅✅ | ⚠️ | ❌ | ✅ |

### Limites Gratuites

| API | Requêtes Gratuites | Après Limite |
|-----|-------------------|--------------|
| GraphHopper | 500/jour | Payant ou self-hosted |
| OSRM | Illimité* | - |
| Mapbox | 100k/mois | 0.50$/1000 |
| Google | 0 | Payant dès le début |
| ORS | 2000/jour | Payant ou self-hosted |

\* Usage raisonnable demandé

### Compatibilité BalanceTaCam

| Critère | GraphHopper | OSRM | Mapbox | Google | ORS |
|---------|-------------|------|--------|--------|-----|
| Philosophie OSM | ✅✅ | ✅✅ | ⚠️ | ❌ | ✅✅ |
| Facilité intégration | ✅✅ | ✅ | ✅✅ | ✅✅ | ✅ |
| Pour éviter caméras | ✅✅ | ⚠️ | ✅ | ✅ | ✅✅ |
| Gratuit long terme | ✅ | ✅✅ | ⚠️ | ❌ | ✅ |

---

## 🏆 RECOMMANDATIONS

### 🥇 1er Choix : **OpenRouteService (ORS)**

**Pourquoi** :
- ✅ **2000 requêtes/jour** : Largement suffisant
- ✅ **Évitement de zones natif** : Parfait pour éviter les caméras !
- ✅ **Basé sur OSM** : Philosophie alignée
- ✅ **Gratuit long terme** : Inscription simple
- ✅ **API moderne** : REST propre

**Idéal pour** : Feature anti-caméras avec évitement de zones

---

### 🥈 2ème Choix : **GraphHopper**

**Pourquoi** :
- ✅ **Open source** : Peut être auto-hébergé
- ✅ **SDK Android** : Intégration native
- ✅ **Poids personnalisés** : Flexible
- ✅ **500/jour** : Suffisant pour tests

**Idéal pour** : Si vous voulez éventuellement self-host

---

### 🥉 3ème Choix : **OSRM**

**Pourquoi** :
- ✅ **Illimité** : Pas de limite de requêtes
- ✅ **Ultra-rapide** : Optimisé vitesse
- ✅ **100% gratuit** : Aucune limite

**Mais** :
- ⚠️ Difficile de personnaliser pour éviter caméras
- ⚠️ Faudra analyser les routes après coup

**Idéal pour** : Si vous voulez du simple et rapide

---

## 💡 MA RECOMMANDATION : OpenRouteService

### Pourquoi ORS est Parfait pour BalanceTaCam

#### 1. Évitement de Zones Natif
ORS supporte `avoid_polygons` nativement :
```kotlin
// Créer des cercles autour des caméras
val avoidPolygons = cameras.map { camera ->
    createCirclePolygon(
        center = camera.position,
        radius = 50 // mètres
    )
}

// L'API évite automatiquement ces zones !
```

#### 2. Généreux et Gratuit
- 2000 requêtes/jour
- Pour 100 utilisateurs = 20 itinéraires/jour/user
- Largement suffisant !

#### 3. Basé sur OSM
- Données OSM
- Philosophie open source
- Communauté active

#### 4. Simple à Intégrer
```kotlin
// Juste Retrofit, pas de SDK lourd
interface ORSService {
    @POST("v2/directions/driving-car")
    suspend fun getRoute(@Body request: ORSRequest): ORSResponse
}
```

---

## 🎨 Implémentation Prévue

### Avec OpenRouteService

```kotlin
// 1. Récupérer caméras dans la zone
val cameras = getCamerasInBounds(startPoint, endPoint, padding = 1km)

// 2. Créer zones d'évitement (cercles de 50m autour des caméras)
val avoidZones = cameras.map { camera ->
    Polygon(
        coordinates = createCircle(
            center = [camera.lon, camera.lat],
            radius = 50 // mètres
        )
    )
}

// 3. Demander route à ORS
val routes = orsService.getRoute(
    start = [startLon, startLat],
    end = [endLon, endLat],
    avoidPolygons = avoidZones,
    alternatives = 3
)

// 4. Analyser et afficher
routes.forEach { route ->
    val remainingCameras = countCamerasOnRoute(route, cameras)
    route.cameraCount = remainingCameras
}

// Afficher la meilleure route (minimum de caméras)
```

---

## 📦 Dépendances à Ajouter

### Pour OpenRouteService

```kotlin
// app/build.gradle.kts

dependencies {
    // Déjà présent
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // Pour le calcul géométrique
    implementation("org.locationtech.jts:jts-core:1.19.0")
}
```

**Taille ajoutée** : ~2 MB (géométrie)

---

## 🎯 Fonctionnalités Possibles

### Version 1 : Route Unique
- Calculer 1 route qui évite les caméras
- Afficher sur la carte
- Montrer le nombre de caméras évitées

### Version 2 : Comparaison Routes
- Afficher 3 routes alternatives :
  - Route normale (directe)
  - Route évitant caméras
  - Route intermédiaire
- Afficher nombre de caméras sur chaque route
- L'utilisateur choisit

### Version 3 : Heatmap + Routing
- Heatmap des zones avec caméras
- Route évitant les zones rouges
- Navigation turn-by-turn

---

## 🚀 Temps d'Implémentation Estimé

### Avec OpenRouteService
- **API Service** : 30 min
- **Logique évitement** : 1h
- **Interface UI** : 1h
- **Tests** : 30 min
- **Total** : ~3 heures

### Avec GraphHopper
- **Intégration SDK** : 45 min
- **Configuration** : 30 min
- **Logique évitement** : 1h30
- **Interface UI** : 1h
- **Total** : ~4 heures

### Avec OSRM
- **API Service** : 20 min
- **Analyse post-routing** : 1h30
- **Interface UI** : 1h
- **Total** : ~3 heures

---

## 📊 Coût en Ressources

| API | APK Size | RAM | Batterie | Réseau |
|-----|----------|-----|----------|--------|
| ORS | +2 MB | Faible | Faible | 1-2 KB/route |
| GraphHopper | +5 MB | Moyen | Moyen | 2-3 KB/route |
| OSRM | +1 MB | Faible | Faible | 1-2 KB/route |
| Mapbox | +15 MB | Élevé | Élevé | 3-5 KB/route |

---

## 🎯 MON CHOIX FINAL : OpenRouteService

### Raisons :
1. ✅ **Évitement de polygones natif** - Parfait pour notre use case
2. ✅ **2000 req/jour** - Très généreux
3. ✅ **Basé OSM** - Cohérent avec BalanceTaCam
4. ✅ **Simple** - Juste REST API
5. ✅ **Gratuit** - Pas de surprise de facturation
6. ✅ **Léger** - +2 MB seulement

### Alternative : GraphHopper
Si vous voulez pouvoir **self-host** plus tard (serveur perso).

---

## ❓ Votre Choix ?

**A.** 🥇 **OpenRouteService** (ma recommandation)  
**B.** 🥈 **GraphHopper** (si vous voulez self-host)  
**C.** 🥉 **OSRM** (si vous voulez 100% gratuit sans limite)  
**D.** Autre ?

**Dites-moi et je commence l'implémentation ! 🚀**

