# 🚀 Guide Complet - Publier sur GitHub

Ce guide vous accompagne étape par étape pour publier OSM Camera Mapper sur GitHub.

## 📋 Prérequis

### 1. Créer un compte GitHub
Si vous n'avez pas encore de compte :
1. Allez sur https://github.com
2. Cliquez sur "Sign up"
3. Suivez les étapes d'inscription

### 2. Installer Git
```bash
# Vérifier si Git est installé
git --version

# Si non installé, installer :
sudo apt-get update
sudo apt-get install git
```

### 3. Configurer Git
```bash
git config --global user.name "Votre Nom"
git config --global user.email "votre.email@example.com"
```

## 🔧 Étape 1 : Préparer le Projet

### Initialiser le dépôt Git local
```bash
cd /home/maun/osm-android

# Initialiser Git (si pas déjà fait)
git init

# Ajouter tous les fichiers
git add .

# Premier commit
git commit -m "Initial commit - OSM Camera Mapper v1.0.0"
```

### Vérifier le .gitignore
Le fichier `.gitignore` est déjà configuré pour exclure :
- Fichiers de build
- Credentials (secrets)
- Fichiers IDE
- APK générés

⚠️ **IMPORTANT** : Ne JAMAIS commit vos clés OAuth !

## 🌐 Étape 2 : Créer le Dépôt GitHub

### Via l'interface web GitHub

1. **Connectez-vous à GitHub**
2. **Cliquez sur "+" en haut à droite → "New repository"**
3. **Remplissez les informations** :
   - **Repository name** : `BalanceTaCam`
   - **Description** : `Android app to map surveillance cameras on OpenStreetMap`
   - **Visibility** : Public (pour open source)
   - **Ne pas cocher** "Initialize with README" (vous en avez déjà un)
   - **License** : GNU General Public License v3.0

4. **Cliquez sur "Create repository"**

## 🔗 Étape 3 : Lier et Pousser

### Lier le dépôt local au dépôt GitHub
```bash
# Remplacer 'votre-username' par votre nom d'utilisateur GitHub
git remote add origin https://github.com/votre-username/BalanceTaCam.git

# Vérifier
git remote -v
```

### Pousser le code
```bash
# Pousser la branche main
git branch -M main
git push -u origin main
```

Si demandé, entrez vos identifiants GitHub.

### Alternative : Utiliser SSH (recommandé)

**Configurer SSH** :
```bash
# Générer une clé SSH
ssh-keygen -t ed25519 -C "votre.email@example.com"

# Copier la clé publique
cat ~/.ssh/id_ed25519.pub
```

**Ajouter à GitHub** :
1. GitHub → Settings → SSH and GPG keys
2. New SSH key
3. Coller la clé publique

**Changer l'URL remote** :
```bash
git remote set-url origin git@github.com:votre-username/BalanceTaCam.git
```

## 📝 Étape 4 : Configurer le Dépôt GitHub

### 1. Ajouter une Description
- Allez sur votre dépôt GitHub
- Cliquez sur l'icône ⚙️ à côté de "About"
- Ajoutez :
  - **Description** : "Open-source Android app to map surveillance cameras on OpenStreetMap"
  - **Website** : URL de documentation (optionnel)
  - **Topics** : `android`, `openstreetmap`, `kotlin`, `jetpack-compose`, `surveillance`, `mapping`

### 2. Configurer la Page README
GitHub affichera automatiquement votre `README.md` sur la page principale.

### 3. Activer GitHub Issues
Settings → Features → Issues (cocher)

### 4. Configurer les Secrets pour CI/CD

Pour que GitHub Actions fonctionne avec les releases signées :

**Settings → Secrets and variables → Actions → New repository secret**

Ajouter ces secrets :
- `KEYSTORE_FILE` : Votre fichier keystore encodé en base64
- `KEYSTORE_PASSWORD` : Mot de passe du keystore
- `KEY_ALIAS` : Alias de la clé
- `KEY_PASSWORD` : Mot de passe de la clé

**Générer le keystore encodé** :
```bash
base64 -w 0 your-keystore.jks > keystore-base64.txt
```

## 🏷️ Étape 5 : Créer la Première Release

### Via l'interface GitHub

1. **Allez dans "Releases" → "Create a new release"**
2. **Tag version** : `v1.0.0`
3. **Release title** : `OSM Camera Mapper v1.0.0 - Initial Release`
4. **Description** :
```markdown
## 🎉 Initial Release

First stable version of OSM Camera Mapper!

### Features
- ✅ Interactive map with existing cameras
- ✅ Add new surveillance cameras (quick & detailed modes)
- ✅ OpenStreetMap authentication
- ✅ GPS location tracking
- ✅ Multilingual support (EN, FR, ES, DE)
- ✅ Material 3 design

### Download
- `app-release.apk` - Production build
- Minimum Android version: 7.0 (API 24)

### Setup
See [SETUP.md](SETUP.md) for configuration instructions.

### Notes
⚠️ **Important**: You need to configure OAuth credentials before using the app. See documentation.
```

5. **Upload APK** : Si vous avez déjà un APK compilé
6. **Publish release**

### Via Git Tags
```bash
# Créer un tag
git tag -a v1.0.0 -m "Version 1.0.0 - Initial release"

# Pousser le tag
git push origin v1.0.0
```

GitHub Actions créera automatiquement la release et l'APK.

## 📱 Étape 6 : Ajouter des Badges au README

Mettez à jour votre README.md avec des badges GitHub :

```markdown
# OSM Camera Mapper

[![Release](https://img.shields.io/github/v/release/votre-username/BalanceTaCam)](https://github.com/votre-username/BalanceTaCam/releases)
[![Build](https://github.com/votre-username/BalanceTaCam/workflows/Android%20CI/badge.svg)](https://github.com/votre-username/BalanceTaCam/actions)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
```

## 🌟 Étape 7 : Bonnes Pratiques

### Structure de Branches

**Créer une branche develop** :
```bash
git checkout -b develop
git push -u origin develop
```

**Workflow recommandé** :
- `main` : Code production stable
- `develop` : Développement en cours
- `feature/*` : Nouvelles fonctionnalités
- `bugfix/*` : Corrections de bugs

### Protection de la Branche Main

**Settings → Branches → Add branch protection rule** :
- Branch name pattern : `main`
- Cocher :
  - ✅ Require a pull request before merging
  - ✅ Require status checks to pass before merging
  - ✅ Require branches to be up to date

### Template de Pull Request

Créer `.github/PULL_REQUEST_TEMPLATE.md` :
```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Tests pass
- [ ] Manual testing completed

## Checklist
- [ ] Code follows style guidelines
- [ ] Documentation updated
- [ ] No new warnings
```

### Template d'Issue

Créer `.github/ISSUE_TEMPLATE/bug_report.md` :
```markdown
---
name: Bug Report
about: Report a bug
title: '[BUG] '
labels: bug
---

## Description
Clear description of the bug

## Steps to Reproduce
1. Go to '...'
2. Click on '...'
3. See error

## Expected Behavior
What should happen

## Actual Behavior
What actually happens

## Environment
- Device: [e.g. Samsung Galaxy S21]
- Android Version: [e.g. 12]
- App Version: [e.g. 1.0.0]

## Screenshots
If applicable
```

## 📢 Étape 8 : Promouvoir le Projet

### 1. Ajouter à OpenStreetMap Wiki
https://wiki.openstreetmap.org/wiki/Android

### 2. Annoncer sur les Forums
- Forum OSM : https://community.openstreetmap.org/
- Forum OSM France : https://forum.openstreetmap.fr/
- Reddit : r/openstreetmap

### 3. Réseaux Sociaux
- Twitter avec hashtag #OpenStreetMap
- Mastodon (OSM community)

### 4. F-Droid (Distribution Open Source)
https://f-droid.org/docs/Inclusion_How-To/

## 🔄 Workflow de Développement Continu

### Pour chaque nouvelle fonctionnalité :

```bash
# 1. Créer une branche
git checkout develop
git pull origin develop
git checkout -b feature/ma-nouvelle-fonctionnalite

# 2. Développer et commiter
git add .
git commit -m "feat: ajoute nouvelle fonctionnalité"

# 3. Pousser la branche
git push -u origin feature/ma-nouvelle-fonctionnalite

# 4. Créer une Pull Request sur GitHub
# Via l'interface web

# 5. Après merge, nettoyer
git checkout develop
git pull origin develop
git branch -d feature/ma-nouvelle-fonctionnalite
```

### Pour une nouvelle version :

```bash
# 1. Mettre à jour le numéro de version
# Dans app/build.gradle.kts :
versionCode = 2
versionName = "1.1.0"

# 2. Mettre à jour CHANGELOG.md

# 3. Commiter et tagger
git add .
git commit -m "chore: bump version to 1.1.0"
git tag -a v1.1.0 -m "Version 1.1.0"
git push origin develop
git push origin v1.1.0

# 4. Merger dans main
git checkout main
git merge develop
git push origin main

# 5. Créer la release sur GitHub
```

## 📊 Étape 9 : Analytics (Optionnel)

### GitHub Insights
Consultez les statistiques de votre projet :
- **Insights** → **Traffic** : Visiteurs, clones
- **Insights** → **Community** : Contributions

### Shields.io Badges
Ajoutez des badges pour :
- Issues ouvertes
- Pull requests
- Dernière mise à jour
- Téléchargements

## ⚠️ Sécurité

### Checklist Sécurité :
- ✅ Pas de credentials dans le code
- ✅ .gitignore configuré correctement
- ✅ Secrets GitHub configurés
- ✅ Dépendances à jour
- ✅ Code review activée

### Scanner les Secrets
```bash
# Installer truffleHog
pip install truffleHog

# Scanner le repo
trufflehog git file://. --json
```

## 🎉 Félicitations !

Votre projet est maintenant publié sur GitHub !

### URLs Importantes :
- **Repo** : `https://github.com/votre-username/BalanceTaCam`
- **Releases** : `https://github.com/votre-username/BalanceTaCam/releases`
- **Issues** : `https://github.com/votre-username/BalanceTaCam/issues`
- **Actions** : `https://github.com/votre-username/BalanceTaCam/actions`

### Prochaines Étapes :
1. ⭐ Demandez à vos amis de "star" le projet
2. 📝 Écrivez un article de blog
3. 🎥 Créez une vidéo de démo
4. 🌍 Contribuez à OSM avec votre app !

## 📞 Besoin d'Aide ?

- GitHub Docs : https://docs.github.com
- Git Guide : https://git-scm.com/book
- Open Source Guide : https://opensource.guide

