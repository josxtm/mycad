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
