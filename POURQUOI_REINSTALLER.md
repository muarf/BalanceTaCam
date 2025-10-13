# 🔐 Pourquoi Désinstaller puis Réinstaller ?

## 🤔 Le Problème

Quand vous essayez de juste installer l'APK par dessus l'ancienne version, Android refuse avec :
```
App not installed
Package conflicts with an existing package
```

## 🔍 La Cause : Signature APK Différente

### Chaque APK Debug = Signature Unique

Les **APK debug** que GitHub Actions compile ont une **signature différente** à chaque build car :

1. GitHub utilise un **keystore debug temporaire**
2. Ce keystore change à chaque build
3. Android refuse d'installer un APK avec une signature différente

**C'est comme si** :
- Version 1 signée par "Personne A"
- Version 2 signée par "Personne B"
- Android dit : "Non ! C'est pas la même personne !"

## ✅ La Solution : Utiliser un Keystore Permanent

### Option 1 : Créer un Keystore (Recommandé)

Je peux créer un keystore permanent et configurer GitHub Actions pour l'utiliser.

**Avantages** :
- ✅ Mise à jour directe (pas besoin de désinstaller)
- ✅ APK signé de manière cohérente
- ✅ Prêt pour Google Play si vous voulez

**Je fais ça ?** Dites "oui crée le keystore" et je configure tout !

### Option 2 : Compiler Localement

Compiler sur votre machine avec votre propre keystore debug.

```bash
cd /home/maun/osm-android
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Tous les APK compilés localement auront la **même signature** (la vôtre).

### Option 3 : Accepter de Désinstaller

Continuer à désinstaller puis réinstaller à chaque fois.

```bash
adb uninstall com.osmcamera.mapper
adb install nouvelle-version.apk
```

## 🎯 Solution Recommandée : Keystore Permanent

### Ce Que Je Peux Faire :

1. **Créer un keystore** pour BalanceTaCam
2. **L'encoder en base64** pour GitHub Secrets
3. **Configurer GitHub Actions** pour l'utiliser
4. **Toutes les futures versions** seront signées pareil
5. **Mise à jour directe** fonctionnera !

### Sécurité

Le keystore sera :
- ✅ Stocké dans GitHub Secrets (chiffré)
- ✅ Jamais exposé dans le code
- ✅ Utilisé uniquement par GitHub Actions
- ✅ Sous votre contrôle

## 📝 Commandes Utiles

### Pour Désinstaller Proprement
```bash
adb uninstall com.osmcamera.mapper
```

### Pour Voir la Signature Actuelle
```bash
adb shell pm list packages -f | grep osmcamera
adb shell dumpsys package com.osmcamera.mapper | grep signatures
```

### Pour Compiler Localement
```bash
cd /home/maun/osm-android
./gradlew clean assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
# -r = replace (tente de remplacer)
```

## 🎁 Bonus : APK Release Signé

Si je crée un keystore, je peux aussi générer des **APK release** :
- Plus petits (ProGuard activé)
- Optimisés
- Signés proprement
- Prêts pour distribution

## ❓ Que Voulez-Vous ?

**A.** Je crée un keystore et configure tout (10 min)  
**B.** Vous continuez à désinstaller/réinstaller  
**C.** Vous compilez localement  

Choisissez ! 😊

