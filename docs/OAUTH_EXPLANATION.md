# 🔐 Explication du Système OAuth

Ce document explique comment fonctionne l'authentification dans OSM Camera Mapper.

## ❓ Pourquoi OAuth avec OpenStreetMap ?

### Concept Clé : PAS de Compte Séparé

**L'application N'A PAS son propre système de comptes**. Voici pourquoi c'est une bonne chose :

1. **Simplicité** : Les utilisateurs utilisent leur compte OSM existant
2. **Sécurité** : On ne stocke jamais de mots de passe
3. **Traçabilité** : Chaque contribution est liée au compte OSM de l'utilisateur
4. **Standard OSM** : Toutes les apps OSM fonctionnent ainsi

## 🔄 Comment Ça Fonctionne ?

### Flux d'Authentification (OAuth 1.0a)

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│             │         │              │         │             │
│  UTILISATEUR│         │  VOTRE APP   │         │  OSM SERVER │
│             │         │              │         │             │
└──────┬──────┘         └───────┬──────┘         └──────┬──────┘
       │                        │                       │
       │ 1. Clic "Se connecter" │                       │
       │───────────────────────>│                       │
       │                        │                       │
       │                        │ 2. Demande Request Token
       │                        │──────────────────────>│
       │                        │                       │
       │                        │ 3. Retourne Token     │
       │                        │<──────────────────────│
       │                        │                       │
       │ 4. Ouvre navigateur    │                       │
       │<───────────────────────│                       │
       │                                                │
       │ 5. Page de connexion OSM                      │
       │───────────────────────────────────────────────>│
       │                                                │
       │ 6. Utilisateur se connecte + Autorise l'app  │
       │───────────────────────────────────────────────>│
       │                                                │
       │ 7. Redirect vers app avec Verifier           │
       │<───────────────────────────────────────────────│
       │                        │                       │
       │ 8. Retour dans l'app   │                       │
       │───────────────────────>│                       │
       │                        │                       │
       │                        │ 9. Échange Verifier   │
       │                        │    contre Access Token│
       │                        │──────────────────────>│
       │                        │                       │
       │                        │ 10. Access Token      │
       │                        │<──────────────────────│
       │                        │                       │
       │ 11. Connecté ! ✅      │                       │
       │<───────────────────────│                       │
       │                        │                       │
```

### En Détail

#### Étape 1-3 : Préparation
- L'app demande un "Request Token" temporaire à OSM
- Ce token permet de démarrer le processus

#### Étape 4-6 : Authentification
- L'app ouvre le navigateur vers OSM
- **L'utilisateur se connecte sur OSM** (pas dans l'app)
- L'utilisateur autorise l'app à accéder à son compte

#### Étape 7-8 : Retour dans l'App
- OSM redirige vers `osmcamera://oauth?oauth_verifier=...`
- Android ouvre automatiquement l'app

#### Étape 9-10 : Finalisation
- L'app échange le verifier contre un Access Token permanent
- Ce token permet de faire des actions au nom de l'utilisateur

#### Étape 11 : Utilisation
- L'app utilise le token pour créer des changesets et nodes
- Chaque contribution est signée avec le compte de l'utilisateur

## 👤 Multi-Utilisateurs : Comment Ça Marche ?

### Cas d'Usage 1 : Un Utilisateur, Un Téléphone
```
Alice installe l'app
  → Se connecte avec son compte OSM "alice_mapper"
  → Toutes ses contributions = compte "alice_mapper"
```

### Cas d'Usage 2 : Plusieurs Utilisateurs, Un Téléphone
```
Alice utilise l'app
  → Se connecte avec "alice_mapper"
  → Ajoute 5 caméras
  → Se déconnecte

Bob prend le téléphone
  → Se connecte avec "bob_cartographer"  
  → Ajoute 3 caméras
  → Ses contributions ≠ celles d'Alice
```

### Cas d'Usage 3 : Un Utilisateur, Plusieurs Téléphones
```
Alice sur téléphone 1
  → Connectée avec "alice_mapper"
  
Alice sur téléphone 2
  → Connectée avec "alice_mapper"
  
Toutes les contributions = même compte OSM
```

## 🔑 Stockage des Tokens

### Dans l'App

```kotlin
// Les tokens OAuth sont stockés de manière sécurisée
EncryptedSharedPreferences
  ├── access_token: "abc123..."
  ├── access_token_secret: "xyz789..."
  ├── user_id: 123456
  └── user_name: "alice_mapper"
```

### Sécurité
- ✅ Chiffrement AES256-GCM
- ✅ MasterKey Android
- ✅ Pas accessible par d'autres apps
- ✅ Supprimé à la déconnexion

## 🚫 Ce Que L'App NE Fait PAS

### ❌ Pas de Backend Propre
L'app ne communique PAS avec un serveur intermédiaire :
```
App ──X──> Votre Serveur ──X──> OSM  ❌ NON
App ──────────────────────────> OSM  ✅ OUI
```

### ❌ Pas de Base de Données Utilisateurs
L'app n'a PAS de table "users" :
```sql
-- Ce qu'on N'a PAS ❌
CREATE TABLE users (
  id, username, password, email
);

-- Ce qu'on A ✅
CREATE TABLE cameras (
  id, latitude, longitude, tags
);
```

### ❌ Pas de Gestion de Mots de Passe
```kotlin
// Ce qu'on N'a PAS ❌
fun login(username: String, password: String)
fun register(username: String, password: String)

// Ce qu'on A ✅
fun startOAuthFlow()
fun handleOAuthCallback(verifier: String)
```

## 💡 Pourquoi C'est Mieux Ainsi ?

### Avantages

1. **Pas de Duplication**
   - Pourquoi créer un compte si on a déjà OSM ?
   - Réduit la friction utilisateur

2. **Sécurité**
   - Pas de risque de fuite de mots de passe
   - OSM gère la sécurité
   - Pas de serveur à sécuriser

3. **Simplicité**
   - Pas de backend à maintenir
   - Pas de base de données users
   - Moins de code = moins de bugs

4. **Conformité**
   - Standard OSM
   - Toutes les bonnes pratiques respectées

5. **Traçabilité**
   - Chaque contribution est attribuée
   - Historique OSM complet

## 🔐 Configuration OAuth (Pour le Développeur)

### Étape 1 : S'inscrire sur OSM
```
1. Créer un compte sur openstreetmap.org
2. Aller dans Settings → OAuth applications
3. Cliquer "Register new application"
```

### Étape 2 : Configurer l'Application OSM
```
Name: OSM Camera Mapper
Main Application URL: https://github.com/votre-user/BalanceTaCam
Callback URL: osmcamera://oauth
Permissions: 
  ✅ Read user preferences
  ✅ Modify the map
```

### Étape 3 : Obtenir les Clés
```
Consumer Key: abc123def456...
Consumer Secret: xyz789ghi012...
```

### Étape 4 : Configurer dans le Code
```kotlin
// Dans data/auth/OAuthService.kt
companion object {
    private const val CONSUMER_KEY = "abc123def456..."
    private const val CONSUMER_SECRET = "xyz789ghi012..."
    private const val CALLBACK_URL = "osmcamera://oauth"
}
```

## 🧪 Test du Flux OAuth

### Test Complet
```
1. Lancer l'app
2. Cliquer "Se connecter avec OSM"
3. Navigateur s'ouvre → Page OSM
4. Se connecter (ou créer compte)
5. Autoriser l'application
6. Redirection automatique vers l'app
7. ✅ Connecté !
```

### Vérifier l'État
```kotlin
// Dans MapScreen
val isAuthenticated = authViewModel.isAuthenticated()
val user = authViewModel.user.collectAsState()

if (isAuthenticated) {
    Text("Connecté en tant que ${user.displayName}")
}
```

## 🔄 Déconnexion

### Comment ça fonctionne
```kotlin
fun logout() {
    // 1. Supprimer les tokens locaux
    preferencesManager.clearOAuthTokens()
    
    // 2. Réinitialiser l'état
    _user.value = null
    _uiState.value = AuthUiState.NotAuthenticated
}
```

### Attention
La déconnexion de l'app **n'annule PAS** l'autorisation sur OSM.

Pour révoquer complètement :
1. Aller sur openstreetmap.org
2. Settings → OAuth authorized applications
3. Révoquer "OSM Camera Mapper"

## 📱 UX Recommendations

### Améliorer l'Expérience Utilisateur

#### 1. Écran de Bienvenue
```kotlin
// Première ouverture
if (isFirstLaunch) {
    WelcomeScreen(
        onContinue = { 
            navigateToAuth() 
        }
    )
}
```

#### 2. Explications Claires
```kotlin
Card {
    Text("Pourquoi se connecter ?")
    Text("""
        - Contribuer à OpenStreetMap
        - Vos contributions sont signées
        - Pas de nouveau compte à créer
    """)
}
```

#### 3. Gestion d'Erreurs
```kotlin
when (authState) {
    is Error -> {
        Text("Erreur de connexion")
        Text("Vérifiez votre connexion Internet")
        Button("Réessayer")
    }
}
```

## 🆘 Dépannage

### "Callback URL mismatch"
```
❌ osmcamera://callback  
✅ osmcamera://oauth
```
Le callback DOIT être exactement `osmcamera://oauth`

### "Invalid consumer key"
Vérifiez que les clés sont correctes dans `OAuthService.kt`

### "Browser not opening"
Test sur appareil physique (pas émulateur)

## 📚 Références

- [OSM OAuth Documentation](https://wiki.openstreetmap.org/wiki/OAuth)
- [OAuth 1.0a Spec](https://oauth.net/core/1.0a/)
- [ScribeJava Docs](https://github.com/scribejava/scribejava)

## ✅ Résumé

**L'OAuth dans cette app = Connexion avec compte OSM**

- ✅ Chaque utilisateur se connecte avec SON compte OSM
- ✅ Pas besoin de système d'inscription séparé
- ✅ Sécurisé et standard
- ✅ Facile à utiliser
- ✅ Contributions traçables

**C'est exactement comme "Se connecter avec Google" mais pour OSM !**

