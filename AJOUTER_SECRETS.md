# 🔐 Comment Ajouter les Secrets GitHub

## Étape par Étape (5 minutes)

### 1. Allez sur la Page Secrets

**Lien direct** : https://github.com/muarf/BalanceTaCam/settings/secrets/actions

**Ou manuellement** :
1. Allez sur https://github.com/muarf/BalanceTaCam
2. Cliquez **"Settings"**
3. Menu gauche → **"Secrets and variables"** → **"Actions"**

---

### 2. Ajoutez le Secret 1 : KEYSTORE_FILE

1. Cliquez **"New repository secret"**
2. **Name** : `KEYSTORE_FILE`
3. **Secret** : Copiez TOUT le contenu de ce fichier :
```bash
cat /home/maun/osm-android/balancetacam.keystore.base64
```
4. Cliquez **"Add secret"**

---

### 3. Ajoutez le Secret 2 : KEYSTORE_PASSWORD

1. Cliquez **"New repository secret"**
2. **Name** : `KEYSTORE_PASSWORD`
3. **Secret** : `balancetacam2025`
4. Cliquez **"Add secret"**

---

### 4. Ajoutez le Secret 3 : KEY_ALIAS

1. Cliquez **"New repository secret"**
2. **Name** : `KEY_ALIAS`
3. **Secret** : `balancetacam`
4. Cliquez **"Add secret"**

---

### 5. Ajoutez le Secret 4 : KEY_PASSWORD

1. Cliquez **"New repository secret"**
2. **Name** : `KEY_PASSWORD`
3. **Secret** : `balancetacam2025`
4. Cliquez **"Add secret"**

---

## ✅ Vérification

Vous devriez voir **4 secrets** dans la liste :
```
KEYSTORE_FILE       Updated now
KEYSTORE_PASSWORD   Updated now
KEY_ALIAS          Updated now
KEY_PASSWORD       Updated now
```

---

## 🚀 Test du Keystore

### Déclencher un Build Signé

```bash
cd /home/maun/osm-android
git commit --allow-empty -m "test: trigger signed build"
git push origin main
```

Attendez 5 minutes, puis téléchargez le nouvel APK.

### Tester la Mise à Jour

**SANS désinstaller** :
```bash
adb install -r BalanceTaCam-nouvelle-version.apk
```

**Si ça marche** → ✅ Keystore configuré correctement !  
**Si ça échoue** → Un secret est mal configuré

---

## 🆘 Problèmes Courants

### "Secret too large"
Le KEYSTORE_FILE est peut-être coupé. Vérifiez que TOUT le contenu base64 est copié.

### "Signing failed"
- Vérifiez que les mots de passe sont corrects
- Pas d'espaces avant/après les valeurs

### "Keystore not found"
Le secret KEYSTORE_FILE n'est pas configuré ou vide.

---

## 💾 Sauvegarde

**IMPORTANT** : Sauvegardez `balancetacam.keystore` !

```bash
# Copier dans un endroit sûr
cp /home/maun/osm-android/balancetacam.keystore ~/backup/
```

Sans ce fichier, vous ne pourrez plus signer de nouvelles versions !

---

**Prêt ? Allez configurer les secrets ! 🚀**

