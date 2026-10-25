# MyCAD — CAD 2D/3D para Android

## Estado actual: MyCAD 0.6.0 en desarrollo

**Versión configurada en Android:** \`0.6.0\` (\`versionCode 60\`, en \`app/build.gradle\`).
**Último commit del motor validado por CI antes de esta actualización:** \`d167559\`.
**Última ejecución de referencia:** [GitHub Actions #322 — correcta](https://github.com/josxtm/mycad/actions/runs/40245497754).

El código 0.6.0 pasa las pruebas de integridad, las regresiones geométricas y las tareas Gradle de tests y Lint. **Estos cambios aún no se han verificado en el Nothing Phone 2a** y **no se ha compilado una APK 0.6.0**. El código no equivale a una versión estable o lista para producción.

### Motor 0.6.0 — funcionalidades ya incorporadas al código

- Bocetos, extrusiones y vinculación parcial entre croquis y piezas, con identificadores persistentes.
- Historial de operaciones y dependencias paramétricas; reconstrucción incremental de algunas operaciones booleanas.
- Selección lógica persistente de caras y aristas de primitivas.
- Primera capa B-Rep **analítica y limitada**: prismas con ocho vértices, doce aristas y seis caras; cilindros con tres superficies y dos aristas circulares.
- Edición de caras de prismas y cilindros desde el visor CAD experimental, incluida conservación de la posición de la cara opuesta y deshacer/rehacer.
- Operaciones booleanas geométricas específicas con límites; la malla resultante no se convierte en una B-Rep general editable.

**Aún pendiente:** núcleo B-Rep general, booleanas exactas entre sólidos arbitrarios, redondeos/chaflanes, STEP, validación táctil de 0.6.0 y pruebas de regresión en Android real. No se ofrece paridad funcional con Shapr3D.

Detalles: [roadmap actual](docs/ROADMAP.md) · [B-Rep de prismas](docs/CAD_BREP_BOX.md) · [B-Rep de cilindros](docs/CAD_BREP_CYLINDER.md) · [edición Android](docs/CAD_BREP_ANDROID.md).

### APK y pruebas en Android

La **última APK de referencia es la de 0.5.0**, no la versión actual del código. Está enlazada en la [ejecución histórica de GitHub Actions 0.5.0](https://github.com/josxtm/mycad/actions/runs/40140494636); el artefacto podría haber caducado. Se han comunicado pruebas funcionales satisfactorias de 0.5.0 en Nothing Phone 2a (inicio, plano, dibujo, cotas, navegación y guardar/abrir); extrusión y otros planos no se validaron completamente.

Para futuros hitos 0.6.0: abrir [Actions](https://github.com/josxtm/mycad/actions), lanzar manualmente el workflow con \`generar_apk=true\` o aprobar un commit de hito con prefijo exacto \`release: MyCAD \`. El flujo ordinario de desarrollo **solo ejecuta pruebas y Lint; no construye APK**. Cuando exista un artefacto \`myCad0.6.0\`, descargarlo desde la ejecución correspondiente (Artifacts), descomprimir el ZIP e instalar la APK en Android. **Nunca instalar \`Source code (zip)\` como si fuese una APK.**

### Flujo de contribución

Trabajar en una rama \`feature/\`, \`fix/\` o \`maintenance/\`, abrir una Pull Request dirigida a \`main\`, revisar el diff y comprobar que termina correctamente el job **verify** antes de fusionar. No ensamblar ni publicar APK para cada commit. Véanse [CONTRIBUTING.md](CONTRIBUTING.md) y [la plantilla de PR](.github/PULL_REQUEST_TEMPLATE.md).

**Importante:** el trabajo por PR se documenta y utiliza; para **impedir técnicamente** pushes directos o merges sin CI hay que activar una regla de protección/ruleset de \`main\` en GitHub, que requiere permisos de administración. Esta documentación no sustituye esa protección.

## Historial anterior (referencia, no estado actual)

La siguiente sección conserva hitos pasados. Las menciones a versiones, pruebas, límites o flujos CI antiguos describen **su contexto histórico**; el estado vigente está arriba.

## Versión 0.4.8 — rotación XYZ táctil

- Herramienta **GIRAR 3D**: ángulos numéricos independientes X, Y y Z en grados.
- Manipulador de rotación con tres aros: rojo X, verde Y y azul Z, arrastrables con el dedo.
- Representación 3D del prisma con rotación XYZ y almacenamiento de los ángulos en JSON.
- Corrección de compilación: variable `rotationMode` compartida correctamente con `MainActivity`.
- **Compilación Android confirmada**. El usuario confirmó el funcionamiento de los gestos en el móvil.
- Limitaciones: el visor GPU, las operaciones con mallas importadas y las transformaciones avanzadas aún no ofrecen paridad con Shapr3D.

## Versión 0.4.7 — primera rotación Z

- Botón **GIRAR Z** para introducir ángulo en grados.
- Rotación Z visible y persistida en el archivo de proyecto.
- Rotación Z confirmada funcional en el móvil.

## Versión 0.4.6 — interfaz móvil y manipulador XYZ

- Barra inferior permanente para NAVEGAR, MOVER, EDITAR y posición XYZ numérica.
- Manipulador táctil de tres ejes sobre la pieza seleccionada (X rojo, Y verde, Z azul); arrastre restringido al eje elegido.
- Árbol de cuerpos accesible desde el lateral derecho, con selección directa y dimensiones.
- Lectura de coordenadas XYZ en milímetros durante el movimiento.
- Esta versión implementa una primera aproximación de interfaz inspirada en CAD táctil; **no** incluye aún operaciones avanzadas de Shapr3D como booleanas, extrusión de perfiles, edición por caras o historial paramétrico.
- Pendiente: comprobar compilación GitHub Actions y probar gestos en Android real.

## Instalación y compilación

1. Entra en [GitHub Actions — APK Android](https://github.com/josxtm/mycad/actions).
2. Abre una ejecución con resultado **Success** y desplázate hasta **Artifacts**.
3. Descarga el artefacto `myCad<VERSIÓN>` (por ejemplo, `myCad0.4.5`).
4. Descomprime el ZIP e instala `myCad<VERSIÓN>.apk` en Android, autorizando la instalación de APK si es necesario.

El proyecto usa **Android SDK 35**, **minSdk 26**, **Java 17** y **Gradle 8.9** en el flujo CI. **Ahora** la ejecución normal valida con `gradle :app:testDebugUnitTest :app:lintDebug --no-daemon`; la tarea `:app:assembleDebug` se reserva para un hito explícito. La salida del hito es una **APK debug** y el nombre depende de `versionName` en `app/build.gradle`.

**Ejecución histórica de v0.4.5:** [GitHub Actions #40132020733](https://github.com/josxtm/mycad/actions/runs/40132020733) — compilación, Lint y subida de artefacto correctos.

## Funcionalidades

- Crear y editar piezas rectangulares y cilíndricas con medidas en milímetros.
- Bocetos 2D táctiles de líneas, rectángulos y círculos.
- Selección de piezas, controles geométricos, desplazamiento y restricciones de movimiento.
- Vistas 2D y 3D, cuadrícula configurable, ejes y ajuste a cuadrícula.
- Deshacer/rehacer, tema claro/oscuro, ajustes de sensibilidad y modo película de cámara.
- Guardar y abrir diseños `.mycad.json` con el selector de documentos de Android; borrador interno automático.
- **Desde v0.4.4:** módulo `MeshEngine.java` de visualización de mallas con OpenGL ES 3.0; importación STL binario/ASCII y OBJ; exportación STL binario y OBJ; triangulación de piezas básicas para exportación. El visor GPU se abre desde la interfaz como visor adicional: **no sustituye aún toda la escena CAD anterior**.
- **Desde v0.4.5:** selector de modos **Navegar / Mover / Editar** para separar el giro de cámara, el movimiento de piezas y el arrastre de controladores.

## Historial de versiones

Todas las versiones descritas corresponden a las iteraciones de este repositorio. Se distinguen los cambios de código de su verificación real en Android.

| Versión | Cambios realizados | Estado conocido |
| --- | --- | --- |
| **0.1** | Primera prueba de concepto de MyCAD para Android, anterior a las mejoras de archivos y herramientas. | Prototipo histórico; alcance detallado no documentado. |
| **0.2** | Piezas básicas con dimensiones y espesor; vista 3D; guardar y abrir `.mycad.json` mediante Android; borrador, guía y plan de desarrollo. | Documentada en el README original; pruebas históricas no verificadas aquí. |
| **0.3** | Creación, selección y movimiento básico de piezas; coordenadas XYZ; deshacer/rehacer; archivos JSON. | Compilada y confirmada funcional por el usuario, según historial del proyecto. |
| **0.4.1** | Barras laterales compactas, menú de proyecto e iconos provisionales. | Compilada y confirmada funcional por el usuario, según historial. |
| **0.4.2** | Bocetos táctiles de línea, rectángulo y círculo; selección y controladores; edición por arrastre; ajustes de tema y sensibilidad; modo película 3D; revisión de guardado/recuperación de bocetos, historial y cámara. | Compilaciones correctas; se detectaron limitaciones de selección y gestos que motivaron versiones posteriores. |
| **0.4.3** | Controladores de dimensiones de piezas 3D (W/D/H/R); bloqueo de cámara durante edición; movimientos libre, ortogonal, por ejes, radial y exacto; cuadrícula con planos XY/XZ/YZ, pasos y Snap; persistencia de ajustes; corrección de Lint y nombre automático del APK. | **Compilación correcta y funciones confirmadas por el usuario en móvil**. |
| **0.4.4** | Nuevo motor gráfico GPU basado en OpenGL ES 3.0; visor de mallas triangulares; lectores STL/OBJ; exportadores STL/OBJ; triangulación de prismas y cilindros; menú de importación/exportación. | **APK compilado**; no consta validación completa de importación/exportación ni rendimiento en móvil. |
| **0.4.5** | Separación de interacción en modos **Navegar, Mover y Editar**; giro con un dedo sobre la escena aunque haya selección; gesto de dos dedos para desplazamiento de vista; zoom con pellizco; eliminación de la dependencia del doble/triple toque para mover/editar; desplazamiento aplicado a la proyección 2D/3D. | **APK compilado y artefacto publicado**; ergonomía y gestos pendientes de confirmación en móvil. |

### Compilaciones de referencia

| Versión | Ejecución GitHub Actions | Resultado |
| --- | --- | --- |
| 0.4.2 (revisión) | [40128738815](https://github.com/josxtm/mycad/actions/runs/40128738815) | Success |
| 0.4.3 | [40130175267](https://github.com/josxtm/mycad/actions/runs/40130175267) | Success |
| 0.4.4 | [40131327164](https://github.com/josxtm/mycad/actions/runs/40131327164) | Success |
| 0.4.5 | [40132020733](https://github.com/josxtm/mycad/actions/runs/40132020733) | Success |

## Formatos de archivo

| Formato | Abrir/importar | Guardar/exportar | Observaciones |
| --- | --- | --- | --- |
| `.mycad.json` | Sí | Sí | Proyecto nativo; no equivale a un sólido CAD industrial. |
| `.stl` | Sí, binario/ASCII | Sí, binario | Malla triangular; se asumen milímetros. |
| `.obj` | Sí, geometría de caras | Sí | Malla triangular; el lector triangula polígonos. No hay soporte completo de materiales/texturas MTL. |
| `.dxf` | Pendiente | Pendiente | Importación CAD 2D/3D aún no implementada. |
| `.step` / `.stp` | Pendiente | Pendiente | Requiere un núcleo geométrico CAD adicional. |
| `.iges` / `.igs` | Pendiente | Pendiente | No implementado. |
| `.dwg`, `.3mf`, `.cad` | Pendiente | Pendiente | No implementado; `.cad` no es un formato universal. |

**Limitaciones actuales:** STL/OBJ importados siguen siendo mallas sin historial paramétrico original. Existen booleanas específicas, algunas extrusiones vinculadas y B-Rep analítica **solo para primitivas** (prismas y cilindros); no existe todavía un motor B-Rep general ni exportación STEP. La integración completa entre visor y motor está pendiente, y el rendimiento con diseños grandes no se ha validado en móvil.

## Organización del proyecto

- `app/src/main/java/es/josxtm/mycad/MainActivity.java`: interfaz, herramientas, escena CAD clásica y flujo de documentos.
- `app/src/main/java/es/josxtm/mycad/MeshEngine.java`: visor de mallas acelerado por OpenGL ES 3.0.
- `app/src/main/java/es/josxtm/mycad/MeshIO.java`: lectura y escritura de STL/OBJ.
- `app/build.gradle`: configuración Android, número de versión y nombre del APK.
- `.github/workflows/android.yml`: compilación y publicación del artefacto APK.
- `HISTORIAL_CAMBIOS.md`: registro técnico histórico.
- `DIRECTRIZ_GESTOS.md`: especificación original de interacción; **las reglas de doble/triple toque de ese documento fueron sustituidas en la v0.4.5 por modos explícitos**.
- `GUIA.md`: documentación de uso y pruebas, que puede contener referencias a versiones anteriores.

## Próximos pasos para 0.6.0

1. Unificar selección/edición de caras y aristas con el grafo paramétrico, manteniendo UUID estables.
2. Añadir pruebas geométricas y de persistencia para cualquier nueva operación del motor.
3. Validar en Nothing Phone 2a las nuevas operaciones, navegación, undo/redo, guardado y archivos.
4. Publicar APK 0.6.0 **únicamente** cuando exista un bloque funcional relevante y la CI esté verde.
5. Mantener documentadas las limitaciones: B-Rep de primitivas ≠ kernel CAD universal.

---

**Actualización del estado:** 26 de octubre de 2026. Las secciones de historial anteriores se conservan como referencia cronológica, no como afirmaciones sobre el estado actual.
