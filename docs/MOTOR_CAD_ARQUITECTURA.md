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
