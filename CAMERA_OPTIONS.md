# 📷 Liste Complète des Options de Caméras

## 🏷️ Tags Obligatoires (Automatiques)

Ces tags sont **toujours ajoutés** automatiquement :

| Tag | Valeur | Description |
|-----|--------|-------------|
| `man_made` | `surveillance` | Indique un équipement de surveillance |
| `surveillance:type` | `camera` | Spécifie que c'est une caméra |

---

## 🎨 Options Disponibles dans l'App

### 1. Type de Caméra (`camera:type`)

| Valeur | Français | Description |
|--------|----------|-------------|
| `fixed` | **Fixe** | Caméra fixe pointant dans une direction |
| `dome` | **Dôme** | Caméra dôme avec vision 360° |
| `ptz` | **PTZ (Motorisée)** | Pan-Tilt-Zoom, caméra orientable à distance |
| `panoramic` | **Panoramique** | Caméra fisheye ou panoramique 180-360° |

**Exemple d'usage** :
- Caméra de rue fixe → `fixed`
- Caméra de plafond ronde → `dome`
- Caméra de surveillance automatique → `ptz`
- Caméra grand angle → `panoramic`

---

### 2. Type de Support (`camera:mount`)

| Valeur | Français | Description |
|--------|----------|-------------|
| `pole` | **Poteau** | Montée sur un poteau dédié |
| `wall` | **Mur** | Fixée sur un mur ou façade |
| `ceiling` | **Plafond** | Montée au plafond (intérieur) |
| `street_lamp` | **Lampadaire** | Sur un lampadaire public |

**Autres valeurs possibles** (pas dans l'app par défaut) :
- `building` - Sur la structure du bâtiment
- `bridge` - Sur un pont
- `gantry` - Sur un portique
- `ground` - Au sol

---

### 3. Direction (`camera:direction`)

**Valeur** : Nombre de 0 à 360 (degrés)

| Degrés | Direction |
|--------|-----------|
| `0` | ⬆️ Nord |
| `45` | ↗️ Nord-Est |
| `90` | ➡️ Est |
| `135` | ↘️ Sud-Est |
| `180` | ⬇️ Sud |
| `225` | ↙️ Sud-Ouest |
| `270` | ⬅️ Ouest |
| `315` | ↖️ Nord-Ouest |

**Note** : Utilisez une boussole ou Google Maps pour déterminer la direction précise

**Quand l'utiliser** :
- ✅ Caméras `fixed` (fixes)
- ❌ Caméras `dome` ou `panoramic` (inutile car vision 360°)

---

### 4. Type de Surveillance (`surveillance`)

| Valeur | Français | Description |
|--------|----------|-------------|
| `public` | **Publique** | Surveillance d'espace public accessible |
| `outdoor` | **Extérieure** | Surveillance extérieure |
| `indoor` | **Intérieure** | Surveillance intérieure |

**Exemples** :
- Rue, place publique → `public`
- Parking extérieur → `outdoor`
- Hall d'immeuble → `indoor`

**Autres valeurs possibles** :
- `webcam` - Caméra web publique
- `guard` - Surveillance avec agent de sécurité
- `ALPR` - Lecture automatique de plaques

---

### 5. Opérateur (`operator`)

**Texte libre** pour identifier qui gère la caméra.

**Exemples** :
```
Ville de Paris
Police Municipale
RATP
SNCF
Carrefour
Mairie de Lyon
Préfecture de Police
```

**Conseils** :
- Utilisez le nom officiel
- Soyez précis (pas juste "Police" mais "Police Municipale de [ville]")
- Pour les commerces, utilisez le nom de l'enseigne

---

### 6. Type d'Opérateur (`operator:type`)

| Valeur | Français | Description |
|--------|----------|-------------|
| `public` | **Public** | Organisme public (ville, police, état) |
| `private` | **Privé** | Entreprise privée ou particulier |
| `commercial` | **Commercial** | Magasin, centre commercial |

**Exemples** :
- Police, Mairie → `public`
- Entreprise de sécurité → `private`
- Supermarché, banque → `commercial`

---

### 7. Zone de Surveillance (`surveillance:zone`)

| Valeur | Français | Description |
|--------|----------|-------------|
| `town` | **Ville** | Surveillance générale d'une zone urbaine |
| `parking` | **Parking** | Parking ou stationnement |
| `traffic` | **Trafic** | Surveillance du trafic routier |
| `building` | **Bâtiment** | Entrée ou intérieur de bâtiment |

**Autres valeurs possibles** :
- `public_transport` - Transport en commun (gare, métro)
- `shop` - Magasin
- `bank` - Banque/DAB
- `atm` - Distributeur automatique
- `entrance` - Entrée spécifique
- `subway` - Métro
- `station` - Gare
- `platform` - Quai
- `industrial` - Zone industrielle
- `residential` - Zone résidentielle

---

### 8. Description (`description`)

**Texte libre** pour informations supplémentaires.

**Exemples** :
```
"Surveille le passage piéton"
"Caméra de feu rouge"
"Entrée principale du bâtiment"
"Surveillance anti-vandalisme"
"Caméra ANPR (lecture de plaques)"
```

---

### 9. Niveau/Étage (`level`)

**Valeur** : Numéro d'étage

| Valeur | Signification |
|--------|---------------|
| `0` | Rez-de-chaussée |
| `1` | 1er étage |
| `2` | 2ème étage |
| `-1` | Sous-sol 1 |
| `-2` | Sous-sol 2 |

**Quand l'utiliser** :
- Caméras dans des bâtiments multi-niveaux
- Parkings souterrains
- Centres commerciaux

---

### 10. Hauteur (`height`)

**Format** : Nombre + unité (généralement mètres)

**Exemples** :
```
3 m
5 m
8 m
10 m
3.5 m
```

**Estimation** :
- Caméra sur lampadaire : ~4-6 m
- Caméra sur poteau : ~3-5 m
- Caméra sur mur : ~2-4 m
- Caméra sur immeuble : ~8-15 m

---

## 🎯 Combinaisons Recommandées

### Caméra de Rue Publique Classique
```yaml
camera:type: dome
camera:mount: pole
surveillance: public
operator: Ville de Paris
operator:type: public
surveillance:zone: town
height: 5 m
```

### Caméra de Parking
```yaml
camera:type: dome
camera:mount: pole
surveillance: outdoor
surveillance:zone: parking
operator: [Nom du parking]
operator:type: commercial
height: 4 m
```

### Caméra de Feu Rouge
```yaml
camera:type: fixed
camera:mount: pole
camera:direction: 180
surveillance: outdoor
surveillance:zone: traffic
operator: Préfecture
operator:type: public
description: Caméra de feu rouge
height: 6 m
```

### Caméra de Magasin
```yaml
camera:type: dome
camera:mount: ceiling
surveillance: indoor
surveillance:zone: shop
operator: Carrefour
operator:type: commercial
level: 0
```

### Caméra de Banque/DAB
```yaml
camera:type: fixed
camera:mount: wall
camera:direction: 90
surveillance: outdoor
surveillance:zone: atm
operator: [Nom de la banque]
operator:type: commercial
description: Surveillance DAB
```

---

## 📋 Tags Additionnels (Avancés)

Ces tags ne sont **pas dans l'app** mais peuvent être ajoutés manuellement sur OSM :

| Tag | Description | Exemples |
|-----|-------------|----------|
| `contact` | Contact de l'opérateur | `+33123456789` |
| `ref` | Numéro de référence | `CAM-001` |
| `start_date` | Date d'installation | `2020` |
| `maxspeed` | Pour caméras de vitesse | `50` |
| `surveillance:signed` | Signalisation présente | `yes`/`no` |
| `surveillance:covered` | Zone couverte | `yes`/`no` |
| `angle` | Angle de vue | `90` (degrés) |

---

## 🔍 Vérification des Données

### Avant d'Ajouter une Caméra

✅ **Vérifiez** :
- La caméra existe vraiment (pas une ancienne caméra retirée)
- Vous êtes sur place (pas depuis photos/satellite)
- La position GPS est précise
- Les informations sont exactes

❌ **N'ajoutez PAS** :
- Caméras fictives ou planifiées
- Caméras sur propriété privée non vérifiées
- Caméras militaires ou ultra-sensibles
- Duplicatas (vérifiez d'abord qu'elle n'existe pas)

---

## 📱 Dans l'App BalanceTaCam

### Mode Rapide
Ajoute seulement :
- Position GPS
- `man_made=surveillance`
- `surveillance:type=camera`

### Mode Détaillé
Permet d'ajouter :
- ✅ Type de caméra (4 choix)
- ✅ Type de support (4 choix)
- ✅ Direction (0-360°)
- ✅ Type de surveillance (3 choix)
- ✅ Opérateur (texte libre)
- ✅ Type d'opérateur (3 choix)
- ✅ Zone (4 choix principaux)
- ✅ Description (texte libre)
- ✅ Niveau/Étage (nombre)
- ✅ Hauteur (texte avec unité)

---

## 💡 Conseils de Contribution

### Qualité > Quantité
- Mieux vaut 10 caméras bien documentées que 100 avec juste la position
- Prenez le temps de remplir les détails en mode détaillé

### Soyez Précis
- Direction : Utilisez une boussole
- Hauteur : Estimez au mieux
- Opérateur : Cherchez les panneaux ou plaques

### Vérifiez
- Utilisez l'app pour voir si la caméra n'existe pas déjà
- Zoomez bien avant d'ajouter
- Vérifiez vos contributions sur OSM après

---

## 🌍 Contribution Responsable

### ✅ Bonnes Pratiques
- Mapper uniquement les caméras publiquement visibles
- Respecter les lois locales
- Être objectif (pas de jugement dans description)
- Vérifier in situ

### ⚠️ À Éviter
- Caméras militaires
- Installations sécurisées sensibles
- Caméras privées non visibles
- Zones interdites

---

**Toutes ces options sont disponibles dans BalanceTaCam !** 📱🗺️

