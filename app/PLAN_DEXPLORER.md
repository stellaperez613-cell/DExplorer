# 📁 DExplorer - Plan del Proyecto

**Gestor de archivos moderno para Android con soporte DeX**

---

## 📋 Índice

1. [Visión General](#visión-general)
2. [Estado Actual](#estado-actual)
3. [Arquitectura](#arquitectura)
4. [Características Implementadas](#características-implementadas)
5. [Características en Desarrollo](#características-en-desarrollo)
6. [Roadmap](#roadmap)
7. [Estructura del Proyecto](#estructura-del-proyecto)
8. [Tecnologías](#tecnologías)
9. [Problemas Conocidos](#problemas-conocidos)
10. [Decisiones de Diseño](#decisiones-de-diseño)

---

## 🎯 Visión General

**DExplorer** es un gestor de archivos nativo para Android construido con Jetpack Compose, diseñado específicamente para funcionar tanto en dispositivos móviles como en Samsung DeX.

### Objetivos del Proyecto

- ✅ Experiencia de usuario similar a Windows Explorer
- ✅ Soporte completo para Samsung DeX (modo escritorio)
- ✅ Rendimiento fluido con archivos grandes
- ✅ Gestión visual de aplicaciones predeterminadas (badges)
- ✅ Miniaturas dinámicas para carpetas
- ✅ Operaciones de archivos modernas (copiar, cortar, pegar, eliminar)

---

## 📊 Estado Actual

### **Fase 6 - Completada (95%)**

#### ✅ Completado
- Sistema de badges para aplicaciones predeterminadas
- Miniaturas dinámicas de carpetas (1-4 imágenes)
- Detección y gestión de aplicaciones por defecto
- Sistema de logging exhaustivo
- Soporte para múltiples tipos de archivo
- Iconos de tipo de archivo coloridos
- Miniaturas para imágenes, videos, audio, PDFs, Office

#### ⚠️ En Progreso
- Pruebas en modo DeX (comportamiento del ciclo de vida)
- Optimización de rendimiento de miniaturas de carpetas
- Corrección de error al cargar icono de Poweramp

#### 🔄 Pendiente
- Sistema de caché para miniaturas de carpetas
- Configuración para habilitar/deshabilitar previsualizaciones
- Documentación de usuario

---

## 🏗️ Arquitectura

### Patrón Arquitectónico: **Clean Architecture + MVVM**

```
┌─────────────────────────────────────────┐
│          UI Layer (Compose)             │
│  ┌───────────┐  ┌──────────────────┐   │
│  │  Screen   │→ │   ViewModel      │   │
│  │Components │  │  (StateFlow)     │   │
│  └───────────┘  └──────────────────┘   │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│         Domain Layer                     │
│  ┌──────────────────────────────────┐   │
│  │      Use Cases                   │   │
│  │  - GetDirectoryContentsUseCase   │   │
│  │  - OpenFileUseCase               │   │
│  │  - CopyFilesUseCase              │   │
│  │  - DeleteFilesUseCase            │   │
│  │  - DefaultAppManager             │   │
│  └──────────────────────────────────┘   │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│          Data Layer                      │
│  ┌──────────────────────────────────┐   │
│  │  File System Access              │   │
│  │  Thumbnail Generation            │   │
│  │  SharedPreferences               │   │
│  └──────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

### Inyección de Dependencias: **Hilt**

---

## ✨ Características Implementadas

### 🗂️ **Navegación de Archivos**
- [x] Navegación por directorios
- [x] Historial de navegación (adelante/atrás)
- [x] Breadcrumb navigation
- [x] Búsqueda de archivos (con debounce)
- [x] Ordenamiento (nombre, tamaño, fecha, tipo)
- [x] Vista de lista y cuadrícula

### 📋 **Operaciones de Archivos**
- [x] Copiar archivos/carpetas
- [x] Cortar/mover archivos/carpetas
- [x] Eliminar archivos/carpetas
- [x] Renombrar archivos/carpetas
- [x] Crear carpetas
- [x] Portapapeles con indicador visual
- [x] Selección múltiple

### 🖼️ **Miniaturas y Previsualizaciones**
- [x] Miniaturas de imágenes (JPG, PNG, WEBP, GIF, BMP)
- [x] Miniaturas de videos (MP4, MKV, AVI, MOV)
- [x] Miniaturas de audio (álbum art)
- [x] Miniaturas de PDFs
- [x] Iconos de documentos Office (Word, Excel, PowerPoint)
- [x] Iconos coloridos por tipo de archivo
- [x] **Miniaturas dinámicas de carpetas** (muestra contenido)
- [x] Sistema de caché para miniaturas

### 📱 **Sistema de Badges (Aplicaciones Predeterminadas)**
- [x] Detección de aplicación predeterminada del sistema
- [x] Tracker interno de badges (evita herencia de categorías)
- [x] Botón "Mostrar badge" en propiedades
- [x] Botón "Limpiar badge" 
- [x] Badges por tipo de archivo específico (MP3 ≠ FLAC)
- [x] Iconos de aplicaciones en badges
- [x] Integración con DefaultAppManager

### 🎨 **Interfaz de Usuario**
- [x] Material Design 3
- [x] Tema oscuro/claro
- [x] Animaciones fluidas
- [x] Context menu (menú contextual)
- [x] Diálogo de propiedades de archivo
- [x] Cálculo de tamaño de carpetas
- [x] Indicadores de permisos (lectura/escritura/ejecución)
- [x] Snackbar para mensajes de error/éxito

### 🖥️ **Soporte Samsung DeX**
- [x] Detección de modo DeX
- [x] Logging de eventos de ciclo de vida
- [x] Tracking de cambios de enfoque de ventana
- [x] Logging de procesos en ejecución

### 🛠️ **Sistema de Logging**
- [x] FileLogger para debugging
- [x] Logs exportables a archivo
- [x] Logging de operaciones de archivo
- [x] Logging de eventos de ciclo de vida
- [x] Logging de detección de aplicaciones predeterminadas

---

## 🚧 Características en Desarrollo

### **Fase 7 - Sistema de Badges (Refinamiento)**

#### Objetivos
1. **Solucionar error de icono de Poweramp**
   - Investigar excepción en `getAppIcon()`
   - Implementar fallback de iconos
   
2. **Mejorar experiencia en DeX**
   - Análisis exhaustivo de logs en modo DeX
   - Determinar viabilidad de auto-tracking de badges
   - Implementar solución manual vs automática

3. **Optimización de miniaturas de carpetas**
   - Implementar caché LRU para previsualizaciones
   - Lazy loading solo para carpetas visibles
   - Configuración para habilitar/deshabilitar

---

## 🗺️ Roadmap

### **Q2 2026**

#### ✅ Fase 6 - Completada
- ✅ Sistema de badges personalizado
- ✅ Miniaturas dinámicas de carpetas
- ✅ Detección de aplicaciones predeterminadas
- ✅ Logging exhaustivo para debugging

#### 🔄 Fase 7 - En Progreso (Actual)
- [ ] Correcciones de bugs críticos
  - [ ] Fix error icono Poweramp
  - [ ] Análisis comportamiento DeX
  - [ ] Decisión: sistema automático vs manual badges
- [ ] Optimizaciones de rendimiento
  - [ ] Caché inteligente de miniaturas
  - [ ] Lazy loading mejorado
- [ ] Pruebas en múltiples dispositivos

#### 📅 Fase 8 - Planificada
- [ ] Operaciones avanzadas de archivos
  - [ ] Comprimir/descomprimir ZIP
  - [ ] Compartir archivos
  - [ ] Enviar a otras apps
- [ ] Gestión de almacenamiento
  - [ ] Análisis de espacio en disco
  - [ ] Limpieza de archivos temporales
  - [ ] Duplicados

#### 📅 Fase 9 - Futura
- [ ] Integración con servicios en la nube
  - [ ] Google Drive
  - [ ] Dropbox
  - [ ] OneDrive
- [ ] Servidor FTP/SMB
- [ ] Transferencia de archivos por WiFi

---

## 📂 Estructura del Proyecto

```
app/src/main/java/com/example/dexplorer/
├── core/
│   └── util/
│       ├── DefaultAppManager.kt          # Gestor de badges personalizado
│       ├── DefaultAppResolver.kt         # Detección de apps predeterminadas
│       ├── FileLogger.kt                 # Sistema de logging
│       ├── ThumbnailGenerator.kt         # Generación de miniaturas
│       └── AppInfo.kt                    # Info de aplicaciones
│
├── data/
│   └── model/
│       ├── FileItem.kt                   # Modelo de archivo/carpeta
│       ├── FilePermissions.kt            # Permisos de archivo
│       ├── ClipboardState.kt             # Estado del portapapeles
│       ├── SortOption.kt                 # Opciones de ordenamiento
│       └── ViewMode.kt                   # Modos de vista
│
├── domain/
│   └── usecase/
│       ├── GetDirectoryContentsUseCase.kt    # Listar archivos
│       ├── OpenFileUseCase.kt                # Abrir archivos
│       ├── CopyFilesUseCase.kt               # Copiar archivos
│       ├── MoveFilesUseCase.kt               # Mover archivos
│       ├── DeleteFilesUseCase.kt             # Eliminar archivos
│       ├── RenameFileUseCase.kt              # Renombrar archivos
│       ├── CreateFolderUseCase.kt            # Crear carpetas
│       ├── SearchFilesUseCase.kt             # Buscar archivos
│       └── CalculateFolderSizeUseCase.kt     # Calcular tamaños
│
├── ui/
│   ├── screen/
│   │   ├── ExplorerScreen.kt             # Pantalla principal
│   │   └── ExplorerViewModel.kt          # ViewModel principal
│   │
│   ├── components/
│   │   ├── IconGridView.kt               # Vista de cuadrícula
│   │   ├── FileListItem.kt               # Item de lista
│   │   ├── FilePropertiesDialog.kt       # Diálogo de propiedades
│   │   ├── TopBar.kt                     # Barra superior
│   │   ├── BottomBar.kt                  # Barra inferior
│   │   ├── ContextMenu.kt                # Menú contextual
│   │   └── Dialogs.kt                    # Diálogos varios
│   │
│   └── theme/
│       ├── Color.kt                      # Colores del tema
│       ├── Theme.kt                      # Tema Material 3
│       └── Type.kt                       # Tipografía
│
└── di/
    └── AppModule.kt                      # Módulo de Hilt
```

---

## 🔧 Tecnologías

### **Core**
- **Kotlin** 1.9.x
- **Jetpack Compose** (UI moderna)
- **Material Design 3**
- **Coroutines** (operaciones asíncronas)
- **Flow/StateFlow** (state management)

### **Arquitectura**
- **Hilt** (Dependency Injection)
- **ViewModel** (MVVM)
- **Use Cases** (Clean Architecture)

### **Multimedia**
- **Coil** (carga de imágenes)
- **MediaMetadataRetriever** (miniaturas de video/audio)
- **PdfRenderer** (miniaturas de PDF)

### **Almacenamiento**
- **SharedPreferences** (badges tracker)
- **File System API** (operaciones de archivos)
- **FileProvider** (compartir archivos)

---

## ⚠️ Problemas Conocidos

### **Críticos** 🔴

#### 1. Error al cargar icono de Poweramp
**Síntoma:**
```
Error getting app icon for com.maxmpz.audioplayer: com.maxmpz.audioplayer
```

**Estado:** Investigando  
**Workaround:** El badge tracking funciona, solo falla el icono  
**Prioridad:** Media

---

### **Modo DeX** 🟡

#### 2. Comportamiento inconsistente del ciclo de vida
**Síntoma:**
- Eventos ON_RESUME/ON_PAUSE se disparan múltiples veces
- No hay señal clara cuando Settings se cierra vs cambio de ventana
- Samsung Music a veces no abre en modo DeX

**Estado:** En análisis con logging exhaustivo  
**Posible solución:** Sistema manual de badges en lugar de automático  
**Prioridad:** Media

#### 3. Chooser de Android no siempre aparece después de limpiar default
**Síntoma:**
- En DeX, después de limpiar app predeterminada, el chooser no aparece
- Se abre directamente con la primera app disponible

**Estado:** Implementado `forceChooser = true` en Stage 1  
**Workaround:** Usuario puede usar "Open with" manualmente  
**Prioridad:** Baja

---

### **Rendimiento** 🟢

#### 4. Generación de miniaturas de carpetas puede ser lenta
**Síntoma:**
- Carpetas con muchas imágenes tardan en mostrar preview
- Uso de memoria aumenta con muchas carpetas visibles

**Estado:** Funcional, pero optimizable  
**Próxima mejora:** LruCache + lazy loading  
**Prioridad:** Baja

---

## 💡 Decisiones de Diseño

### **1. Sistema de Badges Personalizado**

**Problema:** Android hereda defaults por categoría (audio/* agrupa MP3, FLAC, WAV)

**Solución:** Sistema interno de tracking con `SharedPreferences`

**Ventajas:**
- ✅ Usuario controla badges por tipo de archivo específico
- ✅ MP3 puede tener Samsung Music, FLAC puede tener Poweramp
- ✅ No depende del comportamiento buggy de Android

**Desventajas:**
- ❌ Requiere acción manual del usuario ("Show badge")
- ❌ Sistema dual (Android maneja defaults, DExplorer maneja badges)

**Decisión:** Manual es mejor que automático buggy

---

### **2. Miniaturas Dinámicas de Carpetas**

**Enfoque:** Mostrar hasta 4 imágenes más recientes

**Razones:**
- ✅ Visual feedback inmediato del contenido
- ✅ Similar a Windows Explorer (familiar)
- ✅ Rendimiento aceptable (solo carpeta actual)

**Limitaciones:**
- ⚠️ Solo escanea carpeta actual (no subfolders)
- ⚠️ Limitado a 4 imágenes máximo
- ⚠️ Solo media files (imágenes/videos)

**Alternativas consideradas:**
- ❌ Escanear recursivamente: Muy lento
- ❌ Mostrar primera imagen solo: Menos informativo
- ✅ Sistema actual: Balance perfecto

---

### **3. Arquitectura Clean Architecture**

**Razones:**
- ✅ Separación clara de responsabilidades
- ✅ Testeable (Use Cases aislados)
- ✅ Escalable (fácil añadir features)
- ✅ Mantenible (cambios localizados)

**Trade-offs:**
- ❌ Más código boilerplate
- ❌ Curva de aprendizaje inicial
- ✅ Vale la pena a largo plazo

---

### **4. Jetpack Compose sobre Views**

**Razones:**
- ✅ UI declarativa (más simple)
- ✅ Menos código
- ✅ Recomposición inteligente
- ✅ Mejor para listas dinámicas

**Desafíos:**
- ⚠️ Lifecycle events requieren DisposableEffect
- ⚠️ StateFlow para sobrevivir configuration changes
- ✅ Resuelto con patrones establecidos

---

## 📝 Notas de Desarrollo

### **Logging Strategy**

El proyecto usa un sistema de logging exhaustivo para debugging:

```kotlin
FileLogger.log("Operación X iniciada")
FileLogger.log("  Parámetro: $valor")
FileLogger.log("  Resultado: $resultado")
```

**Ubicación logs:** `/storage/emulated/0/Android/data/com.example.dexplorer/files/logs/`

**Formato:** `dexplorer_YYYY-MM-DD_HH-MM-SS.txt`

---

### **Testing en DeX**

**Protocolo de prueba:**
1. Iniciar app en modo DeX
2. Navegar a carpeta con archivos de prueba
3. Ejecutar operación específica
4. Verificar logs
5. Comparar con comportamiento en teléfono

**Archivos de prueba necesarios:**
- MP3 files (para badges)
- FLAC files (para badges)
- Carpeta con imágenes (para thumbnails)
- Carpeta con videos (para thumbnails)

---

### **Próximos Pasos Inmediatos**

1. ✅ **Implementar miniaturas de carpetas** ← Completado
2. 🔄 **Generar logs exhaustivos en DeX** ← En progreso
3. 📋 **Analizar logs y decidir estrategia de badges**
4. 🐛 **Fix error icono Poweramp**
5. 🧪 **Testing en múltiples dispositivos Samsung**

---

## 📚 Recursos

### Documentación
- [Android File Provider](https://developer.android.com/reference/androidx/core/content/FileProvider)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [Samsung DeX Guidelines](https://developer.samsung.com/samsung-dex)

### Referencias
- Windows Explorer (UI/UX inspiration)
- Solid Explorer (Android reference)
- MiXplorer (Feature reference)

---

## 📊 Métricas del Proyecto

**Líneas de código:** ~8,000+  
**Archivos Kotlin:** ~30  
**Composables:** ~25  
**Use Cases:** 9  
**Tiempo de desarrollo:** 6 fases (~3 meses)  
**Estado:** 95% completado (Fase 6)

---

## 🤝 Contribuciones

Este es un proyecto personal en desarrollo activo.

**Áreas donde se necesita ayuda:**
- 🐛 Testing en modo DeX (múltiples dispositivos)
- 🎨 Diseño de iconos personalizados
- 📱 Testing en tablets
- 🌍 Traducción a otros idiomas

---

## 📄 Licencia

*(Definir licencia según preferencia)*

---

**Última actualización:** 28 de enero de 2026  
**Versión del plan:** 1.0  
**Autor:** DExplorer Team

---

*Este documento es un plan vivo y se actualiza conforme el proyecto evoluciona.*
