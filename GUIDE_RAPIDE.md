# 🚀 Guide Rapide - Publication GitHub

## 🔐 IMPORTANT : OAuth Multi-Utilisateurs

### ✅ Bonne Nouvelle : C'est Déjà Fait !

**L'OAuth fonctionne déjà pour chaque utilisateur de votre app** ! Voici comment :

```
📱 Alice télécharge votre app
   ↓
👤 Elle clique "Se connecter"
   ↓
🌐 Elle se connecte avec SON compte OpenStreetMap
   ↓
✅ Toutes ses contributions = son compte OSM

📱 Bob télécharge aussi l'app
   ↓
👤 Il se connecte avec SON compte OSM
   ↓
✅ Ses contributions ≠ celles d'Alice
```

### 💡 Comment Ça Marche ?

**Pas besoin de page inscription/connexion séparée** ! 

C'est comme :
- "Se connecter avec Google" ➜ mais pour OpenStreetMap
- "Se connecter avec Facebook" ➜ mais pour OpenStreetMap

Chaque utilisateur utilise son compte OSM personnel. Simple et sécurisé !

### 📖 Plus d'infos
Voir `docs/OAUTH_EXPLANATION.md` pour les détails techniques.

---

## 🌐 Publier sur GitHub

### Étape 1️⃣ : Créer un Compte GitHub (si besoin)

```
1. Aller sur https://github.com
2. Cliquer "Sign up"
3. Suivre les étapes
```

### Étape 2️⃣ : Configurer Git

```bash
# Ouvrir un terminal
git config --global user.name "Votre Nom"
git config --global user.email "votre.email@example.com"
```

### Étape 3️⃣ : Initialiser le Projet

```bash
# Aller dans le dossier du projet
cd /home/maun/osm-android

# Initialiser Git
git init

# Ajouter tous les fichiers
git add .

# Premier commit
git commit -m "Initial commit - BalanceTaCam v1.0.0"
```

### Étape 4️⃣ : Créer le Dépôt sur GitHub

1. **Aller sur GitHub** et se connecter
2. **Cliquer le "+" en haut à droite** → "New repository"
3. **Remplir** :
   - **Nom** : `BalanceTaCam`
   - **Description** : `Application Android pour cartographier les caméras de surveillance sur OpenStreetMap`
   - **Public** (coché)
   - **License** : GNU General Public License v3.0
   - ⚠️ **NE PAS cocher** "Initialize with README"
4. **Cliquer "Create repository"**

### Étape 5️⃣ : Lier et Pousser

```bash
# Remplacer 'VOTRE_USERNAME' par votre nom GitHub
git remote add origin https://github.com/VOTRE_USERNAME/BalanceTaCam.git

# Renommer la branche en main
git branch -M main

# Pousser le code
git push -u origin main
```

**Si demandé** : Entrez votre nom d'utilisateur et mot de passe GitHub.

### Étape 6️⃣ : ✅ C'est Fait !

Votre projet est maintenant en ligne :
```
https://github.com/VOTRE_USERNAME/BalanceTaCam
```

---

## 🎨 Personnaliser le README

### Mettre à jour les liens

**Ouvrir** `README.md` et **remplacer** `muarf` par votre vrai nom :

```markdown
# Chercher et remplacer :
muarf → VOTRE_USERNAME_GITHUB

# Par exemple :
https://github.com/muarf/BalanceTaCam
→ https://github.com/alice_dev/BalanceTaCam
```

**Sauvegarder et pousser** :
```bash
git add README.md
git commit -m "docs: update GitHub username in README"
git push
```

---

## 🔑 Configurer OAuth (OBLIGATOIRE)

Pour que l'app fonctionne, vous DEVEZ configurer OAuth :

### 1. S'inscrire sur OSM

```
1. Aller sur https://www.openstreetmap.org
2. Créer un compte (si besoin)
3. Se connecter
```

### 2. Créer une Application OAuth

```
1. Cliquer sur votre nom → "Settings"
2. Onglet "oauth 2 applications"
3. En bas : "OAuth 1 applications" → "Register your application"
4. Remplir :
   - Name: BalanceTaCam
   - Main Application URL: https://github.com/VOTRE_USERNAME/BalanceTaCam
   - Callback URL: osmcamera://oauth
   - Permissions: ✅ Modify the map
5. Cliquer "Register"
```

### 3. Copier les Clés

Vous obtiendrez :
```
Consumer Key: abc123def456789...
Consumer Secret: xyz789ghi012345...
```

### 4. Mettre dans le Code

**Ouvrir** : `app/src/main/java/com/osmcamera/mapper/data/auth/OAuthService.kt`

**Ligne 19-20**, remplacer :
```kotlin
private const val CONSUMER_KEY = "votre_consumer_key_ici"
private const val CONSUMER_SECRET = "votre_consumer_secret_ici"
```

**Par vos vraies clés** :
```kotlin
private const val CONSUMER_KEY = "abc123def456789"
private const val CONSUMER_SECRET = "xyz789ghi012345"
```

⚠️ **ATTENTION** : Ne JAMAIS pusher ces clés sur GitHub public !

**Mieux : Utiliser un fichier local** (non commité) :

```bash
# Créer un fichier secrets.properties
echo "OSM_CONSUMER_KEY=abc123def456789" > secrets.properties
echo "OSM_CONSUMER_SECRET=xyz789ghi012345" >> secrets.properties

# Ajouter au .gitignore
echo "secrets.properties" >> .gitignore
```

---

## 📱 Compiler l'APK

### Debug (pour tester)

```bash
cd /home/maun/osm-android
./gradlew assembleDebug
```

**APK généré** : `app/build/outputs/apk/debug/app-debug.apk`

### Release (pour distribuer)

```bash
./gradlew assembleRelease
```

**APK généré** : `app/build/outputs/apk/release/app-release.apk`

---

## 🏷️ Créer une Release GitHub

### Via l'Interface Web

1. **Aller sur votre dépôt GitHub**
2. **Cliquer "Releases"** → "Create a new release"
3. **Remplir** :
   - **Tag** : `v1.0.0`
   - **Title** : `Version 1.0.0 - Release initiale`
   - **Description** :
```markdown
## 🎉 Première Version !

### Fonctionnalités
- ✅ Carte interactive avec caméras existantes
- ✅ Ajout de nouvelles caméras (modes rapide et détaillé)
- ✅ Authentification OpenStreetMap
- ✅ Localisation GPS
- ✅ Support multilingue (FR, EN, ES, DE)

### Téléchargement
- Android 7.0 minimum
- Voir SETUP.md pour la configuration

### Important
⚠️ Configuration OAuth requise (voir documentation)
```

4. **Uploader l'APK** (glisser-déposer `app-debug.apk`)
5. **Publish release**

---

## 📊 Après Publication

### Ajouter des Topics

Sur GitHub :
1. Cliquer l'icône ⚙️ à côté de "About"
2. Ajouter des topics :
   - `android`
   - `openstreetmap`
   - `kotlin`
   - `jetpack-compose`
   - `surveillance`
   - `mapping`

### Promouvoir

- 📢 Forum OSM France : https://forum.openstreetmap.fr/
- 🐦 Twitter avec #OpenStreetMap
- 🌍 Wiki OSM : https://wiki.openstreetmap.org/wiki/Android

---

## ❓ Questions Fréquentes

### Q: Dois-je créer un système d'inscription ?
**R:** NON ! L'OAuth OSM gère déjà tout. Chaque utilisateur se connecte avec son compte OSM.

### Q: Comment plusieurs utilisateurs peuvent utiliser l'app ?
**R:** Chacun se connecte avec son propre compte OSM. Leurs contributions sont séparées.

### Q: Que faire des clés OAuth ?
**R:** Les garder secrètes ! Ne JAMAIS les pusher sur GitHub public.

### Q: L'app peut fonctionner sans backend ?
**R:** OUI ! Elle communique directement avec l'API OpenStreetMap.

### Q: Comment gérer plusieurs versions ?
**R:** Utiliser des tags Git : `v1.0.0`, `v1.1.0`, etc.

---

## 🆘 Besoin d'Aide ?

### Documentation Complète
- 📖 `GITHUB_PUBLISHING.md` - Guide détaillé GitHub
- 🔐 `docs/OAUTH_EXPLANATION.md` - Explications OAuth
- 🛠️ `SETUP.md` - Configuration développement

### Commandes Git Utiles

```bash
# Voir l'état
git status

# Voir l'historique
git log --oneline

# Annuler les changements non commités
git checkout -- .

# Créer une branche
git checkout -b ma-branche

# Pousser une branche
git push -u origin ma-branche

# Mettre à jour depuis GitHub
git pull origin main
```

---

## ✅ Checklist Finale

- [ ] Compte GitHub créé
- [ ] Projet poussé sur GitHub
- [ ] README personnalisé avec votre username
- [ ] OAuth configuré sur openstreetmap.org
- [ ] Clés OAuth dans le code
- [ ] APK compilé avec succès
- [ ] Release v1.0.0 créée
- [ ] Topics ajoutés au repo
- [ ] Documentation lue

## 🎉 Bravo !

Votre application est maintenant :
- ✅ Open source sur GitHub
- ✅ Prête à être utilisée
- ✅ Avec OAuth multi-utilisateurs
- ✅ Documentée complètement

**Prochain objectif** : Trouver des contributeurs et mapper des caméras ! 🗺️📷

