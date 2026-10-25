# Estado de fases de MyCAD 0.6.0

Revisión: 2026-10-25. **Progreso global estimado: 42 % antes de este lote**. Es juicio de ingeniería sobre el objetivo CAD completo, no dato calculado por GitHub ni equivalencia a madurez comercial. El porcentaje no implica certificación funcional en teléfono.

| Fase | Estado | Alcance / limitación |
|---|---|---|
| Primitivas prismáticas y cilíndricas | Base implementada | Mallas trianguladas y transformaciones XYZ |
| Navegación, zoom, ViewCube | Parcial | ViewCube y selección avanzada precisan más trabajo |
| Bocetos en XY/XZ/YZ y cotas | Parcial | Falta solver de restricciones generales |
| Extrusión 2D→3D | Básica | Parte de los vínculos son prototipos |
| Guardado, apertura, deshacer/rehacer | Base implementada | JSON v1 y migración pendiente |
| Intercambio STL/OBJ | Básico | No son formatos sólidos paramétricos |
| Booleanas entre prismas rectangulares | Limitadas | Cajas sin giros en operaciones principales |
| Historial encadenado y recetas booleanas | Parcial | No existe árbol de features general |
| Cilindros, taladros XYZ ciegos/pasantes | Base implementada | Triangulación poligonal, no B-Rep |
| Selección de caras/aristas | Parcial | Sin IDs topológicos persistentes |
| CI + regresiones geométricas | Activo | Compilar no equivale a validar en Nothing Phone 2a |

## Fases principales pendientes

1. **Booleanas curvas generales**: unión, corte e intersección para sólidos de geometría arbitraria. Este lote añade un **subconjunto delimitado** entre caja y cilindro, cuando el cutter está alineado con uno de los ejes locales, cabe en la sección y entra por la cara positiva. Corte y previsualización de intersección; no sustituye un motor general.
2. **Topología B-Rep** con caras, aristas, vértices y referencias estables.
3. **Historial paramétrico completo**: dependencias, inserción y reconstrucción ante cambios topológicos.
4. **Sketch solver**: restricciones geométricas, perfiles complejos, snapping robusto.
5. **Redondeos y chaflanes** verdaderos, shell, patrones, loft y operaciones avanzadas.
6. **Selección y edición 3D táctil**: ray casting profundo, aristas, ViewCube completo, manipulación con gestos.
7. **Persistencia versionada del motor nuevo** e interoperabilidad CAD sólida además de STL/OBJ.
8. **Calidad y rendimiento** en geometría compleja; pruebas en Nothing Phone 2a y cierre de errores.

## Nueva etapa en este lote: prismas y cilindros

- `RigidTransform.inversePoint` permite analizar un cortador en coordenadas locales del prisma, incluso si los dos cuerpos están girados en mundo.
- `CylinderBoxBoolean.calculate` identifica si un cilindro ocupa un eje local X/Y/Z del prisma, cabe completamente en su cara transversal y atraviesa la cara **positiva**. La operación devuelve una receta editable `DrilledBoxRecipe` para la diferencia y una malla cerrada de intersección.
- Menú Android `Proyecto → Booleanas prisma + cilindro`: seleccionar prisma, elegir cilindro, previsualizar corte/intersección y guardar. Guardar consume los dos cuerpos originales, con deshacer. Los cortes se conservan como recetas editables; las intersecciones guardadas son mallas sin historial de cilindro.
- Se corrige la posición espacial de taladros X/Y, cuyo mapeo local anteriormente desplazaba el centro en Z media altura.
- Las pruebas abarcan volumen, manifold, transformación inversa, taladros espaciales y rechazos de casos fuera del subconjunto. **No se admiten** cilindros inclinados respecto de los ejes del prisma, cilindros que cruzan un lateral, unión general ni diferencias con entradas por la cara negativa.
- Es un incremento de alcance restringido y no cambia la estimación global de 42 % sin validación del teléfono y criterios más amplios de aceptación.

## Lote de topología lógica para primitivas — selección 0.6.0

- **IDs de cuerpos persistentes**: cada `Part` posee ahora `bodyId` UUID, guardado como campo opcional en el JSON v1. Los proyectos anteriores generan un UUID al abrirse; al guardar conservan esa identidad. Editar medidas conserva el ID; duplicados, valores malformados y colisiones se rechazan en importación. Deshacer y rehacer utilizan el mismo ID del snapshot.
- **Mapa de caras estable para primitivas**: `PrimitiveTopologyMap` convierte índices de triángulos GPU a referencias `<bodyId>/face/<tipo>`, independientes del orden de las piezas y de la resolución de teselado. Reconoce 6 caras de prismas y **tapa/base/lateral** de cilindros de 48 segmentos. No asigna topología a mallas booleanas/importadas.
- **Selección segura**: `CadFaceReference` conserva UUID opcional y comprueba el cuerpo antes de editar una cara; evita modificar por accidente otro prisma si cambió el índice. El visor muestra selección de cilindros con región lógica (sin permitir todavía editar directamente las superficies curvas).
- **Aristas**: `PrimitiveEdges` enumera 12 ranuras geométricas de prismas, con claves estables `<bodyId>/edge/<eje>_<índice>`, longitud y extremos XYZ mundiales, incluidos giros.
- **Error corregido**: el menú de extrusión ya no usa `Face.values()[n]` (orden incompatible con las etiquetas), sino `Face.valueOf(faces[n])` y respeta la cara elegida.
- **Pruebas**: `PrimitiveTopologyMapTest`, `PrimitiveEdgesTest` e integridad de proyecto se suman a GitHub Actions; cubren orden de triángulos, claves, reordenación, selección de cilindros, cambios de resolución, duplicados, giros y aristas.

**Alcance limitado**: esto constituye *identificación lógica de caras y aristas de primitivas*, NO un kernel B-Rep, ray casting completo ni denominación topológica estable tras booleanas arbitrarias, particiones o fusiones. Falta validación táctil en Nothing Phone 2a. El avance global sigue estimado en **~42 %**, sin aumentar por cambios aislados no validados.

## Fase de historial paramétrico: UUID estables y dependencia de operaciones

**Incremento funcional:**

- `FaceExtrusionChain` dispone de un UUID permanente de cadena; cada `Step` incorpora también UUID propio. Editar una distancia conserva el identificador de ese paso; duplicados dentro de una cadena se rechazan, con límite de 50 pasos.
- `ChainBooleanFeature` añade `chainId` opcional, que localiza la cadena origen por identidad y no únicamente por su posición en la lista. Las modificaciones de la operación o sus seis parámetros conservan ese vínculo. Si se elimina la cadena original, la operación falla explícitamente: nunca se redirige silenciosamente a otra cadena.
- El JSON v1 escribe `extrusionChains[].id`, `steps[].id` y `chainBooleanFeatures[].chainId`. **Archivos antiguos**: se asignan UUIDs durante la importación y las booleanas legadas basadas en índice se vinculan a la cadena identificada en el momento de la migración. Guardar persiste la identidad a partir de entonces.
- `ChainDependencyReport` comprueba UUIDs de cadenas y pasos, duplicados, fallos de reconstrucción y referencias booleanas huérfanas. Se ejecuta al abrir un archivo y está disponible en Android mediante `Proyecto → Estado de dependencias CAD`.
- `Historial paramétrico → Reordenar cadenas sin romper dependencias` permite desplazarlas arriba o abajo conservando los vínculos por UUID y deshacer/rehacer. Editar y eliminar la última extrusión trabaja sobre una copia de la cadena para conservar la identidad y el historial previo.
- La prueba `StableFeatureDependenciesTest` verifica conservación de IDs de pasos, reordenación, referencias huérfanas, duplicados y compatibilidad con recetas basadas en índices anteriores.

**Limitaciones:** el grafo cubre las cadenas rectangulares y booleanas actualmente soportadas, no todas las piezas ni el sistema de bocetos Android. No es una solución B-Rep de nombres topológicos tras divisiones, fusiones o redondeos. La compilación CI no sustituye la prueba de reconstrucción y los diálogos táctiles en el Nothing Phone 2a. El avance del objetivo CAD completo sigue estimado en ~42 %, a la espera de hito funcional más amplio.

## Fase: reconstrucción atómica boceto → sólido y auditoría de vínculos

- **Módulo independiente `LinkedSketchSolidPlan`:** calcula geometría de cuerpos dependientes desde bocetos rectangulares o circulares en planos XY/XZ/YZ, con valores en mm, altura de extrusión validada y transformaciones de posicionamiento equivalentes a la interfaz actual. Evalúa todos los cuerpos afectados **antes** de cambiar la geometría existente; si falla alguno, se rechaza el cambio.
- **Integración Android:** `makeExtrudedPart` utiliza el planificador en lugar de fórmulas duplicadas; `rebuildLinkedParts` prepara todas las sustituciones y luego aplica actualizaciones conservando identidad UUID. La acotación del círculo recibe la misma validación previa que el rectángulo. Se mantiene la posibilidad de editar líneas abiertas sin extruirlas.
- **Edición independiente sin efecto al cancelar:** ya no se rompe el vínculo del cuerpo al abrir el formulario; se desvincula únicamente si se confirma una edición manual. El ID permanente del cuerpo se conserva.
- **`LinkedSketchAudit`:** diagnóstico independiente que detecta bocetos duplicados, cuerpos sin ID, referencias a bocetos inexistentes, extrusiones incompatibles y cuerpos cuyas medidas/posición difieren del boceto fuente. Las diferencias se indican como advertencias, sin modificar o bloquear automáticamente documentos anteriores.
- **Nuevo menú:** `Proyecto → Auditar vínculos de bocetos`, con recuentos de perfiles, cuerpos vinculados y errores detectados. No modifica el proyecto.
- **Regresiones CI:** `LinkedSketchSolidPlanTest` y `LinkedSketchAuditTest`: validación de rectángulo/círculo en 3 planos, múltiples dependientes, rechazados atómicos, volumen, IDs duplicados, referencias huérfanas y divergencia geométrica. Ejecutadas antes de Gradle.

**Limitaciones:** el puente solamente maneja perfiles circulares y rectangulares básicos. No implementa solver de restricciones, splines, esquinas redondeadas, extrusión de perfiles arbitrarios ni un kernel B-Rep general. La validación automática de compilación no sustituye la comprobación de gestos y diálogos en un teléfono real. Avance global de motor CAD avanzado: estimación **~42 %** hasta lograr un hito funcional más amplio.

## Lote de control de dependencias: borrado seguro de bocetos y recuperación robusta

- **Planificador puro `SketchDeletionPlan`**: inspecciona los cuerpos vinculados al UUID del boceto objetivo sin mutar datos y produce dos alternativas inmutables: conservar los cuerpos desvinculados (`KEEP_SOLIDS`) o eliminar los cuerpos dependientes (`CASCADE_DELETE`). Rechaza IDs de cuerpo duplicados, referencias malformadas y planes que han quedado desactualizados.
- **Interfaz Android**: en el menú **Boceto táctil** aparecen «Eliminar último boceto» y «Eliminar boceto seleccionado». Antes de actuar se muestra la cantidad exacta de extrusiones vinculadas y se elige «Conservar cuerpos», «Eliminar también cuerpos» o «Cancelar». Ambos modos hacen un único snapshot deshacible antes de modificar el proyecto.
- **Selección y escena**: tras eliminar, se mantiene la selección de un cuerpo superviviente por UUID; se corrigen índices de bocetos, se limpia la cara activa y se refresca el visor 3D. Un cuerpo conservado mantiene forma, orientación, dimensiones e identidad, pero pierde su vínculo con el boceto eliminado.
- **Deshacer/rehacer robustos**: la revisión guardada se deserializa y valida antes de hacer `pop()` de la pila de historial. Si el archivo de revisión está corrupto no se destruye la posibilidad de recuperación de otras revisiones.
- **Pruebas**: `SketchDeletionPlanTest` verifica varias extrusiones dependientes, cuerpos ajenos, preservación de UUIDs, eliminación cascada, inmutabilidad y rechazo de planes obsoletos. `verify-project.sh` comprueba que Android utilice el plan, tome snapshot y aplique la operación confirmada.

**Límite:** gestión de dependencias de bocetos y cuerpos primitivos; no elimina ni migra rasgos B-Rep generales, intersecciones arbitrarias o restricciones CAD avanzadas. El progreso estimado de motor CAD sigue en ~42 % mientras no se validen los siguientes hitos en el teléfono.
