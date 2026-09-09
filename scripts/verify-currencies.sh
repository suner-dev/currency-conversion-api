#!/usr/bin/env bash
# =============================================================================
# Verification de la couverture des devises de l'API de conversion.
#
# Usage :
#   ./scripts/verify-currencies.sh [BASE_URL]              # matrice des monnaies cles
#   ./scripts/verify-currencies.sh [BASE_URL] --all [CODE] # TOUTES les devises du fournisseur
#
# Exemples :
#   ./scripts/verify-currencies.sh http://localhost:8080
#   ./scripts/verify-currencies.sh http://localhost:8080 --all XAF
#
# NB : le mode --all interroge le fournisseur une fois pour obtenir la liste
# complete des devises, puis verifie CHAQUE devise via l'API (GET /rate).
# Aucune liste n'est hardcodee : la couverture provient du fournisseur lui-meme.
# =============================================================================
set -u

BASE_URL="${1:-http://localhost:8080}"
MODE="${2:-matrix}"
ALL_FROM="${3:-XAF}"

check_rate() {
  local from="$1" to="$2"
  local http_code body
  body=$(curl -s -w '\n%{http_code}' "${BASE_URL}/api/currency/rate?from=${from}&to=${to}")
  http_code=$(printf '%s' "$body" | tail -n1)
  local rate
  rate=$(printf '%s' "$body" | head -n -1 | python3 -c 'import json,sys; print(json.load(sys.stdin).get("exchangeRate","?"))' 2>/dev/null || echo '?')
  if [ "$http_code" = "200" ]; then
    printf '  OK   %-4s -> %-4s taux=%s\n' "$from" "$to" "$rate"
    return 0
  fi
  printf '  FAIL %-4s -> %-4s HTTP=%s\n' "$from" "$to" "$http_code"
  return 1
}

fail=0

case "${MODE#--}" in
  matrix)
    echo "=== Matrice des monnaies cles via ${BASE_URL} ==="
    KEY_CURRENCIES="XAF XOF CNY EUR USD GBP JPY CHF"
    for to in $KEY_CURRENCIES; do
      check_rate XAF "$to" || fail=$((fail+1))
    done
    for from in $KEY_CURRENCIES; do
      [ "$from" = "XAF" ] && continue
      check_rate "$from" XAF || fail=$((fail+1))
    done
    echo "---"
    if [ "$fail" -eq 0 ]; then echo "RESULTAT : toutes les conversions cles sont OK."; else echo "RESULTAT : ${fail} echec(s)."; exit 1; fi
    ;;

  all)
    echo "=== Verification de TOUTES les devises du fournisseur (base ${ALL_FROM}) ==="
    codes=$(curl -s "https://open.er-api.com/v6/latest/USD" | python3 -c 'import json,sys; print(" ".join(json.load(sys.stdin)["rates"].keys()))')
    if [ -z "$codes" ]; then echo "Impossible de recuperer la liste des devises du fournisseur."; exit 1; fi
    total=0; ok=0
    for code in $codes; do
      total=$((total+1))
      if check_rate "$ALL_FROM" "$code" > /dev/null 2>&1; then ok=$((ok+1)); else
        printf '  FAIL %s -> %s\n' "$ALL_FROM" "$code"; fail=$((fail+1))
      fi
      sleep 0.1
    done
    echo "---"
    echo "RESULTAT : ${ok}/${total} devises convertibles depuis ${ALL_FROM}."
    [ "$fail" -eq 0 ] || exit 1
    ;;

  *)
    echo "Mode inconnu : ${MODE}. Utiliser 'matrix' ou 'all'."; exit 1 ;;
esac
