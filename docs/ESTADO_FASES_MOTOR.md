# Estado de fases de MyCAD 0.6.0

Revisión: 2026-10-26. **Progreso global estimado: 42 % antes de este lote**. Es juicio de ingeniería sobre el objetivo CAD completo, no dato calculado por GitHub ni equivalencia a madurez comercial. El porcentaje no implica certificación funcional en teléfono.

| Fase | Estado | Alcance / limitación |
|---|---|---|
| Primitivas prismáticas y cilíndricas | Base implementada | Mallas trianguladas y transformaciones XYZ |
| Navegación, zoom, ViewCube | Parcial | ViewCube y selección avanzada precisan más trabajo |
| Bocetos en XY/XZ/YZ y cotas | Parcial | Falta solver de restricciones generales |
| Extrusión 2D→3D | Primer sólido poligonal B-Rep | Contornos XY/XZ/YZ con caras planas analíticas, semiaristas compartidas e IDs estables; booleanas y superficies libres pendientes |
| Guardado, apertura, deshacer/rehacer | Base implementada | JSON v1 y migración pendiente |
| Intercambio STL/OBJ | Básico | No son formatos sólidos paramétricos |
| DXF de bocetos 2D | Subconjunto XY integrado | LINE, CIRCLE y rectángulos ortogonales; no DXF 3D ni splines/arcos |
| Booleanas entre sólidos B-Rep | Intersección convexa inicial | Dos extrusiones poligonales del mismo plano y sentido; sólidos originales intactos, resultado B-Rep analítico exportado como malla independiente; unión/diferencia pendientes |
| Historial encadenado y recetas booleanas | Parcial | No existe árbol de features general |
| Cilindros, taladros XYZ ciegos/pasantes | Base implementada | Triangulación poligonal, no B-Rep |
| Selección de caras/aristas | Edición analítica parcial de caras B-Rep | El visor resuelve caras estables; tapas editan altura, laterales permiten offset en UV con regeneración atómica; operaciones generales pendientes |
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

## Bloque: grafo de rasgos paramétricos e impactos (CI sin APK)

- `ParametricFeatureGraph` incorpora un **DAG** de rasgos con cuatro tipos: `SKETCH`, `BODY`, `CHAIN` y `BOOLEAN`. Las referencias son UUID persistentes; se rechazan nodos duplicados, dependencias huérfanas y ciclos, y se obtiene un orden de ejecución padre → hijos. `affectedBy(id)` devuelve, en orden seguro, los dependientes transitivos de un rasgo que cambie.
- Cada `ChainBooleanFeature` dispone ahora de UUID propio, preservado tras cambiar sus cotas, parámetros del cortador o tipo de booleana. Se guarda en `chainBooleanFeatures[].id`; al importar documentos antiguos se le asigna automáticamente uno nuevo. `ChainDependencyReport` detecta duplicados.
- En Android, `Proyecto → Árbol paramétrico e impactos` enseña los rasgos y permite consultar los dependientes de un boceto o una cadena. **Diagnóstico solamente**: en esta fase el DAG no ejecuta booleanas arbitrarias ni sustituye el kernel de malla. Los cambios de perfil ya regeneran sus cuerpos básicos por los mecanismos específicos anteriores; una cadena puede regenerar sus booleanas.
- `ParametricFeatureGraphTest` verifica orden, propagación transitiva, orfandad, ciclos, identidad y resultados inmutables. La integración está protegida por las pruebas de CI y el verificador estático.
- Se corrige la política de APK tras la ejecución #315: esta había empaquetado porque el texto `[apk]` aparecía en el cuerpo de un merge. Ahora solo se activa automáticamente si **la primera línea del commit empieza por `release: MyCAD `** o hay petición manual explícita mediante `generar_apk=true`. Los merges normales mantienen tests Android, Lint y geometría sin ensamblar una APK.

**Limitaciones:** no hay B-Rep ni regeneración genérica de todo tipo de operaciones a partir del DAG; es la infraestructura de ordenación, auditoría e impacto para fases posteriores. El porcentaje global continúa estimado en ~42 %, a validar en pruebas de dispositivo.

## Bloque: regeneración incremental y atómica de booleanas encadenadas

- **`ChainBooleanRebuildCache`** compara la firma de las cadenas (UUID, cuerpo raíz, orden/IDs de pasos, caras y distancias) y de cada operación booleana (UUID, tipo, cortador XYZ y dimensiones). Una modificación de cadena invalida todas las booleanas que dependen de ella; una edición del cortador invalida solo esa operación. Las referencias siguen funcionando si se cambia el orden de las cadenas y los rasgos.
- **Resultados atómicos:** se calculan y comprueban todas las mallas nuevas antes de publicar un nuevo `State`. Si una booleana produce geometría vacía, contiene números no finitos, se supera el límite o se rompe un vínculo, no se publica una reconstrucción parcial; se conserva el `State` válido anterior. Los arrays son defensivos, evitando que la interfaz modifique accidentalmente una malla guardada en caché.
- **Trabajo fuera del hilo UI:** `Proyecto → Reconstruir booleanas encadenadas` copia las cadenas y booleanas, calcula en un hilo separado y comprueba que el proyecto no haya cambiado antes de mostrar una **vista previa temporal aislada**. La vista previa reúne varias mallas para visualización, **no es una unión sólida topológica** ni se guarda una pieza nueva. Reporta cuántas operaciones fueron recalculadas/reutilizadas y número total de triángulos.
- **Seguridad:** máximo de 250.000 triángulos para cada resultado y 350.000 en la vista previa conjunta. La selección de la escena no se aplica al visor temporal. No se añade nada a `booleanMeshes` ni al historial al pulsar Regenerar.
- **Pruebas Java CI:** `ChainBooleanRebuildCacheTest` comprueba reconstrucción inicial, reutilización, invalidación localizada por cadena, reordenación por UUID, ediciones, eliminación de operaciones, inmutabilidad, rechazo de referencias rotas/UUID duplicados, fallo atómico y protección frente a vistas previas obsoletas.
- **Sin APK de este bloque:** solamente integridad, pruebas geométricas, compilación de código Android y Lint. La APK continuará reservada para hitos de prueba.

**Alcance:** solo las operaciones booleanas actuales basadas en cadenas rectangulares. No incluye B-Rep ni operaciones curva-curva generales, tampoco regenera un ensamble de cuerpos con aristas topológicas persistentes. Estimación global del motor CAD avanzado: **~42 %** hasta completar hitos mayores y validación móvil.
