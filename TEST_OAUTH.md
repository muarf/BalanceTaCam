# 🔍 Test et Débogage OAuth

## ⚠️ Si OAuth ne fonctionne toujours pas

L'erreur HTML que vous voyez peut venir de plusieurs causes :

### Cause 1 : Les clés sont pour OAuth 2.0 au lieu de OAuth 1.0a

**OSM a DEUX systèmes OAuth différents** :
- OAuth 2.0 (nouveau, recommandé par OSM)
- OAuth 1.0a (ancien, ce que l'app utilise)

### ✅ SOLUTION : Vérifier le type d'application OAuth

1. Allez sur : https://www.openstreetmap.org/oauth2/applications
2. Si vous voyez votre app là, c'est OAuth 2.0 ❌
3. **Vous devez créer une app OAuth 1.0a** :
   - Allez sur : https://www.openstreetmap.org/user/YOUR_USERNAME/oauth_clients
   - Ou directement : https://www.openstreetmap.org/oauth_clients/new

### Différences Importantes

**OAuth 1.0a** (ce qu'on utilise) :
```
URL : /user/YOUR_USERNAME/oauth_clients
Clés : Consumer Key + Consumer Secret
```

**OAuth 2.0** (NE PAS utiliser) :
```
URL : /oauth2/applications  
Clés : Client ID + Client Secret
```

## 🔧 Créer OAuth 1.0a (Correct)

### Étape par Étape :

1. **Connectez-vous à OSM** : https://www.openstreetmap.org

2. **Allez directement ici** : 
   ```
   https://www.openstreetmap.org/oauth_clients/new
   ```

3. **Remplissez** :
   ```
   Name: BalanceTaCam
   Main Application URL: https://github.com/muarf/BalanceTaCam
   Callback URL: osmcamera://oauth
   Support URL: (laisser vide ou mettre le repo GitHub)
   ```

4. **Permissions** - Cochez UNIQUEMENT :
   ```
   ✅ read their user preferences
   ✅ modify the map
   ```

5. **Cliquez** "Register"

6. **Page de confirmation** - vous verrez :
   ```
   Consumer Key: une_longue_clé_ici
   Consumer Secret: un_long_secret_ici
   ```

### ⚠️ IMPORTANT

Les clés que vous m'avez données ressemblent à des clés OAuth 2.0 (format base64).

**OAuth 1.0a** devrait ressembler plutôt à :
```
Consumer Key: abc123XYZ (alphanumerique simple)
Consumer Secret: def456UVW (alphanumerique simple)
```

## 🧪 Test Local

Pour tester si OAuth fonctionne, on peut créer un petit test :

```bash
cd /home/maun/osm-android
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
adb logcat | grep "OAuth"
```

Puis dans l'app, cliquez "Se connecter" et regardez les logs.

## 🔄 Alternative : Simplifier OAuth

Si OAuth 1.0a pose trop de problèmes, on peut :

### Option A : Utiliser OAuth 2.0 à la place
Changer toute l'implémentation pour OAuth 2.0 (plus moderne)

### Option B : Mode offline
Permettre d'ajouter des caméras localement, à uploader plus tard

### Option C : Tester avec le serveur dev OSM
Utiliser le serveur de développement OSM pour les tests

## 📝 Message d'Erreur Actuel

L'erreur `<!DOCTYPE html>` signifie qu'au lieu de recevoir un token OAuth, on reçoit une page HTML.

Causes possibles :
1. ❌ Mauvais type d'OAuth (2.0 au lieu de 1.0a)
2. ❌ Callback URL incorrecte
3. ❌ Permissions manquantes
4. ❌ Clés invalides

## ✅ Checklist

Vérifiez que :
- [ ] Application créée sur `/oauth_clients/new` (pas `/oauth2/applications`)
- [ ] Callback URL = exactement `osmcamera://oauth`
- [ ] Permission "modify the map" cochée
- [ ] Consumer Key et Secret copiés correctement

## 🆘 Si Ça Ne Fonctionne Toujours Pas

Dites-moi et je peux :
1. Implémenter OAuth 2.0 à la place
2. Ajouter des logs détaillés pour voir exactement où ça bloque
3. Créer un mode "test" sans OAuth pour mapper localement

