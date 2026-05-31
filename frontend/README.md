# FraudGuard UI

React + Vite frontend para el sistema de detección de fraude bancario con KD-Tree + KNN.

## Requisitos

- Node.js 18+
- Backend corriendo en `http://localhost:8080/api/v1`

## Instalación y arranque

```bash
npm install
npm run dev
```

La app abre en `http://localhost:5173`. Las llamadas a `/api/v1/*` se proxean automáticamente al backend.

## Módulos

| Ruta | Pantalla | Operación |
|------|----------|-----------|
| `/dashboard` | Dashboard con KPIs y gráficas | Estadísticas + Escaneo masivo |
| `/registrar` | Formulario de registro | POST /transacciones (CREATE) |
| `/alertas` | Lista de alertas MEDIA/ALTA | PATCH confirmar / descartar |
| `/knn` | Motor KNN — vecinos R⁵ | GET vecinos + POST analizar |
| `/consultas` | Búsqueda por hipercubo 5D | POST /deteccion/rango |
| `/buscar` | Consulta + lista con filtros | GET por ID + DELETE lógico |
| `/estado` | Diagnóstico KD-Tree/Hash/Disco | GET salud + estadísticas |

## Build para producción

```bash
npm run build
npm run preview
```
