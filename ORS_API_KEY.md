# 🔑 Obtenir une Clé API OpenRouteService

## ⏱️ Temps : 2 minutes

### Étape 1 : S'Inscrire

1. **Allez sur** : https://openrouteservice.org/dev/#/signup

2. **Remplissez** :
   ```
   Email: votre.email@example.com
   Nom: muarf
   Organisation: BalanceTaCam (optionnel)
   ```

3. **Cochez** "I agree to the terms"

4. **Cliquez** "Sign Up"

5. **Vérifiez** votre email et cliquez sur le lien de confirmation

---

### Étape 2 : Obtenir la Clé API

1. **Connectez-vous** : https://openrouteservice.org/dev/#/login

2. **Dashboard** : Vous verrez votre clé API immédiatement

3. **Copiez** la clé (format : `5b3ce3597851110001cf6248abc123def456...`)

---

### Étape 3 : Configurer dans l'App

**Donnez-moi la clé** et je la configure dans le code :

```kotlin
// Dans OpenRouteServiceApi.kt
const val API_KEY = "VOTRE_CLE_ICI"
```

---

## 📊 Limites du Plan Gratuit

- **2000 requêtes/jour**
- **40 requêtes/minute**
- **Toutes les fonctionnalités** incluses
- **Pas de carte de crédit** requise

**Largement suffisant pour BalanceTaCam !**

---

## ⚠️ Note

L'API key ORS est **différente** de la clé OAuth OSM.

- **OAuth OSM** : Pour ajouter des caméras sur la carte
- **ORS API** : Pour calculer les itinéraires

Ce sont 2 services séparés.

---

## ✅ Une Fois Configuré

L'app pourra :
- ✅ Calculer des itinéraires
- ✅ Éviter les zones avec caméras
- ✅ Montrer plusieurs alternatives
- ✅ Comparer les routes

---

**Inscrivez-vous (2 min) et donnez-moi la clé ! 🚀**

