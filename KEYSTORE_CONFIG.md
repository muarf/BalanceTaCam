# 🔑 Configuration du Keystore BalanceTaCam

## ✅ Keystore Créé !

Le keystore permanent a été généré pour BalanceTaCam.

### 📋 Informations

```
Fichier : balancetacam.keystore
Alias : balancetacam
Store Password : balancetacam2025
Key Password : balancetacam2025
Validité : 10000 jours (~27 ans)
```

---

## 🌐 Configuration GitHub Secrets

### Étape 1 : Aller dans Settings

1. **Allez sur** : https://github.com/muarf/BalanceTaCam/settings/secrets/actions
2. **Ou** : Votre repo → Settings → Secrets and variables → Actions

### Étape 2 : Ajouter les Secrets

Cliquez **"New repository secret"** et ajoutez **4 secrets** :

#### Secret 1 : KEYSTORE_FILE
```
Name: KEYSTORE_FILE
Value: [Coller le contenu de balancetacam.keystore.base64]
```

**Pour obtenir le contenu** :
```bash
cat /home/maun/osm-android/balancetacam.keystore.base64
```

#### Secret 2 : KEYSTORE_PASSWORD
```
Name: KEYSTORE_PASSWORD
Value: balancetacam2025
```

#### Secret 3 : KEY_ALIAS
```
Name: KEY_ALIAS
Value: balancetacam
```

#### Secret 4 : KEY_PASSWORD
```
Name: KEY_PASSWORD
Value: balancetacam2025
```

---

## ⚙️ GitHub Actions Est Déjà Configuré !

Le workflow `.github/workflows/android-build.yml` est déjà prêt à utiliser ces secrets.

Dès que vous ajoutez les secrets, **toutes les prochaines versions** seront signées avec ce keystore !

---

## 🧪 Test

Après avoir configuré les secrets :

1. **Faites un commit** :
```bash
git commit --allow-empty -m "test: trigger signed build"
git push origin main
```

2. **Attendez la compilation** (~5 min)

3. **Téléchargez le nouvel APK**

4. **Installez PAR DESSUS** l'ancienne version :
```bash
adb install -r nouvelle-version.apk
```

**Ça devrait marcher cette fois !** ✅

---

## 🔒 Sécurité du Keystore

### ⚠️ IMPORTANT - Sauvegardez le Keystore !

```bash
# Copier dans un endroit sûr
cp balancetacam.keystore ~/Documents/backup/
# Ou sur une clé USB
```

**Si vous perdez ce keystore** :
- ❌ Vous ne pourrez plus mettre à jour l'app
- ❌ Il faudra republier sous un nouveau nom
- ❌ Les utilisateurs devront désinstaller/réinstaller

### ✅ Le Keystore est Sécurisé

- Dans `.gitignore` (pas commité)
- Dans GitHub Secrets (chiffré)
- Mots de passe protégés

---

## 📦 APK Release vs Debug

Avec le keystore, on peut générer 2 types d'APK :

### Debug APK (actuel)
- 20 MB
- Rapide à compiler
- Pour tests

### Release APK (nouveau possible)
- ~15 MB (optimisé)
- Code obfusqué (ProGuard)
- Prêt pour distribution

---

## 🎯 Prochaines Étapes

1. **Copiez** le contenu de `balancetacam.keystore.base64`
2. **Allez sur** : https://github.com/muarf/BalanceTaCam/settings/secrets/actions
3. **Ajoutez** les 4 secrets
4. **Je recompile** une version signée
5. **Vous testez** la mise à jour directe !

Dites-moi quand vous avez ajouté les secrets ! 🚀

