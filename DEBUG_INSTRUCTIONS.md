# 🐛 Instructions de Débogage BalanceTaCam

## 📱 Étape 1 : Installer l'APK

```bash
adb install -r /home/maun/BalanceTaCam-v2.1.0-SEARCH.apk
```

## 🧪 Étape 2 : Tester la Recherche d'Adresse

1. Ouvrir BalanceTaCam
2. Cliquer sur **🗺️** (Itinéraire) en haut à droite
3. Dans le champ "Rechercher adresse", taper :
   ```
   Paris
   ```
4. Cliquer sur **🔍**

## 📊 Étape 3 : Voir les Logs

Dans un autre terminal :

```bash
adb logcat | grep BalanceTaCam
```

### Ce Que Vous Devriez Voir :

**✅ Succès** :
```
D/BalanceTaCam: Searching address: Paris
D/BalanceTaCam: Response code: 200
D/BalanceTaCam: Found 5 results
D/BalanceTaCam: First result: Paris, Île-de-France, France (48.8566, 2.3522)
```

**❌ Échec** :
```
D/BalanceTaCam: Searching address: Paris
E/BalanceTaCam: Search failed: 403 - Forbidden
```

→ Si **403**, c'est le **User-Agent manquant** !

## 🧪 Étape 4 : Tester la Position GPS

1. Sur l'écran Itinéraire
2. Regarder le bouton **📍** (Ma position)
   - **Bleu** = GPS disponible
   - **Gris** = GPS indisponible

Dans les logs :

```bash
adb logcat | grep "User location"
```

**✅ Succès** :
```
D/BalanceTaCam: User location in routing: GeoPoint{lat=48.8566, lon=2.3522}
```

**❌ Échec** :
```
D/BalanceTaCam: User location in routing: null
W/BalanceTaCam: No user location available
```

## 🔧 Corrections Nécessaires

### Si Recherche Échoue (403)
→ **User-Agent manquant** dans les requêtes HTTP

### Si GPS Indisponible
→ **Position pas passée** de MapScreen à RoutingScreen

## 📝 Donnez-Moi les Logs !

Après avoir testé, envoyez-moi :

```bash
adb logcat | grep BalanceTaCam > /home/maun/balancetacam_logs.txt
```

Et dites-moi ce que vous voyez !
