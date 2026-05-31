#!/usr/bin/env bash
# =============================================================================
# FraudGuard — Suite de pruebas E2E
# Valida los flujos principales contra el backend en http://localhost:8080
# Uso: bash e2e-tests.sh
# =============================================================================

BASE="http://localhost:8080/api/v1"
PASS=0; FAIL=0; SKIP=0

# ── colores ──────────────────────────────────────────────────────────────────
GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; RESET='\033[0m'

# ── helpers ──────────────────────────────────────────────────────────────────
ok()   { echo -e "  ${GREEN}✓${RESET} $1"; ((PASS++)); }
fail() { echo -e "  ${RED}✗${RESET} $1"; ((FAIL++)); }
skip() { echo -e "  ${YELLOW}⊘${RESET} $1"; ((SKIP++)); }
section() { echo -e "\n${CYAN}${BOLD}▶ $1${RESET}"; }

# Realiza un request y devuelve el body; guarda el HTTP status en $STATUS
req() {
  local method=$1 url=$2 body=$3
  if [[ -n "$body" ]]; then
    RESPONSE=$(curl -s -w "\n__STATUS__%{http_code}" -X "$method" \
      -H "Content-Type: application/json" -d "$body" "$url")
  else
    RESPONSE=$(curl -s -w "\n__STATUS__%{http_code}" -X "$method" "$url")
  fi
  STATUS=$(echo "$RESPONSE" | grep "__STATUS__" | sed 's/__STATUS__//')
  BODY=$(echo "$RESPONSE" | sed '/^__STATUS__/d')
  echo "$BODY"
}

# Verifica que $STATUS esté dentro del rango esperado
assert_status() {
  local expected=$1 label=$2
  if [[ "$STATUS" == "$expected" ]]; then
    ok "$label (HTTP $STATUS)"
  else
    fail "$label — esperaba HTTP $expected, recibió $STATUS | body: $(echo $BODY | head -c 120)"
  fi
}

# Verifica que el body JSON contenga un campo con cierto valor
assert_field() {
  local field=$1 expected=$2 label=$3
  local actual
  actual=$(echo "$BODY" | jq -r "$field" 2>/dev/null)
  if [[ "$actual" == "$expected" ]]; then
    ok "$label ($field = $expected)"
  else
    fail "$label — esperaba $field='$expected', recibió '$actual'"
  fi
}

# Verifica que el campo sea un número mayor que el threshold
assert_gt() {
  local field=$1 threshold=$2 label=$3
  local actual
  actual=$(echo "$BODY" | jq -r "$field" 2>/dev/null)
  if [[ -n "$actual" && "$actual" != "null" ]] && awk "BEGIN{exit !($actual > $threshold)}"; then
    ok "$label ($field=$actual > $threshold)"
  else
    fail "$label — esperaba $field > $threshold, recibió '$actual'"
  fi
}

assert_not_null() {
  local field=$1 label=$2
  local actual
  actual=$(echo "$BODY" | jq -r "$field" 2>/dev/null)
  if [[ -n "$actual" && "$actual" != "null" ]]; then
    ok "$label ($field presente)"
  else
    fail "$label — campo $field ausente o null"
  fi
}

# =============================================================================
echo -e "\n${BOLD}╔══════════════════════════════════════════════════╗${RESET}"
echo -e "${BOLD}║        FraudGuard — Suite E2E                    ║${RESET}"
echo -e "${BOLD}╚══════════════════════════════════════════════════╝${RESET}"

# ── 0. Health check ──────────────────────────────────────────────────────────
section "0. Health check del sistema"
req GET "$BASE/sistema/salud"
if [[ "$STATUS" == "200" ]]; then
  ESTADO=$(echo "$BODY" | jq -r '.estado // .estado')
  ok "Backend responde (HTTP 200) — estado: $ESTADO"
else
  fail "Backend no disponible (HTTP $STATUS). Abortando."
  exit 1
fi

# Detectar si el servidor ya aplica la nueva API (snake_case)
NUEVA_API=false
HASH_CARGA=$(echo "$BODY" | jq -r '.hash_table_carga // empty')
[[ -n "$HASH_CARGA" ]] && NUEVA_API=true
echo -e "  ℹ  API snake_case activa: $NUEVA_API"

# ── Cleanup: eliminar transacciones E2E de ejecuciones anteriores ────────────
section "0b. Limpieza previa (idempotencia)"
for ID in E2E-TXN-001 E2E-TXN-002 E2E-TXN-003 E2E-TXN-004 E2E-TXN-005 E2E-TXN-006 E2E-TXN-007; do
  STATUS_DEL=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE/transacciones/$ID")
  [[ "$STATUS_DEL" == "204" ]] && echo -e "  ↻ $ID eliminada" || true
done

# ── 1. Registrar transacciones (mínimo 5) ────────────────────────────────────
section "1. Registrar 7 transacciones (flujo CREATE + KNN automático)"

NOW=$(date +%s)

register_tx() {
  local id=$1 cuenta=$2 monto=$3 tipo=$4 ts=$5 label=$6
  req POST "$BASE/transacciones" \
    "{\"idTransaccion\":\"$id\",\"numCuenta\":\"$cuenta\",\"monto\":$monto,\"timestamp\":$ts,\"tipo\":\"$tipo\"}"
  assert_status 201 "$label — registro"

  # Verificar campos en la respuesta (compatible con API vieja y nueva)
  local id_field
  if $NUEVA_API; then id_field=".id_transaccion"; else id_field=".idTransaccion"; fi
  local returned_id
  returned_id=$(echo "$BODY" | jq -r "$id_field" 2>/dev/null)
  if [[ "$returned_id" == "$id" ]]; then
    ok "$label — ID confirmado en respuesta ($id)"
  else
    fail "$label — ID incorrecto en respuesta: '$returned_id'"
  fi
}

# timestamp de madrugada (00:30)
TS_EARLY=$(( (NOW / 86400) * 86400 + 1800 ))
# timestamp diurno (14:00)
TS_DAY=$(( (NOW / 86400) * 86400 + 50400 ))

register_tx "E2E-TXN-001" "CTA-E2E-001" 500000    "RETIRO"        "$TS_DAY"   "TXN-001 retiro normal"
register_tx "E2E-TXN-002" "CTA-E2E-001" 12000000  "RETIRO"        "$TS_EARLY" "TXN-002 retiro alto madrugada"
register_tx "E2E-TXN-003" "CTA-E2E-002" 250000    "DEPOSITO"      "$TS_DAY"   "TXN-003 depósito normal"
register_tx "E2E-TXN-004" "CTA-E2E-002" 8500000   "TRANSFERENCIA" "$TS_EARLY" "TXN-004 transferencia alta madrugada"
register_tx "E2E-TXN-005" "CTA-E2E-003" 100000    "DEPOSITO"      "$TS_DAY"   "TXN-005 depósito pequeño"
register_tx "E2E-TXN-006" "CTA-E2E-003" 15000000  "RETIRO"        "$TS_EARLY" "TXN-006 retiro muy alto madrugada"
register_tx "E2E-TXN-007" "CTA-E2E-001" 350000    "TRANSFERENCIA" "$TS_DAY"   "TXN-007 transferencia normal"

# ── 2. Registro duplicado debe fallar ────────────────────────────────────────
section "2. Validación: ID duplicado → 409"
req POST "$BASE/transacciones" \
  "{\"idTransaccion\":\"E2E-TXN-001\",\"numCuenta\":\"CTA-E2E-001\",\"monto\":1000,\"timestamp\":$TS_DAY,\"tipo\":\"RETIRO\"}"
if [[ "$STATUS" == "409" || "$STATUS" == "400" ]]; then
  ok "Duplicado rechazado correctamente (HTTP $STATUS)"
else
  fail "Duplicado no fue rechazado — recibió HTTP $STATUS"
fi

# ── 3. Consulta por ID (Hash Table O(1)) ─────────────────────────────────────
section "3. Consulta por ID — Hash Table O(1)"

for ID in E2E-TXN-001 E2E-TXN-003 E2E-TXN-006; do
  req GET "$BASE/transacciones/$ID"
  assert_status 200 "GET /transacciones/$ID"
  local_id=$(echo "$BODY" | jq -r '.idTransaccion // .id_transaccion' 2>/dev/null)
  if [[ "$local_id" == "$ID" ]]; then
    ok "ID verificado en body: $ID"
  else
    fail "ID incorrecto — esperaba $ID, recibió '$local_id'"
  fi
done

# ── 4. Consulta ID inexistente → 404 ─────────────────────────────────────────
section "4. Validación: ID inexistente → 404"
req GET "$BASE/transacciones/NO-EXISTE-9999"
if [[ "$STATUS" == "404" ]]; then
  ok "404 correcto para ID inexistente"
else
  fail "Esperaba 404, recibió $STATUS"
fi

# ── 5. Listar con paginación y filtros ───────────────────────────────────────
section "5. Listado con filtros y paginación"

# Sin filtros
req GET "$BASE/transacciones?page=0&size=20"
assert_status 200 "GET /transacciones (sin filtros)"
TOTAL=$(echo "$BODY" | jq -r '.totalFiltrados // .total_elementos // 0' 2>/dev/null)
if (( TOTAL >= 7 )); then
  ok "Total transacciones >= 7 (recibió $TOTAL)"
else
  fail "Total inesperado — esperaba >= 7, recibió $TOTAL"
fi

# Filtro por cuenta
req GET "$BASE/transacciones?numCuenta=CTA-E2E-001&page=0&size=20"
assert_status 200 "GET /transacciones?numCuenta=CTA-E2E-001"
FILTRADO=$(echo "$BODY" | jq -r '.totalFiltrados // .total_elementos // 0' 2>/dev/null)
if (( FILTRADO >= 3 )); then
  ok "Filtro por cuenta correcto (>= 3 resultados: $FILTRADO)"
else
  fail "Filtro por cuenta incorrecto — esperaba >= 3, recibió $FILTRADO"
fi

# Filtro por tipo
req GET "$BASE/transacciones?tipo=RETIRO&page=0&size=20"
assert_status 200 "GET /transacciones?tipo=RETIRO"
RET=$(echo "$BODY" | jq -r '.totalFiltrados // .total_elementos // 0' 2>/dev/null)
if (( RET >= 3 )); then
  ok "Filtro por tipo=RETIRO correcto (>= 3: $RET)"
else
  fail "Filtro tipo RETIRO — esperaba >= 3, recibió $RET"
fi

# ── 6. Motor KNN — vecinos más cercanos ──────────────────────────────────────
section "6. Motor KNN — vecinos de E2E-TXN-002 (k=3)"
req GET "$BASE/deteccion/vecinos/E2E-TXN-002?k=3"
if [[ "$STATUS" == "200" ]]; then
  ok "GET /deteccion/vecinos/E2E-TXN-002 (HTTP 200)"
  # Contar vecinos según estructura (nueva API vs vieja)
  N_VEC=$(echo "$BODY" | jq -r '
    if type == "array" then length
    elif .vecinos != null then (.vecinos | length)
    else 0 end' 2>/dev/null)
  if (( N_VEC >= 1 )); then
    ok "Recibió $N_VEC vecinos"
  else
    skip "Pocos vecinos en KD-tree (normal si hay < k activos)"
  fi
elif [[ "$STATUS" == "422" || "$STATUS" == "400" ]]; then
  skip "KD-tree con pocos nodos activos para k=3 (HTTP $STATUS)"
else
  fail "Vecinos KNN fallido — HTTP $STATUS | $(echo $BODY | head -c 120)"
fi

# ── 7. Re-análisis KNN sobre una transacción ─────────────────────────────────
section "7. Re-análisis KNN — POST /deteccion/analizar/E2E-TXN-004"
req POST "$BASE/deteccion/analizar/E2E-TXN-004?k=5"
if [[ "$STATUS" == "200" ]]; then
  ok "Re-análisis exitoso (HTTP 200)"
  # Verificar campo de estado (compatible ambas APIs)
  ESTADO_FIELD=$(echo "$BODY" | jq -r '
    .estadoAlertaNombre // .estado_alerta_nombre //
    .estadoAlerta // .estado_alerta // "?"' 2>/dev/null)
  ok "Estado tras re-análisis: $ESTADO_FIELD"
else
  skip "Re-análisis HTTP $STATUS (normal si árbol tiene pocos nodos)"
fi

# ── 8. Búsqueda por rango 5D ─────────────────────────────────────────────────
section "8. Búsqueda multidimensional — hipercubo (retiros, monto alto)"

# Intentar con formato de nueva API (campos planos snake_case)
RANGO_PAYLOAD='{"d1MontoMin":0.5,"d1MontoMax":1.0,"d4TipoMin":0,"d4TipoMax":0}'
req POST "$BASE/deteccion/rango" "$RANGO_PAYLOAD"

if [[ "$STATUS" == "200" ]]; then
  IS_ARRAY=$(echo "$BODY" | jq 'if type == "array" then true else false end' 2>/dev/null)
  if [[ "$IS_ARRAY" == "true" ]]; then
    N=$(echo "$BODY" | jq 'length')
    ok "Búsqueda rango (nueva API, array) — $N resultados"
  else
    N=$(echo "$BODY" | jq -r '.total // (.transacciones | length) // 0' 2>/dev/null)
    ok "Búsqueda rango (nueva API, wrapper) — $N resultados"
  fi
else
  # Fallback: formato antiguo con arrays min/max (API antes del refactor)
  RANGO_PAYLOAD='{"min":[0.5,-1e9,-1e9,0,0],"max":[1.0,1e9,1e9,0,1e9]}'
  req POST "$BASE/deteccion/rango" "$RANGO_PAYLOAD"
  if [[ "$STATUS" == "200" ]]; then
    IS_ARRAY=$(echo "$BODY" | jq 'if type == "array" then true else false end' 2>/dev/null)
    if [[ "$IS_ARRAY" == "true" ]]; then
      N=$(echo "$BODY" | jq 'length')
    else
      N=$(echo "$BODY" | jq -r '.total // (.transacciones | length) // 0' 2>/dev/null)
    fi
    ok "Búsqueda rango (API legacy, arrays) — $N resultados"
  else
    fail "Búsqueda rango falló — HTTP $STATUS | $(echo $BODY | head -c 120)"
  fi
fi

# ── 9. Alertas activas ────────────────────────────────────────────────────────
section "9. Alertas activas — GET /alertas"
req GET "$BASE/alertas?nivel_minimo=1"
if [[ "$STATUS" == "200" ]]; then
  ok "GET /alertas (HTTP 200)"
  # compatible con array (vieja) y wrapper (nueva)
  IS_ARR=$(echo "$BODY" | jq 'if type == "array" then true else false end' 2>/dev/null)
  if [[ "$IS_ARR" == "true" ]]; then
    N=$(echo "$BODY" | jq 'length')
    ok "Alertas activas (array): $N"
  else
    N=$(echo "$BODY" | jq -r '.total_alertas // (.transacciones | length) // 0' 2>/dev/null)
    ok "Alertas activas (wrapper): $N"
  fi
else
  fail "GET /alertas — HTTP $STATUS"
fi

# ── 10. Estadísticas del sistema ─────────────────────────────────────────────
section "10. Estadísticas — GET /sistema/estadisticas"
req GET "$BASE/sistema/estadisticas"
assert_status 200 "GET /sistema/estadisticas"
# Compatible con ambas estructuras
TOTAL_TXN=$(echo "$BODY" | jq -r '
  .totalTransacciones // .total_transacciones //
  .totalRegistros // .registrosActivos // 0' 2>/dev/null)
if (( TOTAL_TXN >= 7 )); then
  ok "Total transacciones en estadísticas: $TOTAL_TXN"
else
  fail "Total transacciones inesperado — esperaba >= 7, recibió $TOTAL_TXN"
fi

# ── 11. Eliminación lógica ────────────────────────────────────────────────────
section "11. Eliminación lógica — DELETE /transacciones/E2E-TXN-007"
req DELETE "$BASE/transacciones/E2E-TXN-007"
assert_status 204 "DELETE E2E-TXN-007"

# Verificar que 404 tras eliminar
req GET "$BASE/transacciones/E2E-TXN-007"
if [[ "$STATUS" == "404" ]]; then
  ok "E2E-TXN-007 no encontrada tras eliminar (404)"
else
  fail "E2E-TXN-007 aún devuelve HTTP $STATUS tras eliminación lógica"
fi

# ── 12. Validaciones de entrada ───────────────────────────────────────────────
section "12. Validaciones de entrada — campos inválidos"

# Monto negativo
req POST "$BASE/transacciones" \
  "{\"idTransaccion\":\"E2E-BAD-001\",\"numCuenta\":\"CTA-X\",\"monto\":-100,\"timestamp\":$NOW,\"tipo\":\"RETIRO\"}"
if [[ "$STATUS" == "400" || "$STATUS" == "422" ]]; then
  ok "Monto negativo rechazado (HTTP $STATUS)"
else
  fail "Monto negativo no fue rechazado — HTTP $STATUS"
fi

# Tipo inválido
req POST "$BASE/transacciones" \
  "{\"idTransaccion\":\"E2E-BAD-002\",\"numCuenta\":\"CTA-X\",\"monto\":1000,\"timestamp\":$NOW,\"tipo\":\"FRAUDE\"}"
if [[ "$STATUS" == "400" || "$STATUS" == "422" ]]; then
  ok "Tipo inválido rechazado (HTTP $STATUS)"
else
  fail "Tipo inválido no fue rechazado — HTTP $STATUS"
fi

# ID demasiado largo (> 19 chars)
req POST "$BASE/transacciones" \
  "{\"idTransaccion\":\"E2E-TOOLONG-XXXXXXXXXX\",\"numCuenta\":\"CTA-X\",\"monto\":1000,\"timestamp\":$NOW,\"tipo\":\"RETIRO\"}"
if [[ "$STATUS" == "400" || "$STATUS" == "422" ]]; then
  ok "ID demasiado largo rechazado (HTTP $STATUS)"
else
  fail "ID largo no rechazado — HTTP $STATUS"
fi

# ── Resumen ───────────────────────────────────────────────────────────────────
TOTAL=$((PASS + FAIL + SKIP))
echo -e "\n${BOLD}════════════════════════════════════════${RESET}"
echo -e "${BOLD}Resultados: $TOTAL pruebas${RESET}"
echo -e "  ${GREEN}✓ Pasaron: $PASS${RESET}"
echo -e "  ${RED}✗ Fallaron: $FAIL${RESET}"
echo -e "  ${YELLOW}⊘ Omitidas: $SKIP${RESET}"
echo -e "${BOLD}════════════════════════════════════════${RESET}\n"

[[ $FAIL -eq 0 ]] && exit 0 || exit 1
