# API de Conversion de Devises

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

36 tests automatisés, **aucune dépendance Internet** :
- `CurrencyConversionServiceTest` : logique de conversion (12 cas)
- `ExchangeRateClientTest` : HTTP 200/401/404/429/500, JSON malformé, timeout, connexion refusée via MockWebServer (11 cas)
- `CurrencyControllerTest` : codes HTTP 200/400/502/500 (10 cas)
- `CurrencyConversionIntegrationTest` : bout en bout avec client mocké + OpenAPI (3 cas)

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
