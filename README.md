# ☕ Cafetín — Gestión de fiados

![Android](https://img.shields.io/badge/Android-API%2023%2B-brightgreen?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blueviolet?logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-blue?logo=jetpackcompose)
![Room](https://img.shields.io/badge/Room-SQLite-orange)
![License](https://img.shields.io/badge/Licencia-MIT-lightgrey)

App Android para gestionar fiados en una cafetería escolar. Reemplaza el cuaderno y el bolígrafo: registra deudas, cobra pagos y exporta reportes en PDF — todo sin internet.

> **Impacto real:** el negocio pasó de llevar las cuentas a mano (varias horas al día) a registrar cada fiado en segundos.

---

## 📸 Capturas de pantalla

| Clientes | Detalle | Historial | Catálogo |
|----------|---------|-----------|----------|
|![Clientes](screenshots/personas.png) |![Detalle](screenshots/detalle.png) |![Historial](screenshots/historial.png) |![Catalogo](screenshots/catalogo.png) |

---

## ✨ Funcionalidades

- **Registro rápido de fiados** por categorías (bebidas, galletas, snacks, etc.) con un solo toque
- **Acumulación inteligente** — si el mismo producto se fía dos veces en el día, se agrupa automáticamente (`Galleta chica x2`)
- **Cobro de pagos** — total o parcial, con confirmación
- **Búsqueda fuzzy** — encuentra clientes aunque escribas con errores de ortografía o sin acentos
- **Historial por día** — navega día a día para ver todos los movimientos
- **Exportar PDF** — reporte detallado por cliente y rango de fechas
- **Compartir por WhatsApp** u otras apps directamente desde la app
- **Catálogo editable** — administra categorías y productos directamente desde la app, sin tocar el código
- **100% offline** — no requiere internet, los datos viven en el dispositivo

---

## 🛠️ Stack tecnológico

| Capa | Tecnología |
|------|------------|
| UI | Jetpack Compose + Material 3 |
| Navegación | Navigation Compose |
| Estado | StateFlow + Coroutines |
| Base de datos | Room (SQLite) |
| Arquitectura | MVVM + Repository pattern |
| DI | Manual (AppContainer) |
| PDF | Android PdfDocument API |
| Compartir archivos | FileProvider |

---

## 🏗️ Arquitectura

El proyecto sigue una arquitectura en capas inspirada en Clean Architecture:

```
com.proyecto.cafetin/
├── data/
│   ├── catalog/        # Catálogo estático de productos (seed inicial)
│   ├── db/             # Room: AppDatabase, DAOs, Converters, Migrations
│   └── model/          # Entidades: Persona, Movimiento, Producto,
│                       #            CatalogoCategoria, CatalogoProducto
├── di/                 # Inyección de dependencias manual (AppContainer)
├── domain/
│   └── usecase/        # AcumularProductoUseCase, ExportarPdfUseCase
├── navigation/         # NavGraph + Routes (constantes de rutas)
├── repository/         # ICafetinRepository + ICatalogoRepository + CafetinRepository
├── ui/
│   ├── catalogo/       # Pantalla de administración del catálogo (nueva)
│   ├── detalle/        # Pantalla de detalle por cliente
│   ├── historial/      # Pantalla de historial diario
│   ├── personas/       # Pantalla principal (lista de clientes)
│   └── theme/          # Colores, tipografía, tema
└── util/               # DateUtils, MoneyUtils, NotaUtils, PdfExporter, SearchUtils
```

**Flujo de datos:**

```
UI (Compose) → ViewModel → UseCase / Repository → Room DAO → SQLite
                    ↑                                              |
              StateFlow / Channel ←───────────────────────────────┘
```

Los ViewModels nunca conocen `Intent` ni `Context` de Activity. Los eventos de UI (como compartir el PDF) se emiten como `sealed class` y la pantalla los maneja.

---

## 🚀 Cómo compilar

**Requisitos:**
- Android Studio Hedgehog o superior
- JDK 11
- Android SDK 35

**Pasos:**

```bash
# 1. Clona el repositorio
git clone https://github.com/tu-usuario/App_Cafetin.git

# 2. Abre el proyecto en Android Studio
# File → Open → selecciona la carpeta App_Cafetin

# 3. Sincroniza Gradle y ejecuta en un emulador o dispositivo físico
```

No se requiere ninguna API key ni configuración adicional. La app funciona completamente offline.

---

## 🗄️ Base de datos

La app usa **Room** con cuatro entidades:

- `Persona` — cliente registrado (nombre, descripción, estado de envío)
- `Movimiento` — fiado o pago asociado a una persona (monto, nota, fecha, tipo)
- `CatalogoCategoria` — categoría editable del catálogo (nombre, emoji, orden)
- `CatalogoProducto` — producto editable dentro de una categoría (nombre, precio en centavos, orden)

La base de datos está en versión `3`. Se incluyen migraciones explícitas para preservar los datos al actualizar:

| Migración | Cambio |
|-----------|--------|
| `MIGRATION_1_2` | Agrega columna `enviadoHasta` a la tabla `personas` |
| `MIGRATION_2_3` | Crea las tablas `catalogo_categorias` y `catalogo_productos`, y las pre-puebla con el catálogo estático que antes estaba hardcodeado en `ProductosCatalogo` |

---

## 📦 Catálogo editable

Antes de esta versión, el catálogo de productos era un objeto Kotlin estático (`ProductosCatalogo`). Ahora el catálogo vive en Room y se puede administrar desde la propia app.

**Qué puede hacer el usuario desde la pantalla de catálogo:**

- Crear, renombrar y eliminar **categorías** (con emoji personalizable)
- Agregar, editar y eliminar **productos** dentro de cada categoría
- Ver los cambios reflejados de inmediato en la pantalla de detalle al registrar fiados

**Acceso:** pantalla Detalle → botón *Gestionar catálogo* → `CatalogoScreen`

La migración `2→3` carga automáticamente el catálogo estático existente como datos iniciales, por lo que los usuarios que actualicen la app no pierden ningún producto.

---

## 🔍 Búsqueda fuzzy

`SearchUtils` implementa un motor de búsqueda tolerante a errores con 5 capas en orden de costo:

1. **Contains directo** sin acentos — `"mar"` → `"María"` ✓
2. **Prefijo por token** — `"rob"` → `"Roberto"` ✓
3. **Subsecuencia ordenada** — `"mra"` → `"Maria"` ✓
4. **Similitud por bigramas** (coeficiente Dice ≥ 40%) — `"juen"` → `"Juan"` ✓
5. **Distancia de Levenshtein** con umbral adaptativo — `"robeto"` → `"Roberto"` ✓

---
