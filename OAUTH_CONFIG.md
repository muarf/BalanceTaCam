# ⚠️ CONFIGURATION OAUTH OBLIGATOIRE

## L'erreur que vous voyez

```
Response body is incorrect. Can't extract token and secret from this: <!DOCTYPE html>
```

**Signifie** : Les clés OAuth ne sont pas configurées correctement.

## 🔧 Solution : Configurer OAuth en 5 minutes

### Étape 1 : Créer une application OAuth sur OSM

1. **Allez sur** : https://www.openstreetmap.org
2. **Connectez-vous** (créez un compte si besoin)
3. **Allez dans** : https://www.openstreetmap.org/user/YOUR_USERNAME/oauth_clients
4. **Cliquez** : "Register your application"

### Étape 2 : Remplir le formulaire

```
Name: BalanceTaCam
Main Application URL: https://github.com/muarf/BalanceTaCam
Callback URL: osmcamera://oauth
```

**Permissions** : Cochez ✅ **"Modify the map"**

**Cliquez** : "Register"

### Étape 3 : Copier vos clés

Vous verrez :
```
Consumer Key: abc123def456789...
Consumer Secret: xyz789ghi012345...
```

**COPIEZ CES DEUX CLÉS !**

### Étape 4 : Mettre les clés dans le code

**Ouvrir** : `app/src/main/java/com/osmcamera/mapper/data/auth/OAuthService.kt`

**Ligne 19-20**, remplacer :
```kotlin
private const val CONSUMER_KEY = "CONFIGURE_ME"
private const val CONSUMER_SECRET = "CONFIGURE_ME"
```

**Par vos vraies clés** :
```kotlin
private const val CONSUMER_KEY = "abc123def456789"
private const val CONSUMER_SECRET = "xyz789ghi012345"
```

### Étape 5 : Recompiler l'APK

```bash
cd /home/maun/osm-android
./gradlew assembleDebug
# Nouvel APK dans : app/build/outputs/apk/debug/app-debug.apk
```

**OU** pusher sur GitHub et laisser Actions compiler :
```bash
git add .
git commit -m "feat: add OAuth credentials"
git push origin main
# APK disponible dans Actions après 5 minutes
```

## ⚠️ IMPORTANT - Sécurité

### ❌ NE JAMAIS faire ça en production :

Mettre les clés directement dans le code n'est OK que pour :
- ✅ Tests personnels
- ✅ Développement local
- ✅ Prototypes

### ✅ Pour une vraie app publique :

Utiliser `local.properties` ou `BuildConfig` :

```kotlin
// Dans local.properties (non commité)
OSM_CONSUMER_KEY=votre_cle
OSM_CONSUMER_SECRET=votre_secret

// Dans build.gradle.kts
buildConfigField("String", "OSM_KEY", "\"${project.property("OSM_CONSUMER_KEY")}\"")

// Dans le code
private const val CONSUMER_KEY = BuildConfig.OSM_KEY
```

## 🎯 Après configuration

Une fois les clés configurées :
1. ✅ Se connecter fonctionnera
2. ✅ Vous pourrez ajouter des caméras
3. ✅ Les contributions seront liées à votre compte OSM

## 📞 Besoin d'aide ?

- Wiki OSM OAuth : https://wiki.openstreetmap.org/wiki/OAuth
- Issues GitHub : https://github.com/muarf/BalanceTaCam/issues

