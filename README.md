# API de Conversion de Devises

## 🚀 Test rapide (pour un examinateur)

Aucune configuration n'est nécessaire : le fournisseur `open.er-api.com` fonctionne **sans clé API**. En 3 commandes :

```bash
git clone https://github.com/suner-dev/currency-conversion-api.git
cd currency-conversion-api
./mvnw spring-boot:run          # JDK 17+ requis
```

Puis ouvrir **http://localhost:8080/swagger-ui/index.html** et cliquer sur un exemple → *Try it out* → *Execute*.

Test immédiat en ligne de commande :

```bash
curl -X POST http://localhost:8080/api/currency/convert \
  -H "Content-Type: application/json" \
  -d '{"from":"USD","to":"XAF","amount":100}'
```

## Description

API REST professionnelle de conversion de devises. Les taux de change sont **récupérés dynamiquement** auprès d'un fournisseur externe (ExchangeRate-API — `open.er-api.com`) via **Spring WebClient**. **Aucune base de données, aucun stockage local des taux** : le fournisseur externe est la seule source de vérité.

```
Client → API Spring Boot → API externe (taux de change) → conversion → réponse
```

## Technologies

- Java 17+ (testé avec JDK 21)
- Spring Boot 3.4 (Web MVC)
- **Spring WebClient** (WebFlux/Reactor Netty) pour l'appel externe
- Springdoc OpenAPI (Swagger UI)
- Bean Validation (Jakarta Validation)
- JUnit 5, Mockito, MockWebServer (tests — aucun appel réseau réel)

## Prérequis

- JDK 17 ou supérieur
- Maven (wrapper `./mvnw` inclus)
- Une clé API du fournisseur de taux (facultative : `open.er-api.com` fonctionne sans clé)

## Configuration

La clé API n'est **jamais** commitée. Définissez la variable d'environnement :

```bash
export EXCHANGE_RATE_API_KEY="YOUR_API_KEY"
```

Variables supportées (valeurs par défaut dans `src/main/resources/application.properties`) :

| Variable | Défaut | Rôle |
|---|---|---|
| `EXCHANGE_RATE_API_KEY` | *(vide)* | Clé API du fournisseur |
| `EXCHANGE_RATE_BASE_URL` | `https://open.er-api.com/v6` | URL de base du fournisseur |
| `EXCHANGE_RATE_TIMEOUT` | `5000` | Timeout de réponse (ms) |
| `EXCHANGE_RATE_CONNECT_TIMEOUT` | `2000` | Timeout de connexion (ms) |
| `SERVER_PORT` | `8080` | Port HTTP |

## Lancement

```bash
./mvnw spring-boot:run
```

## Swagger

- Swagger UI : **http://localhost:8080/swagger-ui/index.html**
- OpenAPI JSON : http://localhost:8080/v3/api-docs

Le endpoint y est documenté avec des exemples de requête/réponse. **Les valeurs de taux dans les exemples sont des illustrations de structure, pas des taux garantis.**

## Devises supportées

Aucune liste n'est codée en dur : **toutes les devises du fournisseur sont acceptées dynamiquement** (166 devises au moment du test). Parmi elles :

| Code | Monnaie |
|---|---|
| `XAF` | Franc CFA d'Afrique centrale (BEAC) |
| `XOF` | Franc CFA de l'Afrique de l'Ouest (BCEAO) |
| `CNY` | Yuan renminbi chinois |
| `GBP` | Livre sterling |
| `JPY` | Yen japonais |
| `CHF` | Franc suisse |
| `USD`, `EUR`, … | Principales devises internationales |

**Toutes les devises du fournisseur sont convertibles entre elles** (XAF ↔ EUR, XAF ↔ USD, XAF ↔ CNY, XAF ↔ XOF, GBP ↔ XOF, …). Pour découvrir les devises disponibles, l'API expose une liste dynamique :

```bash
curl http://localhost:8080/api/currency/currencies
```

```json
{
  "count": 166,
  "currencies": ["AED", "AFN", "ALL", "AMD", "ANG", "...", "XAF", "XOF", "XPF", "ZAR", "ZMW"],
  "timestamp": "2026-09-09T12:00:00Z"
}
```

Un script de vérification de couverture est également fourni :

```bash
# Matrice des monnaies clés (XAF, XOF, CNY, EUR, USD, GBP, JPY, CHF)
./scripts/verify-currencies.sh http://localhost:8080

# Vérifier TOUTES les devises du fournisseur depuis le XAF
./scripts/verify-currencies.sh http://localhost:8080 --all XAF
```

Exemples :

```bash
# USD → Franc CFA d'Afrique centrale
curl -X POST http://localhost:8080/api/currency/convert \
  -H "Content-Type: application/json" \
  -d '{"from":"USD","to":"XAF","amount":100}'

# Franc CFA de l'Afrique de l'Ouest → Yuan chinois
curl -X POST http://localhost:8080/api/currency/convert \
  -H "Content-Type: application/json" \
  -d '{"from":"XOF","to":"CNY","amount":10000}'

# Yuan chinois → Franc CFA d'Afrique centrale
curl -X POST http://localhost:8080/api/currency/convert \
  -H "Content-Type: application/json" \
  -d '{"from":"CNY","to":"XAF","amount":500}'

# Livre sterling → Franc CFA de l'Afrique de l'Ouest
curl -X POST http://localhost:8080/api/currency/convert \
  -H "Content-Type: application/json" \
  -d '{"from":"GBP","to":"XOF","amount":100}'

# Yen japonais → Euro
curl -X POST http://localhost:8080/api/currency/convert \
  -H "Content-Type: application/json" \
  -d '{"from":"JPY","to":"EUR","amount":10000}'

# Franc suisse → Franc CFA de l'Afrique de l'Ouest
curl -X POST http://localhost:8080/api/currency/convert \
  -H "Content-Type: application/json" \
  -d '{"from":"CHF","to":"XOF","amount":300}'
```

Réponse type (USD → XAF) :

```json
{
  "from": "USD",
  "to": "XAF",
  "amount": 100,
  "exchangeRate": 564.2916,
  "convertedAmount": 56429.16,
  "timestamp": "2026-09-09T12:00:00Z"
}
```

## Endpoint principal

```
POST /api/currency/convert
Content-Type: application/json
```

Requête :

```json
{
  "from": "USD",
  "to": "EUR",
  "amount": 100
}
```

Réponse (200) :

```json
{
  "from": "USD",
  "to": "EUR",
  "amount": 100,
  "exchangeRate": 0.9234,
  "convertedAmount": 92.34,
  "timestamp": "2026-09-09T12:00:00Z"
}
```

- Le taux exact est conservé pendant le calcul ; il est arrondi à 4 décimales pour l'affichage, le montant converti à 2 décimales (`HALF_UP`).
- Codes normalisés automatiquement : `usd` → `USD`.
- Même devise (`from == to`) : taux = 1, **aucun appel externe** (optimisation documentée).

Endpoint secondaire :

```
GET /api/currency/currencies        # liste des devises disponibles (dynamique)
GET /api/currency/rate?from=USD&to=EUR
```

## Gestion des erreurs

| Code | Situation |
|---|---|
| `400` | Montant absent, ≤ 0, devise absente/malformée ou non supportée par le fournisseur |
| `502` | Fournisseur indisponible, timeout, erreur HTTP du fournisseur (401/403/404/429/5xx), réponse invalide |
| `500` | Erreur serveur imprévue (message générique, aucune stack trace exposée) |

Exemple d'erreur (aucun détail technique exposé) :

```json
{
  "timestamp": "2026-09-09T12:00:00Z",
  "status": 502,
  "error": "BAD_GATEWAY",
  "message": "Exchange rate provider is currently unavailable.",
  "path": "/api/currency/convert"
}
```

## Tests

```bash
./mvnw test
```

51 tests automatisés, **aucune dépendance Internet** :
- `CurrencyConversionServiceTest` : logique de conversion + liste des devises (18 cas)
- `ExchangeRateClientTest` : HTTP 200/401/404/429/500, JSON malformé, timeout, connexion refusée, liste des devises via MockWebServer (14 cas)
- `CurrencyControllerTest` : codes HTTP 200/400/502/500 (12 cas)
- `CurrencyConversionIntegrationTest` : bout en bout avec client mocké + OpenAPI (7 cas)

### Tester depuis Swagger

1. Ouvrir http://localhost:8080/swagger-ui/index.html
2. Déplier `POST /api/currency/convert` → *Try it out*
3. Saisir `{"from":"USD","to":"EUR","amount":100}` → *Execute* → vérifier le 200 et le corps de réponse.
4. Tester les cas d'erreur : `amount: 0` (400), devise `XXX` (400), puis couper le réseau ou définir `EXCHANGE_RATE_BASE_URL=http://localhost:9` et relancer pour observer le 502.

### Tester en ligne de commande

```bash
curl -X POST http://localhost:8080/api/currency/convert \
  -H "Content-Type: application/json" \
  -d '{"from":"USD","to":"EUR","amount":100}'
```
