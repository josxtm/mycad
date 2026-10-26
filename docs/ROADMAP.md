# Roadmap de MyCAD

**Principio:** primero comportamiento verificable, después ampliación de herramientas.

**Extrusión de contornos cerrados con líneas (0.6.0):** permite
seleccionar una línea del boceto y detectar su circuito conectado de
segmentos rectos en XY, XZ o YZ. Triangulación por recorte de orejas
para polígonos cóncavos, paredes y tapas orientadas, con verificación
de estanqueidad. Los perfiles abiertos, ramificados, autointersectados
y con agujeros no admitidos se rechazan. La salida es una **malla STL/OBJ
persistida en el proyecto y deshacible**, no un sólido B-Rep editable
ni una extrusión que se regenere al modificar el boceto. Pruebas Java
de las tres orientaciones y perfiles cóncavos; Android CI y Lint antes
de APK. Véase [extrusión de contornos](CAD_CLOSED_LINE_EXTRUSION.md).

**Bocetos DXF 2D XY (0.6.0):** importación y exportación ASCII DXF
en milímetros de líneas, círculos y rectángulos ortogonales (polilínea
LWPOLYLINE cerrada de cuatro vértices). La importación valida todas las
entidades y añade perfiles en una sola operación deshacible, sin sustituir
el diseño original; rechaza geometría DXF no compatible y unidades
distintas de mm (o archivos sin unidad). XZ/YZ no se proyectan ni
exportan silenciosamente, y las piezas 3D no se convierten en DXF.
Pruebas de ida/vuelta, unidades, errores y límites de tamaño en CI.
No es un importador DXF completo, ni soporta splines, arcos o cotas
DXF. Sin APK en este incremento. Véase
[intercambio DXF limitado](CAD_DXF_SKETCH_XY.md).

**Raycast independiente de la escala (0.6.0):** el selector de triángulos compara el determinante con el producto de las longitudes de sus aristas, en lugar de usar un mínimo absoluto que ocultaba caras de 1 µm. Se prueban impactos, fallos y equivalencia con el índice BVH; sin APK ni validación táctil. Véase [selección a escala](CAD_RAY_PICKING_SCALE.md).

**Áreas degeneradas independientes de escala (0.6.0):** se elimina el umbral absoluto de área que rechazaba triángulos legítimos de sólidos diminutos o alargados; se detectan como degenerados solo triángulos de área exactamente nula y esquinas repetidas, manteniendo la comprobación de volumen por componente. Regresiones con tetraedros de 1 µm y altas relaciones de aspecto. Sin APK; véase [áreas a escala](CAD_MESH_SCALE_DEGENERACY.md).

**Rechazo de componentes sin volumen (0.6.0):** el validador de sólidos comprueba el volumen orientado de cada componente de caras conectadas por aristas; una lámina coplanar de doble cara puede tener aristas emparejadas y aun así no encerrar sólido. Se rechaza la componente de volumen exactamente cero, incluso si otra componente válida aporta volumen total positivo. Se exponen contadores de componentes y componentes nulas; regresiones Java, sin APK. Véase [mallas sin volumen](CAD_MESH_ZERO_VOLUME.md).

**Precisión del volumen orientado (0.6.0):** el diagnóstico de mallas calcula determinantes respecto al primer vértice de cada componente de caras conectadas por aristas y acumula aportaciones con compensación de Neumaier; regresiones de tetraedros pequeños separados a ±10 millones de mm y orientación invertida. Sin cambios en la topología ni APK; véase [volumen estable](CAD_MESH_STABLE_VOLUME.md).

**Integridad topológica de mallas (incremento 0.6.0):** la validación de
mallas cerradas ahora rechaza vértices con varios abanicos desconectados
(dos sólidos que solo se tocan en un punto), además de aristas abiertas,
invertidas o no manifold. Incluye regresiones con tetraedros y no cambia
la política de APK por hitos. Véase [validador de vértices](CAD_MESH_VERTEX_MANIFOLD.md).

## Estado vigente — 0.6.0 (26 de octubre de 2026)

**ViewCube táctil visible dentro de OpenGL:** el visor GPU dispone de un control 3×3 superpuesto (SUP/INF/IZQ/DER/FREN/ISO), pulsable sin regresar al lienzo. Centraliza la creación del visor y comparte el motor de ángulos del ViewCube original; la superposición se oculta al cerrar el visor y no bloquea gestos fuera de su esquina. CI Android y verificación de integración, sin APK. Véase [ViewCube superpuesto](CAD_VIEWCUBE_GPU_OVERLAY.md).

**ViewCube y visor GPU sincronizados en ambos sentidos:** girar con un dedo en OpenGL o ejecutar el gesto de tres dedos actualiza también la orientación del cubo del lienzo (radianes/grados, yaw normalizado y pitch limitado). Se evita reentrar en la cámara desde el callback; tests Java y compilación Android en CI, sin APK.

**Raycast analítico estable en coordenadas grandes:** la dirección del rayo se transforma solo por rotación, sin restar dos puntos mundiales de gran magnitud. Evita cancelación numérica al seleccionar caras B-Rep lejos del origen dentro del límite actual de 10 000 mm por eje; prueba matemática adicional de rotación con traslaciones de 10¹² mm. CI Java/Android, sin APK ni validación táctil.

**Selección jerárquica de mallas grandes:** un árbol AABB binario sobre bloques de 64 triángulos permite descartar subárboles completos durante el raycast. Conserva índices globales y desempates; regresiones frente al raycast lineal con 96 bloques y 250 rayos deterministas. Sin APK ni pruebas de rendimiento en móvil. Véase [índice jerárquico](CAD_MESH_RAY_BVH.md).

**Tres dedos → vista inicial 3D:** toque simultáneo rápido de tres dedos restablece orientación isométrica (35°/25°), centro y distancia adaptados al sólido. El gesto no se confunde con pellizco ni con selección de caras, y cancela arrastre de gizmos. Pruebas Java/CI Android, sin APK.

**Picking de mallas por cercanía:** el índice AABB visita antes los bloques más próximos al rayo y evita comprobar triángulos de bloques posteriores cuando ya hay un impacto más cercano. Conserva empates por índice de triángulo; pruebas de igualdad y rayos pseudoaleatorios. Sin APK.

**Selección de mallas grandes acelerada:** el raycast agrupa los triángulos en bloques de 64 con cotas 3D cacheadas; se comprueban por rayos solo los bloques atravesados, sin alterar el triángulo global, el cuerpo visible ni las referencias B-Rep. Pruebas comparativas Java frente al motor previo, Gradle/Lint, sin APK.

**Toques CAD sin giro accidental:** el visor GPU ya no rota la cámara por el pequeño temblor del dedo al seleccionar una cara/arista; el giro empieza al superar 12dp. Se comprueba también la posición final de `ACTION_UP` para no confundir un arrastre con un toque. Tests Java y CI Android sin APK.

**Cámara estable durante la edición:** al confirmar una cara/arista, deshacer, guardar o regenerar, el visor GPU actualiza triángulos sin reajustar automáticamente el centro y zoom; conserva el encuadre elegido. Al abrir otro resultado aislado o ver por primera vez un sólido sí se encuadra. Tests y CI Android, sin APK.

**Corrección crítica de picking GPU:** se restaura la selección táctil de superficies en el visor 3D cuando solo está registrado el callback moderno de rayos. El `ACTION_UP` anterior exigía por error el callback legado y podía impedir seleccionar caras y aristas. Reglas de regresión y tests Java, sin APK.

**Tolerancia de aristas por profundidad del toque:** el radio de 12 dp
se calcula con la distancia del rayo al punto 3D tocado, no solo con
la distancia de cámara al foco. Reduce variaciones de selección entre
piezas cercanas y lejanas de una misma escena; pruebas Java y Android
en CI, sin APK. Véase [tolerancia visual](CAD_BREP_EDGE_SCREEN_TOLERANCE.md).

**Selección de aristas vinculada al rayo exacto:** el visor pasa el impacto
analítico completo al selector de bordes y este comprueba que la geometría
original (cuerpo, dimensiones y pose XYZ) siga siendo válida. Se conserva
la alternativa por triángulo cuando no hay refinamiento. Pruebas Java y
Android en CI, sin APK ni validación táctil; véase
[selección exacta](CAD_BREP_ANALYTIC_RAY_PICKING.md).

**Asa de arista en el punto realmente tocado:** el selector conserva
la posición exacta sobre la arista y el control de arrastre la reutiliza
si la geometría sigue idéntica. En cilindros la normal radial gira con
el punto tocado; el inspector mantiene un asa nominal. Pruebas Java y
Android en CI sin APK; véase
[anclaje táctil de aristas](CAD_BREP_EDGE_DRAG.md).


**Selección de aristas según la cara visible:** el visor restringe la
clasificación analítica a las aristas incidentes de la cara seleccionada
por la malla/refinamiento analítico. Evita escoger bordes de otra cara
solo por proximidad geométrica; conserva IDs estables y el umbral por
zoom. Regresiones Java para prismas y cilindros girados; sin APK ni
validación táctil. Véase
[contorno de cara visible](CAD_BREP_VISIBLE_FACE_EDGES.md).


**Gizmo 3D en la cara realmente tocada:** el raycast analítico devuelve normales
mundiales exactas y el arrastre de caras usa el impacto aceptado del visor
para colocar el manipulador, también en cilindros girados. Se rechazan
puntos obsoletos tras cambiar la geometría y se mantiene el anclaje al centro
como alternativa. Se refuerza el rechazo de rayos casi tangentes exteriores.
Pruebas Java/Android en CI, sin APK; véase
[anclaje analítico de controles](CAD_BREP_SURFACE_HIT_HANDLES.md).

**ViewCube vinculado a OpenGL:** pulsar una cara/dirección del cubo de orientación o arrastrarlo actualiza la cámara del visor 3D GPU, además del lienzo. Se conserva el centro/zoom del visor y se limitan las vistas verticales; CI Java y Android sin APK.

**Optimización GPU de navegación:** los buffers directos de triángulos se preparan una vez por malla y se reutilizan en cada fotograma de órbita/zoom. Se invalidan al cambiar la escena, previsualizar o recuperar contexto EGL; tests Java de memoria/cursor y verificación Android sin APK.

**Navegación multitáctil mejorada:** dos dedos permiten zoom por separación y desplazamiento del foco 3D por el punto medio; un dedo conserva la órbita. Se cancela un manipulador si comienza multitáctil. Matemática XYZ probada sin Android; sensibilidad pendiente de dispositivo.

**Picking B-Rep analítico:** intersección exacta de rayos con seis caras de prismas y superficies cilíndricas/tapas, incluso bajo rotaciones XYZ. La malla determina primero el cuerpo visible y el rayo refina el punto y la cara cuando la aproximación coincide; no es picking B-Rep general. Pruebas Java y CI Android sin APK.

**Unificación de mallas XYZ:** el renderizado de prismas y cilindros emplea ahora la misma geometría B-Rep analítica en vistas normales y experimentales, exportación STL/OBJ y previsualización. Evita que las piezas con rotaciones X/Y pierdan orientación en la vista clásica. Concatenación segura de mallas con límite de memoria y tests Java; APK pendiente de hito.

**Picking de superficies reforzado:** raycasting con dirección normalizada, distancia física en mm y descarte seguro de triángulos no finitos o degenerados; mantiene índices originales y pruebas de regresión.

**Nuevo imán de precisión:** movimiento de caras/aristas con arrastre libre o incrementos de 0,1 / 0,5 / 1 mm, simétrico en sentidos positivos y negativos y con límite geométrico. Preferencia local persistente; pruebas sin APK.

**Prototipo de arrastre de aristas:** elegir borde B-Rep y una de sus dos normales incidentes para moverlo directamente en el visor con vista previa, confirmación y deshacer; la otra dirección permanece fija. No equivale a filetes ni deformación libre.

**Prototipo de arrastre sobre sólido (sin APK):** cara B-Rep seleccionada con punto/asa proyectada al visor GPU, arrastre táctil restringido a su normal mundial, vista previa reversible, confirmación con undo/persistencia solo al aceptar. Requiere calibración táctil y visual en Nothing Phone 2a.

**Picking sensible al zoom:** la distancia de selección de aristas ahora deriva de 12dp de tolerancia visual y del tamaño/proyección de cada cuerpo, acotada para evitar selecciones lejanas. Mejora la interacción al variar el zoom; validación táctil en Nothing Phone 2a pendiente.

**Selección visual de aristas:** el visor OpenGL destaca toda la arista matemática (segmento recto o circunferencia), con aproximación de línea independiente de los triángulos; se limpia con el cambio de selección o previsualización. Pendiente validación táctil real.

**Vista previa geométrica en edición:** deslizador(es) táctiles para caras y aristas de primitivas con recalculado de geometría inmutable, actualización GPU sin cambiar la escena guardada, confirmación mediante checkpoint y restauración al cancelar. Verificación en Android físico pendiente.

**Incremento de edición:** desplazamiento numérico atómico de aristas de prismas/cilindros, limitado a las dos caras adyacentes, con IDs estables, controles de selección obsoleta y deshacer. No son filetes/chaflanes ni arrastre gestual de aristas; estos siguen pendientes.

**Nuevo bloque (26/10):** selección de aristas analíticas en puntos de impacto del visor GPU, comprobando distancia a segmentos de prismas y circunferencias exactas de cilindros. Es selección asistida por un hit de malla, no raycasting directo de bordes ni manipulación táctil de aristas. CI Java independiente, sin APK.

**Código en desarrollo; NO se ha publicado APK 0.6.0 ni validado completamente en Nothing Phone 2a.**

| Bloque | Estado del código | Próxima puerta |
|---|---|---|
| Bocetos y extrusiones | Implementación inicial y vínculos paramétricos parciales | Pruebas táctiles + integración completa |
| Primitivas 3D | Prismas y cilindros con transformación rígida XYZ | Validar casos extremos |
| Identidad y dependencias | UUID estables, DAG y regeneración incremental parcial | Reconstrucción consistente del proyecto completo |
| B-Rep analítica | 8 vértices/12 aristas/6 caras de prismas; 3 caras y 2 circunferencias de cilindros | Edición topológica general |
| Edición de caras | Cambios geométricos de caras en visor experimental | Verificar undo/redo, guardar/reabrir y rendimiento en teléfono |
| Booleanas | Operaciones específicas y mallas; NO kernel B-Rep general | Generalización geométrica y control topológico |
| CI | [#320](https://github.com/josxtm/mycad/actions/runs/40244844784), [#321](https://github.com/josxtm/mycad/actions/runs/40245317064) y [#322](https://github.com/josxtm/mycad/actions/runs/40245497754) correctas | PR obligatoria mediante protección de `main` |
| APK 0.6.0 | No generada | Hito funcional validado y decisión explícita |

**Norma de desarrollo:** rama → Pull Request → job `verify` correcto → revisión → merge a `main`. Las APK se generan solo en hitos. Las reglas de protección de `main` deben activarse por separado en los ajustes de GitHub; [CONTRIBUTING.md](../CONTRIBUTING.md) indica cómo.

**Nota histórica:** las notas y tablas posteriores conservan el contexto de fases previas y pueden contener estados ya superados; no deben interpretarse como la situación actual del motor.


| Fase | Alcance | Entregable | Estado |
|---|---|---|---|
| 0.4.8 | Creación de primitivas, movimiento y rotación XYZ | APK experimental | Validado en móvil por el usuario |
| 0.5.0-A | Análisis de referencias y arquitectura | Documentación | En curso |
| 0.5.0-B | Nueva GUI móvil y estados de interacción | Prototipo navegable | Pendiente |
| 0.5.0-C | Bocetos sobre planos XY/XZ/YZ | Editor 2D | Parcial: XY existente; XZ/YZ pendientes |
| 0.5.0-D | Detección de perfiles y cotas | Selección de regiones | Parcial: cotas de rectángulo/círculo y perfiles básicos |
| 0.5.0-E | Extrusión táctil y numérica | Sólido desde boceto | Parcial: extrusión numérica XY; arrastre y preview pendientes |
| 0.5.0-F | Persistencia, migración y pruebas | APK candidata 0.5.0 | Pendiente |
| 0.6.x | Booleanas, B-Rep de primitivas y selección/edición de caras | Motor CAD experimental | Parcialmente implementado; CI correcta, móvil pendiente |
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

## Selección táctil de caras planas — 2026-10-25

- En modo **NAVEGAR** (visor 3D Canvas), tocar una cara de un prisma la selecciona y resalta con contorno amarillo.
- Picking por proyección de polígonos y profundidad media; se elige la cara proyectada más cercana. Compatible con la rotación de primitivas.
- La selección es informativa y no cambia la geometría; los cilindros, mallas importadas y mallas booleanas no admiten todavía picking facial.
- En 2D y modos de movimiento/edición se mantiene el funcionamiento existente. Queda pendiente selección precisa por ray casting para modelos complejos y detección de aristas.
- Compilación de este incremento aún por verificar en la rama `ci-status`.
