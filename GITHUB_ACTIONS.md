# 🚀 GitHub Actions - Compilation Automatique

## 📱 Téléchargement d'APK

### Pour les Features (Branches feature/*)
1. **Push sur une branche feature** → GitHub Action se lance automatiquement
2. **Aller dans l'onglet "Actions"** sur GitHub
3. **Cliquer sur le workflow "Build APK"** en cours
4. **Télécharger l'APK** depuis les artifacts ou la release

### Pour les Releases (Tags v*)
1. **Créer un tag** : `git tag v1.0.0 && git push origin v1.0.0`
2. **Aller dans "Releases"** sur GitHub
3. **Télécharger l'APK** de la release

## 🔧 Workflows Disponibles

### 1. Build APK (Feature Branches)
- **Déclencheur** : Push sur `feature/*` ou `main`
- **Produit** : APK Debug
- **Artifacts** : Disponibles 30 jours
- **Release** : Créée automatiquement pour les features

### 2. Build Release APK (Tags)
- **Déclencheur** : Push de tag `v*`
- **Produit** : APK Release
- **Artifacts** : Disponibles 90 jours
- **Release** : Release officielle

## 📋 Instructions d'Installation

### Sur Android
1. **Télécharger l'APK** depuis GitHub
2. **Activer "Sources inconnues"** :
   - Paramètres → Sécurité → Sources inconnues
   - Ou Paramètres → Applications → Accès spécial → Installer des applications inconnues
3. **Installer l'APK** téléchargé
4. **Lancer l'application**

### Vérification
- L'app doit se lancer sans erreur
- Les icônes de caméras doivent être stylisées
- Les fonctionnalités doivent être opérationnelles

## 🔍 Debugging

### Logs GitHub Actions
1. Aller dans l'onglet "Actions"
2. Cliquer sur le workflow en cours
3. Cliquer sur "build" pour voir les logs détaillés

### Problèmes Courants
- **SDK non trouvé** : Vérifier la configuration Android SDK
- **Gradle échoue** : Vérifier les dépendances
- **APK corrompu** : Re-télécharger depuis les artifacts

## 🎯 Utilisation pour le Développement

### Workflow Recommandé
1. **Développer** sur une branche `feature/nom-feature`
2. **Push** → APK généré automatiquement
3. **Tester** l'APK sur votre téléphone
4. **Corriger** si nécessaire
5. **Merge** vers main quand prêt

### Commandes Utiles
```bash
# Créer une nouvelle feature
git checkout -b feature/ma-nouvelle-feature

# Push pour déclencher la compilation
git push -u origin feature/ma-nouvelle-feature

# Créer une release
git tag v1.0.0
git push origin v1.0.0
```

## 📊 Statut des Builds

- ✅ **Succès** : APK disponible en téléchargement
- ❌ **Échec** : Vérifier les logs pour diagnostiquer
- 🟡 **En cours** : Attendre la fin de la compilation

## 🔗 Liens Utiles

- **Actions** : https://github.com/muarf/BalanceTaCam/actions
- **Releases** : https://github.com/muarf/BalanceTaCam/releases
- **Artifacts** : Disponibles dans chaque workflow terminé