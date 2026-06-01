# FraudGuard — Sistema de Detección de Fraude Bancario

Proyecto integrador de la materia **Estructuras de Datos e Indexación en Disco** — Universidad de Antioquia, 2026-1.

El sistema detecta transacciones bancarias sospechosas usando estructuras de datos implementadas desde cero: una **tabla hash con encadenamiento** para acceso O(1) por ID y un **KD-tree 5D** para búsqueda de similitud multidimensional (KNN). La persistencia se maneja directamente sobre un archivo binario de registros de tamaño fijo, sin ninguna base de datos externa.

---

## Tecnologías

### Backend
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.6-6DB33F?style=flat&logo=springboot&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?style=flat&logo=apachemaven&logoColor=white)
![Lombok](https://img.shields.io/badge/Lombok-latest-BC4521?style=flat&logoColor=white)
![SpringDoc](https://img.shields.io/badge/SpringDoc_OpenAPI-2.8.6-85EA2D?style=flat&logo=swagger&logoColor=black)

### Frontend
![React](https://img.shields.io/badge/React-18-61DAFB?style=flat&logo=react&logoColor=black)
![Vite](https://img.shields.io/badge/Vite-5-646CFF?style=flat&logo=vite&logoColor=white)
![Recharts](https://img.shields.io/badge/Recharts-2.12-22B5BF?style=flat&logoColor=white)

### Restricción del proyecto
![Sin BD](https://img.shields.io/badge/Sin_base_de_datos-archivo_binario_propio-red?style=flat)
![Estructuras propias](https://img.shields.io/badge/Estructuras-implementación_propia-blue?style=flat)

---

## Qué hace el sistema

El sistema expone una API REST que permite gestionar transacciones bancarias y clasificar automáticamente su riesgo de fraude.

Al insertar una transacción se calculan **5 dimensiones métricas**:

| Dimensión | Descripción |
|-----------|-------------|
| D1 | Monto normalizado (0–1) |
| D2 | Hora del día extraída del timestamp |
| D3 | Frecuencia de transacciones de la cuenta en las últimas 24 h |
| D4 | Tipo codificado (RETIRO=0.0 / DEPOSITO=0.5 / TRANSFERENCIA=1.0) |
| D5 | Desviación del monto respecto al promedio histórico de la cuenta |

Con esas dimensiones se ejecuta **KNN (k=5) sobre el KD-tree** antes de guardar el registro, y se asigna un nivel de alerta:

| Vecinos confirmados como fraude | Estado asignado |
|---------------------------------|-----------------|
| ≥ 3 | ALTA |
| ≥ 1 | MEDIA |
| 0 | NORMAL |

Un analista puede luego confirmar o descartar la alerta (`CONFIRMADO_FRAUDE` / `FALSO_POSITIVO`), y eso retroalimenta el modelo para futuras clasificaciones.

---

## Arquitectura

```
React (puerto 5173)
        ↕ HTTP / JSON
Spring Boot — API REST (puerto 8080)
        ↕
    Capa de servicio
        ↕
┌──────────────────┬────────────────────┐
│   Hash Table     │     KD-tree 5D     │
│  ID → offset     │  KNN / rango 5D   │
│  acceso O(1)     │  poda de ramas     │
└────────┬─────────┴────────┬───────────┘
         └────────┬──────────┘
            FileManager
        (RandomAccessFile)
              ↕ 128 bytes / registro
         transacciones.dat
```

**Spring Boot se usa únicamente como servidor HTTP** (`@RestController`, rutas, Tomcat embebido). No hay JPA, Hibernate ni ninguna conexión a base de datos.

### Persistencia: registros de 128 bytes fijos

```
[0–19]   id_transaccion   (20 bytes, UTF-8 + relleno \0)
[20–39]  num_cuenta        (20 bytes)
[40–47]  monto             (double, 8 bytes)
[48–55]  timestamp         (long, epoch en segundos)
[56–95]  d1 … d5           (5 doubles, 8 bytes c/u)
[96]     estado_alerta     (1 byte: 0=NORMAL 1=MEDIA 2=ALTA 3=CONFIRMADO 4=FALSO_POS)
[97]     deleted           (1 byte: 0=activo 1=eliminado lógicamente)
[98–127] padding           (30 bytes reservados)
```

Offset de cualquier registro: `num_registro × 128`. Los registros nunca se borran físicamente.

---

## API REST — resumen de endpoints

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/api/v1/transacciones` | Crear transacción + clasificación KNN automática |
| `GET` | `/api/v1/transacciones` | Listar con filtros (cuenta, tipo, estado, paginación) |
| `GET` | `/api/v1/transacciones/{id}` | Buscar por ID via Hash O(1) |
| `PUT` | `/api/v1/transacciones/{id}` | Actualizar y reclasificar |
| `DELETE` | `/api/v1/transacciones/{id}` | Eliminación lógica |
| `POST` | `/api/v1/deteccion/analizar/{id}` | Re-ejecutar KNN sobre una transacción existente |
| `GET` | `/api/v1/deteccion/vecinos/{id}` | Obtener los K vecinos más cercanos |
| `POST` | `/api/v1/deteccion/rango` | Búsqueda por hipercubo 5D |
| `POST` | `/api/v1/deteccion/escaneo-masivo` | KNN en lote sobre todas las NORMAL/MEDIA |
| `GET` | `/api/v1/alertas` | Listar alertas activas (MEDIA + ALTA) |
| `PATCH` | `/api/v1/alertas/{id}/confirmar` | Confirmar fraude |
| `PATCH` | `/api/v1/alertas/{id}/descartar` | Descartar como falso positivo |
| `GET` | `/api/v1/sistema/estadisticas` | Totales, distribución de alertas y métricas |
| `GET` | `/api/v1/sistema/salud` | Health check del archivo y estructuras en memoria |
| `POST` | `/api/v1/sistema/seed` | Cargar 30 transacciones de ejemplo |

La documentación interactiva completa está disponible en `http://localhost:8080/swagger-ui.html` una vez el backend está corriendo.

---

## Cómo correr el proyecto

### Requisitos

- Java 21
- Node.js 18+

### Backend

```bash
cd backend
./mvnw spring-boot:run
# queda escuchando en http://localhost:8080
```

O con el JAR precompilado:

```bash
java -jar backend/executable/fraud-detector-1.0.jar
```

### Frontend

```bash
cd frontend
npm install
npm run dev
# abre http://localhost:5173
```

> El frontend proxea `/api/*` al backend en `localhost:8080` mediante Vite. El backend debe estar corriendo antes de usar el frontend.

### Datos de ejemplo

Con el backend corriendo, ejecuta:

```bash
curl -X POST http://localhost:8080/api/v1/sistema/seed
```

Esto carga 30 transacciones precalculadas: 20 normales, 8 fraudes confirmados y 2 alertas ALTA sin confirmar.

---

## Estructura del repositorio

```
├── backend/
│   ├── src/main/java/co/edu/udea/fraud_detector/
│   │   ├── api/controller/        # Controllers REST
│   │   ├── service/               # Lógica de negocio
│   │   ├── estructura/
│   │   │   ├── hash/              # HashTable, HashBucket, HashEntry
│   │   │   └── kdtree/            # KDTree, KDNode, ResultadoKNN
│   │   ├── persistencia/          # FileManager, RegistroTransaccion, DimensionCalculator
│   │   └── model/                 # DTOs, enums, excepciones
│   └── executable/
│       └── fraud-detector-1.0.jar # JAR ejecutable listo para usar
└── frontend/
      └── src/                     # Aplicación React
```

---

