# 🐛 Résolution "Failed to Create Node"

## Erreur Rencontrée

```
Failed to create node
```

## 🔍 Causes Possibles

### 1. Problème d'Authentification OAuth
- Le token n'est pas valide
- Permissions insuffisantes
- Session expirée

### 2. Problème de Changeset
- Le changeset n'a pas été créé correctement
- Le changeset est fermé avant la création du node
- Timeout

### 3. Problème de Tags
- Tags invalides ou mal formés
- XML mal encodé
- Tags manquants obligatoires

### 4. Problème de Coordonnées
- Latitude hors limites (-90 à 90)
- Longitude hors limites (-180 à 180)
- Coordonnées à 0,0

## 🔧 Solutions

### Solution 1 : Ajouter des Logs Détaillés

Je vais ajouter des logs pour voir exactement où ça bloque :

```kotlin
// Dans OSMRepository.kt
Log.d("OSM", "Creating changeset...")
val changesetId = createChangeset()
Log.d("OSM", "Changeset created: $changesetId")

Log.d("OSM", "Creating node...")
val nodeId = createNode(changesetId, cameraData)
Log.d("OSM", "Node created: $nodeId")
```

### Solution 2 : Vérifier les Permissions OAuth

Votre app OSM doit avoir :
- ✅ `read_prefs` - Lire les préférences
- ✅ `write_api` - Modifier la carte

**Vérifier** : https://www.openstreetmap.org/oauth2/applications

### Solution 3 : Tester sur le Serveur de Dev OSM

Au lieu du serveur production, tester sur :
```
https://master.apis.dev.openstreetmap.org/
```

Cela évite de polluer OSM pendant les tests.

## 🧪 Test Manuel

Pour tester si les credentials OAuth fonctionnent :

```bash
# Obtenir un token (manuellement)
curl -X POST https://www.openstreetmap.org/oauth2/token \
  -d "grant_type=client_credentials" \
  -d "client_id=VOTRE_CLIENT_ID" \
  -d "client_secret=VOTRE_CLIENT_SECRET"

# Tester l'API
curl -H "Authorization: Bearer TOKEN" \
  https://api.openstreetmap.org/api/0.6/user/details.json
```

## 💡 Prochaine Version

Je vais ajouter dans la prochaine version :

1. **Logs détaillés** pour voir où ça bloque
2. **Messages d'erreur précis** au lieu de "failed to create node"
3. **Retry automatique** en cas d'échec
4. **Validation avant envoi** plus stricte
5. **Mode offline** pour sauvegarder localement et uploader plus tard

## 🔍 Debugging

Regardez les logs Android pour plus d'infos :

```bash
adb logcat | grep -E "(OSM|BalanceTaCam|oauth)"
```

Ou dans Android Studio : Logcat filtered by "OSM"

---

Voulez-vous que j'ajoute des logs détaillés et recompile ?

