# Roadmap de MyCAD

**Principio:** primero comportamiento verificable, después ampliación de herramientas.

| Fase | Alcance | Entregable | Estado |
|---|---|---|---|
| 0.4.8 | Creación de primitivas, movimiento y rotación XYZ | APK experimental | Validado en móvil por el usuario |
| 0.5.0-A | Análisis de referencias y arquitectura | Documentación | En curso |
| 0.5.0-B | Nueva GUI móvil y estados de interacción | Prototipo navegable | Pendiente |
| 0.5.0-C | Bocetos sobre planos XY/XZ/YZ | Editor 2D | Parcial: XY existente; XZ/YZ pendientes |
| 0.5.0-D | Detección de perfiles y cotas | Selección de regiones | Parcial: cotas de rectángulo/círculo y perfiles básicos |
| 0.5.0-E | Extrusión táctil y numérica | Sólido desde boceto | Parcial: extrusión numérica XY; arrastre y preview pendientes |
| 0.5.0-F | Persistencia, migración y pruebas | APK candidata 0.5.0 | Pendiente |
| 0.6.x | Booleanas, selección de caras/aristas | Modelado avanzado | Propuesto |
| 0.7.x | Fillets, chamfers, shell, patrones, historial | Herramientas avanzadas | Propuesto |

## Puertas de calidad

- **Diseño:** casos de uso, estados y controles descritos.
- **Implementación:** código separado por responsabilidades.
- **CI:** compilación y pruebas automáticas satisfactorias.
- **Dispositivo:** comprobación manual en Nothing Phone 2a.
- **Entrega:** changelog, APK y limitaciones conocidas.

Si el motor actual no admite una operación fiable, registrar la limitación y decidir sustitución antes de añadir parches.

## Registro de implementación 2026-10-25

- Commit `2a28e508`: comandos **Acotar perfil** y **Extruir perfil** para rectángulos/círculos del boceto XY; líneas abiertas rechazadas.
- Commit `4f52861f`: código marcado como versión de desarrollo 0.5.0.
- **No considerar 0.5.0 terminada**: faltan planos XZ/YZ, selección contextual completa, extrusión táctil con previsualización, pruebas de migración y validación en móvil.
- Los cuerpos creados a partir de bocetos aún no conservan una relación paramétrica editable con el perfil de origen. El motor sigue siendo experimental.

- Commit `67c291d3`: selector de plano y persistencia de metadatos XY/XZ/YZ; la vista perpendicular y la selección completa siguen pendientes.
- Commit `d76d6bde`: orientación inicial de extrusiones en planos XZ/YZ; requiere validación geométrica y táctil.
- Commit `4433723f`: corrección de captura de variables en diálogo de extrusión tras error de compilación Java.
- No se considera liberada la versión hasta que el último workflow sea satisfactorio y se complete el plan de pruebas.

## Inicio de MyCAD 0.6.0 — 2026-10-25

**Objetivo:** mejorar la edición táctil de bocetos antes de ampliar el motor CAD. Versión en desarrollo, no liberada ni validada en teléfono.

### Primer incremento implementado
- Acotación de **longitud de líneas**: introduce milímetros; se conserva el punto inicial y la dirección de la línea. Valores admitidos: longitud positiva hasta 10 000 mm.
- Conserva historial de deshacer mediante `checkpoint()`, guardado y redibujado.
- Identificación de aplicación y APK actualizada a **0.6.0** (versionCode 60).

### Pendientes prioritarios
1. Comprobar compilación GitHub Actions de los nuevos commits.
2. Validar longitud de línea en móvil: línea diagonal → acotar 50 mm → comprobar orientación y persistencia tras reabrir.
3. Mejorar vista perpendicular y mapeo de gestos al dibujar en XZ/YZ.
4. Extrusión táctil con previsualización y controles de confirmación/cancelación.
5. Pruebas de regresión de 0.5.0 y documentación del resultado.

**Límite conocido:** aún no existe un kernel B-Rep paramétrico equivalente a Shapr3D; la versión 0.6.0 no debe presentarse como completada.

## Continuación 0.6.0 — revisión de planos y CI

- Se añade control de orientación táctil a Scene (vistas predefinidas, arrastre) en la rama main, pendiente de validación en dispositivo.
- Se corrige visualización de entidades de boceto XY/XZ/YZ en vista plana y la colocación de extrusión XZ/YZ. La validación visual y de sólidos está pendiente.
- Se añaden `GeometryMath.java` y `tests/GeometryMathTest.java` (volumen prismático/cilíndrico, mapeo de planos y validación de datos).
- CI ejecuta primero prueba Java independiente, luego `testDebugUnitTest`, `lintDebug`, `assembleDebug` y verifica el nombre del APK antes de publicarlo.
- **No hay verificación confirmada del último run** en esta revisión; no declarar APK compilado ni versión final.
- Próximo bloque: probar resultado de Actions, resolver sus fallos si aparecen, implementar sólidos paramétricos y previsualización de extrusión; conservar el JSON actual.

## Incremento: extrusión interactiva (commit 74111394)

- El cuadro de extrusión crea un **sólido temporal** que se representa en la escena durante la introducción de la altura.
- Campo numérico hasta 10000 mm y deslizador de ajuste rápido entre 1 y 200 mm.
- Cancelar descarta el sólido temporal, restaura la selección y la vista previa, **sin persistir ni registrar un cambio en el historial**.
- Confirmar elimina la previsualización, registra el punto de deshacer y crea el sólido definitivo.
- Extrusiones limitadas a perfiles rectangulares y circulares; sin motor B-Rep ni vínculo paramétrico persistente todavía.
- **Estado: implementado en código; pendiente de confirmar compilación y validación táctil en Nothing Phone 2a.**

## Fase de enlaces paramétricos — 2026-10-25

- Part incorpora `sourceSketchId` y Sketch incorpora `id` persistente (UUID).
- Al crear una extrusión, se guarda el identificador del croquis origen.
- Desde **Editar pieza**, si hay vínculo, se ofrecen: cambiar altura, editar perfil de origen o editar cuerpo sin vínculo.
- Modificar cotas/coordenadas del croquis reconstruye sus cuerpos vinculados conservando la altura.
- Eliminar el croquis de origen desvincula los cuerpos existentes para conservar la geometría.
- `encode/decode` conserva los campos opcionales sin cambiar el formato JSON v1; los archivos anteriores siguen admitidos.
- Falta validar en móvil y comprobar el último workflow. No confundir esta implementación con un kernel B-Rep: booleanas y edición de caras siguen pendientes.
- Prueba recomendada: croquis rectángulo 20×10, extruir 5; modificar a 30×10; comprobar dimensiones 30×10×5; modificar altura a 8; guardar, reabrir y repetir; deshacer y eliminar perfil.

## Fase de persistencia de mallas booleanas — 2026-10-25

- Se incorpora el botón **Guardar malla** tras la previsualización de unión/intersección/resta de dos prismas sin rotación.
- Las mallas guardadas se almacenan como triángulos en el campo opcional `booleanMeshes` de JSON v1, compatible con documentos antiguos.
- Los resultados se recuperan al abrir archivos y borradores, se incluyen en deshacer/rehacer y se borran al crear un diseño nuevo.
- `currentMesh()` incorpora las mallas guardadas a la exportación STL/OBJ, además de los sólidos de origen.
- **Limitación importante:** el resultado se añade como malla independiente; los operandos originales no se ocultan ni eliminan y la malla no es un sólido B-Rep editable. Al exportar toda la escena pueden existir superficies solapadas. No usar sin revisar para fabricación/impresión 3D.
- Estado: código subido, sin verificación de ejecución nueva ni prueba en móvil para esta fase.

## Gestión individual de resultados booleanos — 2026-10-25

- Añadido botón **▤ Resultados booleanos** en la barra derecha.
- Lista de mallas persistidas con acciones **ver por separado**, **exportar únicamente esa malla en STL/OBJ** y **eliminar resultado** con confirmación.
- Los prismas operandos no se modifican. El borrado se registra en deshacer y se persiste.
- Se limpia la malla pendiente si se cancela el selector de exportación, para evitar exportar el resultado incorrecto posteriormente.
- El visor GPU muestra una sola malla cuando se selecciona esa acción; **no** se ha implementado todavía su selección/edición topológica en el lienzo principal.
- Estado: cambios subidos a GitHub; compilación y prueba funcional de este incremento pendientes de comprobar.

## Inspección de caras de primitivas — 2026-10-25

- Nueva acción **▦ Inspeccionar caras** en la barra derecha del editor.
- Requiere seleccionar previamente un cuerpo; lista seis caras nominales en prismas y tres regiones en cilindros.
- Para cada cara plana de prisma muestra área nominal en mm², con las dimensiones del sólido.
- No se seleccionan caras tocándolas directamente en el visor y no se modifican sólidos; todavía no existe selección de aristas ni topología B-Rep.
- Estado: código subido, compilación y prueba de esta etapa pendientes de confirmar.

## Inspección de aristas — 2026-10-25

- Nuevo botón **⌑ Inspeccionar aristas** en la barra derecha.
- Para prismas se enumeran las 12 aristas locales agrupadas por ejes X/Y/Z y se muestra su longitud en mm.
- La selección de arista es por lista; aún no dispone de picking táctil directo ni resaltado 3D de aristas.
- Los cilindros no se incluyen en esta etapa. La inspección no modifica la geometría.
- Estado: código incorporado; pendiente de comprobar el workflow y pruebas en teléfono.
