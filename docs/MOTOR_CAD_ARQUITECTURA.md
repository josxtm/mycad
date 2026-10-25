# MyCAD — Arquitectura del motor geométrico v0.1

Estado: diseño e interfaz inicial; NO hay kernel B-Rep integrado.

## Objetivo
Un núcleo CAD independiente de Android, UI, OpenGL y persistencia de pantalla que admita bocetos, cuerpos, caras, aristas, features, selección topológica estable, reconstrucción e intercambio de mallas. El motor calcula geometría; la interfaz solo envía comandos y dibuja vistas.

## Capas
1. **core / Documento**: entidades CAD inmutables, IDs estables, historial dirigido de features, tolerancias, validación, unidades en mm.
2. **sketch**: entidades y restricciones 2D por plano; resolver geométrico y detección de perfiles cerrados.
3. **kernel**: adaptador `SolidKernel` con primitivas, extrusión, booleanas, fillet, chamfer, shell, transform y consulta de topología. Primera implementación: prototipo limitado; destino: evaluación Open CASCADE vía NDK/JNI.
4. **tessellation**: convertir cuerpos a triángulos para MeshEngine y STL/OBJ. La malla no es fuente de verdad B-Rep.
5. **selection**: ray casting con profundidad, identificación estable de cuerpo/cara/arista, sin promediar profundidad de caras como criterio definitivo.
6. **document I/O**: formato nativo versionado y migrador desde JSON v1. Guardar árbol de features y parámetros. OBJ/STL son salida/intercambio de malla, NO formatos editables.
7. **android/UI**: usa `CadEngine` y eventos `CommandResult`; ningún cálculo del kernel dentro de MainActivity.

## Modelo
- `CadBody`: UUID, forma opaca del kernel, bounding box, sistema local, etiqueta.
- `CadFace`, `CadEdge`: UUID, id de cuerpo, tipo geométrico, referencias y atributos (superficie, curva, normales, límites).
- `CadFeature`: UUID, tipo (sketch, extrude, boolean, fillet...), inputs, parámetros, dependencias y estado de reconstrucción.
- `CadDocument`: versión, unidades, entidades, árbol ordenado de operaciones, referencias, historial, errores.
- `CadTolerance`: precisión lineal/ angular y reglas de operaciones robustas; nunca comparar doubles directamente para uniones.
- `SolidKernel`: operaciones y topología, sin importar clases Android.
- Nombres/IDs estables en reconstrucción: referencias por feature + criterio geométrico, registro explícito de cambios/splits/merges. No prometer persistencia perfecta ante operaciones arbitrarias.

## Contratos de operación
- Entrada validada (unidades, rango, perfil cerrado, geometría no degenerada).
- Resultado explícito `SUCCESS/UNSUPPORTED/INVALID_GEOMETRY/FAILED` con diagnóstico.
- **Atomicidad**: no sustituir geometría válida si una operación falla.
- Deshacer/rehacer: historial de comandos y snapshots/versiones consistentes.
- Cálculos de volumen, superficie, bounding box, manifold y orientación; medición de regresiones.
- Booleanas deben devolver topología sin caras interiores y con orientaciones coherentes.
- Las operaciones avanzadas quedan deshabilitadas hasta que el kernel concreto las implemente.

## API objetivo
```java
interface SolidKernel {
  Result<Body> createBox(double x, double y, double z, double w, double d, double h);
  Result<Body> extrude(Profile closedProfile, double signedHeight);
  Result<Body> booleanOp(Body a, Body b, BooleanOperation operation);
  Result<Body> fillet(Body body, List<EdgeId> edges, double radius);
  Result<Body> chamfer(Body body, List<EdgeId> edges, double distance);
  Result<Mesh> tessellate(Body body, double deflection);
  Result<Topology> topology(Body body);
}
```
Este bloque es contrato de diseño, no código compilable. Se implementará con clases Java puras y un adaptador NDK opcional.

## Motor existente y migración
- Mantener `MeshEngine.java` como visualizador de triángulos.
- Conservar `GeometryMath`, `BoxBoolean` y `BoxBooleanMesh` como geometría simple/regresión.
- `MainActivity` sigue ejecutando el modelo antiguo durante la transición.
- Crear un núcleo Java independiente en `es.josxtm.mycad.cad`; implementar caja con volumen y validación antes de conectarlo.
- Sin cambiar formato JSON ni comportamiento en móvil mientras se comprueba compatibilidad.
- No usar REA como kernel; se reserva para investigación permitida de comportamientos de producto.

## Fases y puertas de entrega
1. Interfaces y tipos base Java puro con tests de volumen y errores.
2. Feature graph y reconstrucción reproducible; tests de dependencias, eliminación y undo/redo.
3. Extrusión y booleanas del kernel, con pruebas geométricas.
4. Evaluar OCCT Android NDK: licencias, ABI arm64-v8a, tamaño APK, compilación CI, JNI, geometría tolerante. Adoptar solamente si pasa prototipo real.
5. Adaptador de mallas y selección por ray cast; precisión y tests de profundidad.
6. Migración JSON v1 a documento nativo versionado y serialización redonda.
7. Integración incremental con Android y validación manual Nothing Phone 2a.

## Criterios mínimos de aceptación
- Cubo 20 × 10 × 5 mm => volumen 1000 mm³ y superficie 700 mm².
- Dimensiones cero, negativas o no finitas => INVALID_GEOMETRY.
- Modificar altura 5 → 8 => 1600 mm³ sin duplicar body.
- Booleanas de cubos solapados: volumen validado y malla cerrada sin caras interiores.
- Deshacer/rehacer y reabrir reproducen el mismo conjunto de entidades y relaciones.
- Selección de caras/aristas respeta ocultación y distancia ray-cast.

**No implementar fillet, chamfer ni shell sobre triángulos como si fueran operaciones B-Rep.**

## Implementación incremental — documento paramétrico

Se incorpora `CadDocument.java` como módulo Java puro independiente de Android. Registra features de prismas con identificadores estables, reconstruye el conjunto de cuerpos desde sus parámetros y aplica transacciones atómicas. Admite `addBox`, `setBoxHeight`, `removeBody`, `undo` y `redo`, y elimina la rama de rehacer al insertar una operación después de deshacer. Los snapshots son inmutables desde su API pública.

Se añade `tests/CadDocumentTest.java` al flujo CI para comprobar volumen 1000→1600 mm³, conservación de ID, rechazo de altura inválida sin cambio de revisión, deshacer/rehacer, múltiples cuerpos y eliminación.

**Limitación:** reconstrucción limitada a primitivas de caja. Las features todavía no incluyen dependencias entre cuerpos, sketch solver, booleanas B-Rep, serialización propia ni integración en la Activity. Es una base paralela, no sustituye el motor actualmente usado por la app.

## Seguimiento de hitos objetivos — 2026-10-25

**Avance global estimado del nuevo motor: ~15 %.** Estimación manual basada en funcionalidades disponibles; no equivale a progreso de GitHub Actions.

| Hito acumulado | Criterio de aceptación | Estado a fecha de revisión |
|---|---|---|
| 30 % | Documento paramétrico y grafo de dependencias entre bocetos y operaciones | En desarrollo: CadDocument soporta cajas, revisiones y deshacer; faltan dependencias reales |
| 50 % | Extrusiones y booleanas usando el nuevo kernel independiente | Pendiente: solo existen implementaciones anteriores fuera del núcleo nuevo |
| 75 % | Nuevo motor conectado a Android y selección topológica fiable | Pendiente: MainActivity continúa usando el motor antiguo |
| 90 % | Persistencia versionada del nuevo motor, historial completo y herramientas avanzadas | Pendiente |
| 100 % | Tests geométricos integrales, verificación CI y validación funcional en Nothing Phone 2a | Pendiente |

**Último CI confirmado antes de esta actualización:** ejecución #127, commit `0797a788`, `success`; la compilación automática no constituye validación funcional en dispositivo.

## Dependencias boceto → extrusión: prototipo Java aislado

`ParametricSketchDocument.java` incorpora perfiles rectangulares XY con identificador estable, extrusión vertical y dependencia explícita de cada cuerpo respecto a su boceto. Editar un rectángulo reconstruye los cuerpos dependientes y conserva los IDs. Modificar un boceto o intentar una extrusión inválida no altera el estado si falla la validación. Se admiten deshacer/rehacer y eliminación de bocetos con sus extrusiones dependientes. `ParametricSketchDocumentTest.java` verifica estas condiciones en la CI.

**Restricciones:** solo rectángulos XY y altura positiva. Aún no se enlaza a `MainActivity`, no reemplaza `CadDocument` anterior, no implementa perfiles arbitrarios, curvas, NDK ni B-Rep. El hito del 30 % sigue abierto hasta generalizar dependencias y guardar/reabrir el grafo.

## Incremento: serialización del documento paramétrico

El prototipo `ParametricSketchDocument` incorpora `exportSnapshot()` e `importSnapshot()` con cabecera de versión `MYCAD-PARAMETRIC|1`, validación de geometría, UUID, límites de tamaño y reconstrucción de extrusiones dependientes. Se añaden pruebas de ida/vuelta y rechazo de versión/dimensiones inválidas. Se trata de un formato textual **experimental del núcleo**, independiente del JSON v1 de la aplicación; no se conecta aún a los controles Guardar/Abrir de Android.

**Pendiente para 30 %:** grafo de features más general, múltiples planos/perfiles, reconstrucción tras edición de bocetos y migración del documento existente. **Pendiente para 100 %:** integración B-Rep, topología robusta, Android, CAD avanzado y validación real en dispositivo.

## Incremento: edición paramétrica de extrusiones — 2026-10-25

- `ParametricSketchDocument.setExtrusionHeight(featureId, height)` actualiza el parámetro y regenera el cuerpo conservando los identificadores de feature y body.
- `removeExtrusion(featureId)` elimina solo la operación y su cuerpo, manteniendo el boceto original para otras operaciones.
- Las ediciones inválidas no modifican el estado ni el historial.
- `ParametricSketchDocumentTest` incorpora regresiones de volumen, identidad, atomicidad y deshacer/rehacer.
- Las pruebas están conectadas al flujo de geometría de GitHub Actions. **No significa que este motor esté conectado a la UI Android ni que exista aún B-Rep.**

## Implementación: mapeo de extrusión XY/XZ/YZ

- `PlaneExtrusion` adapta perfiles rectangulares en coordenadas locales (u,v) a prismas axis-aligned en los planos XY, XZ y YZ.
- Admite extrusión con altura firmada: el sentido negativo desplaza el origen al otro lado del plano manteniendo dimensiones positivas.
- Pruebas independientes de Android verifican volumen, dirección y dimensiones en los tres planos, así como rechazo de entradas inválidas.
- **Todavía no está conectado al grafo `ParametricSketchDocument` ni a la interfaz Android.** Es una capa geométrica separada; no supone selección avanzada ni B-Rep.

## Integración paramétrica de los planos — 2026-10-25

`ParametricSketchDocument` conserva ahora el plano de cada boceto rectangular (`XY`, `XZ`, `YZ`). El método `addRectangleOnPlane()` y las extrusiones de altura firmada utilizan `PlaneExtrusion` durante la reconstrucción, manteniendo el identificador del cuerpo. `exportSnapshot` guarda el plano por perfil y `importSnapshot` acepta tanto registros nuevos de 7 campos como los antiguos de 6 campos (asumiendo XY). Se han añadido regresiones de volumen, edición y recuperación para los tres planos; pruebas Java puras, todavía no integradas en la UI ni equivalentes a B-Rep.

## Dependencias compartidas y compatibilidad documental

`ParametricSketchDocument.dependentsOf(sketchId)` expone una lista inmutable de extrusiones dependientes. Un mismo boceto puede impulsar varios cuerpos, que se reconstruyen conjuntamente al editar el perfil, conservando los IDs propios. Se incluyen pruebas de eliminación selectiva de una extrusión, deshacer, carga de instantáneas antiguas sin campo de plano (XY por defecto), rechazo de referencias colgantes y duplicados. Estas son pruebas del núcleo Java; el editor Android sigue empleando su modelo previo y la topología B-Rep continúa pendiente.

## Edición de posicionamiento paramétrico — 2026-10-25

Nuevas operaciones `setExtrusionOffset(featureId, newOffset)` y `setSketchPlane(sketchId, newPlane)`: regeneran cuerpos vinculados mediante el adaptador XY/XZ/YZ, conservan identificadores, registran revisiones para deshacer/rehacer y rechazan cambios inválidos sin comprometer el documento. Pruebas cubren traslado, cambio de plano, persistencia y deshacer/rehacer. Aún no conectadas a la interfaz de Android ni a B-Rep.

## Hito de integridad interna — 2026-10-25

`ParametricSketchDocument.validateIntegrity()` comprueba sin modificar el historial la existencia de bocetos referenciados, unicidad de ID de cuerpos, coherencia numérica de cada prisma regenerado y ausencia de cuerpos huérfanos. Devuelve una lista inmutable de diagnósticos. Las pruebas cubren documentos editados, con múltiples dependencias y restaurados. Este control no sustituye a un kernel B-Rep ni constituye integración con la interfaz Android.

## Primer uso del motor en Android — 2026-10-25

La acción «Crear sólido» de una extrusión rectangular invoca ahora `PlaneExtrusion.extrude(PrimitiveKernel, plano, dimensiones, altura)` antes de confirmar el guardado. Si el motor rechaza la geometría, no se confirma el borrador; si la acepta, el mensaje de volumen procede del motor. La geometría visual y el formato de archivo actuales siguen utilizándose para preservar compatibilidad; los cilindros mantienen el camino previo. **Esta es una integración incremental de validación, no una sustitución del renderizador ni un motor B-Rep.**

## Validación de edición de altura en Android

`MainActivity.editExtrusionHeight` comprueba ahora, para prismas rectangulares vinculados a un boceto existente, la extrusión con `PlaneExtrusion` y `PrimitiveKernel` antes de guardar la modificación. No altera el tratamiento actual de cilindros ni los archivos anteriores. La validación no equivale aún a sincronizar el historial de la UI con el documento paramétrico ni a B-Rep.

## Lote integrado de edición de bocetos Android (2026-10-25)

`SketchEditValidator` efectúa validación previa conjunta de un rectángulo modificado y de todas sus extrusiones vinculadas, en planos XY/XZ/YZ. Se conecta a **Acotar perfil** y **Editar boceto** antes de registrar el checkpoint, evitando guardar dimensiones degeneradas en esos flujos. Incluye pruebas Java de varios dependientes, extrusión de sentido negativo, alturas cero, planos inválidos y NaN. Se mantiene el guardado JSON v1, visor y geometría circular sin cambios. La UI no está todavía migrada al grafo paramétrico persistente; el avance no se debe interpretar como B-Rep.

## Vista previa de migración paramétrica (fase Android)

`CadIntegrationProbe.inspect` genera ahora un `parametricSnapshot` de los perfiles rectangulares y sus extrusiones compatibles, lo vuelve a importar y verifica integridad y número de cuerpos antes de devolver el informe. El formato es MYCAD-PARAMETRIC|1. En proyectos mixtos es una **vista previa parcial**: círculos, piezas libres y mallas no se trasladan, por lo que nunca debe sustituir automáticamente el JSON v1. La aplicación no cambia aún el formato de guardado. Pruebas de ida/vuelta sobre documentos completos y mixtos.

## Archivo Android con vista previa paramétrica opcional (2026-10-25)

`MainActivity.encode()` mantiene el contenedor JSON `format=mycad`, `version=1`, `units=mm` y los campos anteriores (`parts`, `sketches`, `booleanMeshes`). Añade `parametricPreview` con el snapshot generado y probado mediante `CadIntegrationProbe`, además del indicador `parametricPreviewCoverageComplete`. Los lectores existentes siguen pudiendo abrir la parte legacy. La vista previa es **no autoritativa** y puede ser parcial; la aplicación sigue reconstruyendo las piezas desde `parts`/`sketches` al cargar. Los errores de construcción de la vista previa no impiden guardar el modelo previo. Sigue pendiente la migración bidireccional, la asignación estable de ID entre modelos y un B-Rep general.

## Vínculos entre cuerpos Android y vista previa (2026-10-25)

`CadIntegrationProbe.Report` registra `compatiblePartIndices` y `generatedBodyIds` en el mismo orden, enlazando cada parte antigua con el cuerpo generado. `MainActivity.encode()` añade el campo opcional `parametricBodyBindings` con índice, ID, boceto origen, traslación XYZ y giros XYZ. Son metadatos complementarios; las posiciones y rotaciones todavía **no se aplican al sólido del núcleo**, de modo que no deben considerarse una migración geométrica completa. El JSON v1 sigue siendo la fuente autoritativa. Pruebas cubren orden y unicidad de los vínculos.

## Núcleo de transformaciones rígidas 3D

`RigidTransform` implementa rotaciones en grados alrededor de X, después Y y después Z, seguidas de traslación XYZ. Transforma puntos y los ocho vértices de los prismas, y calcula una caja contenedora AABB de la geometría rotada. Valida entradas finitas; las pruebas cubren los tres ejes, traslaciones y límites. **No cambia todavía el renderizado Android ni dota al kernel de caras/aristas B-Rep; las cajas contenedoras no sustituyen sólidos rotados.**

## Vista previa transformada de cuerpos — 2026-10-25

`TransformedBody` combina `CadKernel.Box` con `RigidTransform`: proporciona ocho vértices en coordenadas globales, centro, AABB transformada y volumen conservado. `CadIntegrationProbe` recibe opcionalmente las transformaciones antiguas y genera cotas mundiales junto a `generatedBodyIds`; la exportación JSON mantiene dichos límites en `parametricBodyBindings[].worldBounds`. Incluye pruebas Java de rotación Z 90° y traslación. **Los límites AABB son metadatos de vista previa, no una representación topológica B-Rep, y el renderizador Android no se ha sustituido.**

## Triangulación de prismas transformados

`BoxSurface.triangles(TransformedBody)` devuelve 12 triángulos (36 vértices, coordenadas float XYZ) de la superficie del cuboide con transformaciones rígidas aplicadas. Las caras se ordenan con normales exteriores y las pruebas validan límites y orientación. Es una malla destinada a renderizado, no una topología B-Rep editable. Aún no está conectada al visor Android.

## Primer visor CAD experimental (2026-10-25)

El menú de proyecto ofrece `Alternar visor CAD experimental`, desactivado inicialmente. Si está activo y todas las piezas del editor son prismas, `currentMesh()` pasa por `PrimitiveKernel` → `TransformedBody` → `BoxSurface` para generar triángulos XYZ con traslación y rotaciones. Conserva las mallas importadas/booleanas; cuando hay cilindros usa el visor clásico para no ocultarlos. Es una opción temporal de representación GPU, no altera `parts`, ni el guardado, ni el algoritmo principal de pantalla; todavía no incluye selección B-Rep ni cilindros en el nuevo motor.

## Selección geométrica inicial (ray picking)

`CadRayPicker` realiza intersección rayo-triángulo sobre una superficie triangulada ya transformada, devuelve el triángulo más cercano y coordenadas de impacto, o null cuando no hay contacto. Incluye controles de entradas inválidas y pruebas sobre prisma rotado, selección de cara superior y rayos que no alcanzan la pieza. No está conectado aún a gestos Android, ni da una entidad de cara B-Rep persistente.

## Interacción táctil con el visor GPU — 2026-10-25

`MeshEngine` distingue una pulsación corta de los gestos de arrastre/órbita; proyecta un rayo desde la pantalla deshaciendo la matriz proyección×vista de la cámara y emplea `CadRayPicker` para localizar la intersección positiva más cercana. Entrega el índice global de triángulo y coordenadas XYZ mediante `SurfaceTapListener`, y `MainActivity` muestra el resultado en el rótulo de información. En esta fase identifica triángulos, no caras paramétricas persistentes, y aún no permite editar directamente la superficie seleccionada. Se conserva la navegación habitual del visor GPU.

## Selección de caras lógicas de prismas (2026-10-25)

`BoxFaceSelection` relaciona dos triángulos consecutivos con cada una de las seis caras etiquetadas Z_MIN, Z_MAX, Y_MIN, Y_MAX, X_MIN, X_MAX y permite extraer los triángulos de una cara. Las pruebas verifican los 12 triángulos. `MainActivity` muestra la cara identificada al tocar el visor GPU experimental **solo cuando hay un único prisma y ninguna malla adicional**; en escenas complejas mantiene la identificación del triángulo hasta implementar el mapa de índices por cuerpo. Aún no hay resaltado visual ni edición directa de caras.

## Selección GPU multicuerpo (2026-10-25)

`CadSceneFaceMap` convierte el índice global de triángulo en índice de pieza y cara lógica del prisma. La pantalla experimental calcula el desplazamiento de triángulos de las mallas importadas/booleanas y agrupa 12 triángulos por prisma válido. Al tocar una cara muestra cuerpo, cara y coordenadas y selecciona la pieza correspondiente en el editor. Si hay cilindros se conserva el modo clásico y la identificación básica de triángulos. La malla GPU aún no resalta gráficamente la cara ni permite extruir desde ella. Las pruebas Java verifican escenas con varias piezas y desplazamiento inicial.

## Resaltado visual de caras GPU (2026-10-25)

`MeshEngine.highlightFace(firstTriangle)` dibuja en ámbar los dos triángulos contiguos de la cara seleccionada en el visor GPU. `MainActivity` convierte la selección multicuerpo en el primer triángulo de la cara y limpia el resaltado cuando no corresponde a un prisma; `setMeshes` reinicia la selección para evitar índices obsoletos. El modo clásico permanece disponible. La edición directa de caras y las pruebas visuales reales en Android siguen pendientes.
