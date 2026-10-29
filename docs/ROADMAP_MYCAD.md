# MyCAD: hoja de ruta y evidencia

Actualizado: 2026-10-29. Repositorio exclusivo: `josxtm/mycad`.
No se declara equivalencia funcional con Shapr3D ni validación física sin pruebas.

## Prioridad inmediata: PR #116 / ejecución #550

- Base examinada: `c1530a193933b26d58001d83ffc40047b390b6eb`.
- Ejecución: https://github.com/josxtm/mycad/actions/runs/40600256527
- Causa comprobada en logs: siete errores de compilación porque el comando
  `javac` de CI omitía `SteppedCoplanarPatchTopology.java`, referenciada por
  `SteppedCoplanarFaceGroupsTest`. No era todavía evidencia de geometría incorrecta.
- Corrección: compilar conjuntamente grupos, topología de parches y su prueba.
- Verificación: integridad y suite geométrica completas locales; compilación Android y Lint locales correctos. `testDebugUnitTest` informa NO-SOURCE (no hay pruebas Android de unidad). [Actions #551](https://github.com/josxtm/mycad/actions/runs/40603360477), commit `bc51bcb`, finalizó en verde. PR #116 corregida, sin fusionar.
- No fusionar con verificaciones fallidas; no modificar directamente `main`.

## Auditoría inicial del código

| Área | Evidencia de implementación | Qué falta verificar o completar |
|---|---|---|
| Bocetos y cotas | `MainActivity.dimensionSketch`, `ParametricSketchDocument`, `SketchEditValidator` | Solver general de restricciones; interacción física |
| Boceto → sólido | `extrudeSketch`, `LinkedSketchSolidPlan`, `LoopExtrusionFeature` | El rectángulo de UI usa primitivas; contornos de líneas tienen una ruta B-Rep distinta |
| Push/Pull | Vista previa con deslizador en `extrudeSketch`; arrastre de caras/aristas en visor GPU | Extruir un perfil directamente con dedo sobre el lienzo, sin depender del diálogo |
| B-Rep | Prismas, primitivas analíticas, booleanas ortogonales y alturas variables; parches coplanares | Kernel general de curvas/superficies, consolidación editable de parches |
| Cámara | ViewCube GPU, presets y políticas de gestos con pruebas Java | Validación móvil/tablet y sincronización durante el flujo completo |
| Selección/edición | Picking por rayos, caras/aristas estables y cambios de altura/offset | Referencias estables ante divisiones/fusiones generales |
| Booleanas | Unión/resta/intersección restringidas y recetas UUID regenerables | Booleanas 3D arbitrarias, curvas y casos fuera del subconjunto admitido |
| Historial | `ParametricFeatureGraph`, cadenas y regeneración atómica | Unificar todos los rasgos bajo historial ejecutable |
| Guardado/apertura | JSON en `MainActivity`, reconstrucción de recetas | Roundtrip Android del hito exacto y migraciones completas |
| Formatos | STL/OBJ en `MeshIO`; DXF de bocetos en `SketchDxfIO` | Cobertura de entidades DXF y aceptación de archivos externos |
| UI adaptable | Controles táctiles y barras existentes | Pruebas de accesibilidad, teclado, móvil/tablet |
| Herramientas avanzadas | No se consideran terminadas | Investigar e implementar Revolve, Sweep, Loft, Shell, Fillet y Chamfer por subconjuntos verificables |

Los documentos anteriores contienen estados históricos (incluidas estimaciones de
porcentaje); no se usan como evidencia de aceptación del estado actual.

## PR pendientes revisadas, sin fusionar

- [#80](https://github.com/josxtm/mycad/pull/80): barra inferior desplazable y
  protocolo tablet. El cambio de `button()` afecta a todos sus llamadores,
  no solo a la barra; comprobar anchura, recortes y accesibilidad en ambos formatos.
  La propia PR mantiene pendiente validación física. Conservar en borrador.
- [#86](https://github.com/josxtm/mycad/pull/86): unión convexa de prismas de igual
  altura firmada, con pruebas. Existe ya `ConvexPrismBRepHullUnion` en la base,
  con objetivo solapado y distinta política para contacto por arista. Antes de
  integrar, comparar cobertura, tolerancias e IDs y evitar dos kernels paralelos
  para la misma operación. Esta revisión es de código; no ejecuta la rama #86.

## Orden de desarrollo

1. Estabilizar Actions y pruebas, empezando por #116.
2. Completar B-Rep y conectar el boceto de UI con el motor.
3. Push/Pull táctil sobre perfil, cancelación sin residuos y cotas en mm.
4. ViewCube/navegación y selección/edición de caras y sólidos.
5. Unión, diferencia e intersección con validación explícita del alcance.
6. Historial paramétrico y persistencia íntegra.
7. UI móvil/tablet y STL/OBJ/DXF.
8. Herramientas avanzadas progresivas, integración y rendimiento.
9. APK por hitos tras verificaciones verdes.

## Primer hito: aceptación requerida

1. Dibujar rectángulo y acotarlo a 40 × 30 mm.
2. Extruir con dedo hasta 10 mm: volumen inicial 12000 mm³.
3. Ver sólido, orbitar y cambiar vista con ViewCube sin alterar la geometría.
4. Restar un cortador interior; comprobar volumen y cierre de la superficie.
5. Cambiar cotas del boceto: regenerar dependientes o rechazar atómicamente.
6. Guardar, cerrar y abrir: conservar IDs, geometría, cotas y dependencias.
7. Repetir en móvil/tablet, deshacer/rehacer y probar APK del hito.

Estado: **no aceptado aún**. Las pruebas del motor no sustituyen esta secuencia Android.


## Incremento: extrusión desde el lienzo por arrastre — [PR #117](https://github.com/josxtm/mycad/pull/117)

- El botón Extruir permite elegir arrastre con dedo o el diálogo anterior.
  También se añade la opción al menú Boceto táctil.
- Seleccionar rectángulo/círculo, activar arrastre, mover hacia arriba en el
  perfil y soltar. Escala inicial: 8 dp/mm, ajuste a 1 mm; 80 dp producen 10 mm.
- El contorno del boceto se proyecta punto a punto en 3D para que rectángulos y círculos permanezcan en su plano durante la vista previa.
- La vista previa no entra en `parts`, JSON ni historial. Un cuerpo se publica
  al soltar con altura positiva y perfil todavía vigente; se conserva el vínculo
  al UUID de boceto. Dos dedos, retorno al origen, cancelación del sistema o
  cambio del perfil descartan la propuesta. Guardar estado de Activity cancela.
- Alcance: perfiles rectangulares/circulares, altura positiva, visor Canvas.
  No es todavía el manipulador axial sobre perfil del visor GPU, ni solver nuevo.
- `SketchPushPullSessionTest`: recorrido 40 × 30 × 10 mm en tres planos,
  vista previa aislada, B-Rep cerrada, cambio a 50 × 30 mm, deshacer y roundtrip
  del documento paramétrico independiente. Comprueba también fuente obsoleta,
  líneas abiertas, altura cero, límites y círculo.
- Pendiente: aceptación física en móvil/tablet y el agujero por sustracción en el recorrido completo. No se declara el hito
  aceptado ni se genera APK de hito por esta implementación parcial.

Siguiente tarea: validar el arrastre en Android y cerrar la prueba integrada
rectángulo acotado → extrusión → sustracción → edición → JSON guardar/abrir.


## Continuación de #117: propiedad de gestos y persistencia Android

La extrusión se activa solo desde el interior o contorno proyectado del perfil
seleccionado. Un toque fuera del perfil devuelve el control a navegación/selección;
el ViewCube recibe sus propios gestos. Añadir un segundo dedo descarta la propuesta
y entrega el mismo evento al desplazamiento/zoom, inicializando el punto medio.
Los eventos restantes de un arrastre cancelado no pueden seleccionar ni orbitar
accidentalmente. Se exige superar el umbral táctil Android para crear geometría.
Cambiar de modo, pausar o guardar el estado de la Activity descarta la propuesta.

El contorno proyectado usado para dibujar y seleccionar se comparte en XY/XZ/YZ,
incluidos círculos vistos como elipses. La conversión de pantalla a boceto descuenta
ahora el desplazamiento de cámara: dibujar después de navegar conserva las cotas
respecto al plano de trabajo.

Se incorpora una suite Android con Robolectric/Android 35, independiente de las
pruebas Java geométricas. Cubre `MotionEvent`, ViewCube, órbita, selección,
pan/zoom, cancelación, umbral táctil, densidad, deshacer/rehacer y ciclo de
acotación mediante el diálogo real. También prueba el codec JSON, el borrador
persistente y los callbacks de archivo Guardar/Abrir con archivos temporales;
un proyecto corrupto debe conservar modelo e historial. Las comparaciones del
modelo excluyen la vista previa diagnóstica opcional cuyos IDs se regeneran;
se comprueban explícitamente los UUID reales de boceto/cuerpo y sus vínculos.

El runtime de prueba Android 35 se resuelve por Gradle, se fija a una versión y
se copia a `app/build/robolectric-sdks`; las pruebas no descargan jars ni crean
candados en la carpeta personal. Es una dependencia exclusiva de pruebas.
CI conserva los resultados JUnit y Lint en un artefacto separado del informe.

Estas pruebas ejecutan la Activity en el runtime JVM de Android; no sustituyen
aceptación física ni acreditan el selector de documentos de cada fabricante.
El hito completo permanece abierto hasta verificar la sustracción, edición de
sus dependencias y aceptación móvil/tablet. No se fusiona la PR automáticamente.


Validación local de esta continuación: suite geométrica Java e integridad,
compilación Android, **13 pruebas Robolectric correctas, 0 fallos/0 omitidas**,
y `lintDebug` correctos. El recorrido de persistencia Android cubre tanto el
borrador como los callbacks de Guardar/Abrir con un archivo real temporal.
La CI remota debe comprobarse sobre el commit publicado antes de integrar.


## Siguiente paso del hito — 2026-10-28

Tras [Actions #554 verde](https://github.com/josxtm/mycad/actions/runs/40605642272)
en `4843bd3`, GitHub informó `CLEAN`, sin revisiones bloqueantes registradas.
La PR se mantiene abierta, sin fusionar.

Se añade **Editar pieza → Acotar perfil original (mm)** para una extrusión
vinculada: permite cambiar ancho/fondo o diámetro desde el sólido seleccionado,
sin localizar el boceto oculto. Se conservan los controles anteriores de altura,
coordenadas del perfil y edición independiente. Se actualiza la guía de la app
para explicar el flujo real de boceto, Push/Pull, ViewCube, edición y archivos.

La prueba Android del archivo usa ahora esa entrada de edición del cuerpo.
Otra regresión comprueba sólido → ViewCube → acotar desde el cuerpo →
deshacer/rehacer, conservando UUID de cuerpo y vínculo al boceto.


Verificación local del segundo avance: **14 pruebas Android correctas,
0 fallos/0 omitidas**, integridad y Lint correctos. Las suites geométricas
completas ya pasaron en #554 y se volverán a ejecutar en la CI del nuevo commit.
Siguiente tarea: sustracción sobre el sólido vinculado, con regeneración y
persistencia de sus dependencias; aceptación física móvil/tablet pendiente.

## Sustracción circular vinculada — 2026-10-28

- `Proyecto → Perforar prisma (circular)` admite una extrusión rectangular
  vinculada, en su posición original respecto al boceto. La receta conserva
  UUID de boceto y altura de extrusión; el resultado sustituye al prisma por
  una malla de perforación editable. Las perforaciones independientes se conservan.
- Cambiar ancho/fondo del perfil prepara todas las recetas dependientes antes
  de publicar geometría. Si el agujero deja de caber, se rechaza la edición sin
  cambiar perfil, malla ni historial. No se permite borrar un perfil con una
  perforación dependiente; primero debe eliminarse el resultado.
- En Resultados booleanos, editar la perforación ofrece `Acotar perfil origen`.
  La receta mantiene el enlace al cambiar diámetro, posición, eje o profundidad.
- JSON incluye campos opcionales `sourceSketchId`/`sourceHeight`. Al abrir se
  exige un origen rectangular único y coincidencia de la malla con su receta
  regenerada. Se mantiene compatibilidad con recetas anteriores independientes.
- La perforación vinculada se dibuja también en Canvas; GPU y exportación
  utilizan la malla unificada existente.
- Dos regresiones Android nuevas recorren rectángulo 40×30 → extrusión 10 →
  agujero de diámetro 6 → cotas 50×30 → deshacer/rehacer → guardar y abrir,
  en XY/XZ/YZ. Verifican malla cerrada, volumen nominal, rechazo atómico,
  dependencia huérfana y callbacks reales de archivo. Suite Android: 16 pruebas.

Alcance: cortador circular parametrizado sobre prisma rectangular, con resultado
triangulado y receta regenerable; no es una booleana B-Rep general ni conserva
el UUID del prisma sustituido. La edición de altura del resultado perforado y
la selección analítica de sus caras siguen pendientes. No se ha validado en
hardware físico ni se declara aceptado el hito completo.

Validación local: suites geométricas completas e integridad correctas;
16 pruebas Android con Robolectric, 0 fallos/0 omitidas; compilación y Lint
correctos. Pendiente comprobar Actions sobre el commit publicado.

Publicación: al comprobar GitHub se detectó que #117 ya estaba fusionada en
`285a74b`. La continuación de perforaciones se publicará en una PR sucesora.
Se incorporó `origin/main` a la rama de trabajo mediante una fusión normal,
sin modificar `main`, rebase ni force push. La fusión no cambió el árbol de
archivos de la implementación local validada.

## Continuación del guion maestro — altura, selección e intercambio

- El editor de perforación vinculada admite la altura de extrusión. Altura,
  eje, diámetro y profundidad se preparan conjuntamente antes de publicar una
  receta: permite reducir a la vez altura y profundidad y rechaza un taladro
  ciego que ya no cabe. Recalcula posición/orientación desde el boceto en XY/XZ/YZ.
- Un toque selecciona la superficie del corte en Canvas; otro abre sus parámetros.
  Usa rayos contra la escena completa y su índice de triángulos: un agujero vacío
  no selecciona material y un cuerpo delante impide seleccionar el corte oculto.
  Se resalta el resultado seleccionado. La selección de caras B-Rep curvas sigue
  pendiente; el control actual edita la receta completa.
- Corregida la exportación OBJ: escribía secuencias literales de nueva línea,
  dejando un archivo que no podía reabrirse. Se verifican roundtrips OBJ/STL,
  STL ASCII externo e índices OBJ negativos. Se rechazan mallas vacías y
  coordenadas no finitas, incluidas las de STL binario.
- Verificación local: 18 pruebas Android Robolectric y 4 de MeshIO, todas correctas;
  compilación y Lint correctos. Se ejecutan de nuevo las suites geométricas completas.
- Se prepara APK debug del recorrido de prueba; no es una versión de producción.
  La aceptación física depende de disponer de teléfono/tablet conectado.

El guion completo continúa abierto: B-Rep general, restricciones generales,
booleanas arbitrarias, historial general, optimización en hardware y herramientas
Revolve/Sweep/Loft/Shell/Fillet/Chamfer necesitan desarrollo y pruebas adicionales.
La evidencia de este bloque cubre el subconjunto descrito, no esas fases pendientes.

### UI móvil/tablet y APK del recorrido

La fila de modos pasa a un contenedor horizontal desplazable con área mínima
88×48 dp por control. El cambio se limita a esa fila y no altera botones de
otros paneles; no fusiona #80 ni #86. Pruebas de layout a 320×640 dp y
1280×800 dp verifican dimensiones, desplazamiento y espacio de lienzo.
La guía de la app explica la edición por toque del resultado perforado.

Verificación del bloque: **24 pruebas correctas (20 Android Robolectric y 4
MeshIO), 0 fallos/omitidas**, geometría completa, integridad, compilación y Lint.
Se genera una APK debug para pruebas del hito con firma de depuración; todavía
no es aceptación física ni finalización de todas las fases del guion.
`adb devices -l` no detecta dispositivos Android conectados en este entorno.

Prueba de aceptación física pendiente: rectángulo 40×30, cota, arrastre 10 mm,
ViewCube, agujero Ø6, edición de ancho/fondo y altura/profundidad del taladro,
guardar/abrir y exportar STL/OBJ. Repetir en móvil y tablet; registrar gestos,
fluidez y resultados antes de declarar aceptado el primer hito.


## PUNTO DE GUARDADO SEGURO — 2026-10-28

Repositorio exclusivo: `https://github.com/josxtm/mycad.git`.
Rama de continuación: `feature/sketch-push-pull-touch`; PR abierta #118.
Los avances funcionales están en `fc663b7`; la fusión `420518a` conserva además
la integración remota de #116. El commit de este punto de guardado incorpora
este registro y actualiza dos comprobaciones estáticas a las rutas de edición
nuevas; no elimina las comprobaciones de persistencia ni selección.

**Verificado por alcance:** Push/Pull rectangular/circular; gestos, ViewCube y
selección del resultado perforado; receta circular vinculada; edición conjunta
de altura/profundidad y cotas; persistencia/undo; corrección OBJ y roundtrips
STL/OBJ; controles de modos desplazables en móvil/tablet. Suite actual:
20 pruebas Android y 4 MeshIO. Se han ejecutado geometría completa, integridad,
Lint y ensamblado debug. La CI del commit publicado es la autoridad de integración.

**No completado:** kernel B-Rep general, solver general de restricciones,
booleanas arbitrarias, historial universal, caras curvas analíticas editables,
DXF completo, Revolve/Sweep/Loft/Shell/Fillet/Chamfer y rendimiento en hardware.
No se declara aceptado el primer hito físico. Solo hay pruebas Robolectric;
`adb devices -l` no encuentra dispositivos. Seleccionar una perforación edita
su receta, no entidades B-Rep curvas. OBJ triangula polígonos por abanico;
no se declara soporte general de polígonos cóncavos.

**Artefactos locales conservados:** APK debug firmada en
`build/checkpoint/mycad-preview.apk` y resultados en `app/build/test-results/`
y `app/build/reports/`. `.gradle/`, `app/build/`, `build/` y `reports/` son
cachés/artefactos generados, no código pendiente. La clave de depuración permanece
fuera del repositorio. No se suben claves ni cachés a Git. No queda una función
incompleta sin commit que requiera rama WIP independiente.

### Reanudar exactamente

1. Clonar `josxtm/mycad` o entrar en su copia. Confirmar remoto y rama antes de tocar código.
2. Ejecutar:

```bash
cd /workspace/mycad
git remote -v
git fetch origin
git switch feature/sketch-push-pull-touch
git pull --ff-only origin feature/sketch-push-pull-touch
git status --short --branch
cat docs/ROADMAP_MYCAD.md
```

3. Usar JDK 17, Gradle 8.9 y SDK Android 35. En este entorno:

```bash
source /workspace/.mycad-tools/env.sh
bash scripts/verify-project.sh
gradle :app:testDebugUnitTest :app:lintDebug --no-daemon
```

4. Comprobar PR #118 y Actions del SHA remoto actual. Si #118 fue fusionada,
   incorporar `origin/main` mediante una fusión normal en la rama de trabajo y
   abrir una PR sucesora. No rebasar ni forzar una rama compartida; nunca editar main.
5. Prioridad siguiente: instalar la APK de pruebas en teléfono/tablet y verificar
   40×30 → extrusión 10 → ViewCube → agujero Ø6 → editar cotas/altura → guardar/abrir.
   Registrar fallos y rendimiento. Después continuar las fases pendientes en el
   orden del guion. No fusionar con verificaciones pendientes o fallidas.

Este punto de guardado pausa el desarrollo por instrucción del usuario; no
programa tareas en segundo plano ni considera completadas las fases restantes.

## Continuación — recetas escalonadas y selección B-Rep Android

Base remota verificada: PR #126, commit `a776a09`, Actions
[#582](https://github.com/josxtm/mycad/actions/runs/40706414114) correcto.
El desarrollo se reanuda en `feature/cad-persisted-brep-binding`; el punto de
guardado anterior se conserva como registro histórico.

Se añaden unión/resta ortogonales de alturas distintas como recetas persistidas,
su asociación exacta con shells en el visor, selección del parche visible con área
y resaltado completo, y descarte de callbacks de escenas reemplazadas. Se conserva
el orden global de los triángulos de selección/exportación y las operaciones antiguas.
Las pruebas nuevas cubren el menú Android, JSON, oclusión, selección de un bolsillo,
topología obsoleta y las seis combinaciones de plano/sentido. Los operandos siguen
existiendo: aislar el resultado permite tocar caras que ellos ocultan en la escena completa.

Validación física y edición libre de parches/aristas escalonados pendientes. La
identidad del propietario es el UUID persistente de la receta; los identificadores
internos de topología representan una regeneración concreta, sin prometer identidad
semántica de todas las subentidades tras cambios topológicos.

Estado, verificaciones y recuperación actuales: [DEVELOPMENT_STATUS.md](DEVELOPMENT_STATUS.md).

### Etapa siguiente — editar alturas desde el resultado seleccionado

La PR [#127](https://github.com/josxtm/mycad/pull/127) quedó integrada tras
[Actions #583 correcto](https://github.com/josxtm/mycad/actions/runs/40713496874).
La continuación `feature/cad-stepped-parameter-edit` permite tocar de nuevo un
parche B-Rep para editar las alturas de sus perfiles de origen. Reutiliza la
regeneración transaccional del motor; no desplaza libremente el parche. El diálogo
se rechaza si la referencia ha cambiado o si falla la topología dependiente.
Pruebas Android añadidas: altura → regeneración → JSON → undo/redo, diálogo obsoleto
y error geométrico sin mutaciones. Total local: 32 pruebas Android/IO superadas.
Pendiente CI de esta continuación y aceptación física. Próximo paso: bordes
topológicos seleccionables y sus referencias a la regeneración actual.

### Selección de bordes del parche visible

PR [#128](https://github.com/josxtm/mycad/pull/128) integrada tras
[Actions #585 correcto](https://github.com/josxtm/mycad/actions/runs/40714473648).
En `feature/cad-stepped-boundary-selection` se seleccionan exclusivamente los bordes
de los bucles del parche B-Rep visible. El inspector muestra longitud, XYZ y adyacencia;
las diagonales y seams coplanares quedan fuera. La tolerancia se adapta a profundidad
y densidad. Regresiones geométricas para seis vistas plano/sentido y 34 pruebas
Android/IO locales superadas. Aún sin edición geométrica libre de estas aristas,
identidad semántica tras cambios topológicos ni aceptación en hardware. Fase A estimada
en 65 %, sin considerar completo el modelador CAD general.

### Identidad de subentidades y versiones geométricas

Las PR #127–#129 están integradas y el hito
[#588](https://github.com/josxtm/mycad/actions/runs/40716562543) pasó integridad,
geometría, Android, Lint y generación/verificación de APK. La APK `myCad0.6.0`
está en los artefactos de ese flujo; aceptación física pendiente.

La continuación `feature/cad-stepped-entity-identities` incorpora nombres
conservadores de vértices/aristas/caras/parches derivados de los UUID de los
operandos y la procedencia de las coordenadas del grid. Se conservan al editar
dimensiones dentro de la misma estructura; coincidencias nuevas o cambios de
operación/orden de operandos invalidan las referencias correspondientes. No se
reutilizan rayos ni triángulos antiguos aunque siga existiendo la misma entidad.
Inspectores Android conectados y reconstrucción determinista tras guardar/abrir,
sin cambiar el formato JSON. Regresiones de motor y 36 pruebas Android/IO locales.
Fase A estimada en 75 %; identidad general, rendimiento y hardware siguen pendientes.

### Continuación — agujeros pasantes y componentes independientes

Base `8340165` (#130), CI [#590](https://github.com/josxtm/mycad/actions/runs/40719663983)
correcto. `NamedGridHoleBody` amplía las identidades de origen a las operaciones
ortogonales con agujeros pasantes y resultados desconectados. `SourceTopologyNames`
comparte el nombrado sin cambiar los identificadores escalonados publicados: una
regresión congela su SHA-256 sobre doce fixtures de la versión anterior.

El visor reconoce las caras quad de estos resultados, incluidos los de vista
aislada; su inspector muestra área, identidad persistente y versión geométrica.
Se verifica que registro, malla persistida y receta coincidan antes de seleccionar.
No cambia el JSON, la teselación ni el orden global de triángulos de exportación.
Las caras aún son celdas quad; no se anuncian como parches coplanares agrupados.

Pruebas: tres planos y ambos signos, agujero → apertura lateral, dos componentes,
dimensiones editadas, compatibilidad de nombres, guardado/apertura, índices,
oclusión y rechazo de geometría obsoleta. Aceptación física pendiente y separada.
Siguiente paso: reconciliación tipada de entidades conservadas, creadas y eliminadas
durante cambios de topología. Fase A estimada en 80 %, todavía parcial.

### Reconciliación tipada de cambios topológicos

PR #131 integrada en `3c3c09b` tras [#592 correcto](https://github.com/josxtm/mycad/actions/runs/40724016163).
El fallo de compilación Java 8 de #591 se corrigió sin desactivar pruebas.

La continuación `feature/cad-topology-reference-reconciliation` añade un índice
inmutable de referencias por tipo/propietario y versión analítica SHA-256, para
escalonados, agujeros, prismas poligonales y primitivas. Registra entidades
conservadas/creadas/eliminadas y distingue re-vinculación persistente de resolución
contra la geometría exacta. El visor publica los índices como un lote validado,
retira cuerpos borrados y muestra el último cambio en los inspectores.

43 pruebas Android/IO y regresiones geométricas incluyen apertura real de agujero
desde su perfil, undo, fallo de geometría sin publicación parcial, cilindros sin
vértices ficticios y cambios analíticos menores que la precisión de malla.
Fase A estimada en 85 %; resultados booleanos no ortogonales y otras rutas de
nombrado siguen pendientes, junto con rendimiento y aceptación física.

### Intersecciones convexas inclinadas y evolución de sus caras

PR #132 integrada en `0c57f8e`, tras
[#594 correcto](https://github.com/josxtm/mycad/actions/runs/40725744292).
`feature/cad-named-convex-intersections` nombra la receta INTERSECTION por
esquinas y líneas de soporte de origen. Las tapas mantienen su rol cuando cambia
el número de esquinas; nuevos vértices/aristas aparecen o desaparecen sin
reasignación por proximidad histórica. Los soportes colineales y un caso agudo
de la geometría anterior a coordenadas altas quedan cubiertos por regresiones.

La vista planar compartida permite seleccionar e inspeccionar caras poligonales
completas, conservando las rutas con agujeros. Rechaza asociaciones después de una
previsualización temporal y eventos pendientes del visor oculto. Java 8, 47 pruebas
Android/IO y suite geométrica local superadas; aceptación física pendiente.
Fase A estimada en 90 %, aún sin completar el nombrado de uniones/restas restantes,
resultados cóncavos generales ni todas las familias curvas.

## Continuación: booleanas planares con identidades persistentes

- Base verificada: PR #133 integrada, main `0a44341`, android.yml [#597](https://github.com/josxtm/mycad/actions/runs/40730118057) verde.
- Rama: `feature/cad-named-planar-booleans`.
- Nombrado por procedencia en las once recetas ANALYTIC/ORTHOGONAL/POLYGONAL/MULTISHELL restantes.
- Identidades tipadas de vértices, aristas y caras, selección Android, inspector y reconciliación; teselación histórica idéntica.
- Separación/fusión: tapas de componentes se retiran y recrean según contorno de procedencia; no se reasignan por índice o proximidad.
- Regresiones: planos/signos, unión no ortogonal, intersección cóncava de altura desigual, U cóncava, división, undo, reapertura y geometría adulterada.
- 51 pruebas Android/IO; integridad y suite geométrica de CI. Gradle/Lint requieren CI verde antes de integrar.
- Fase A estimada 93 %, parcial: faltan otras familias y migración de propietarios/referencias. Perforaciones curvas aún basadas en mallas.
- Validación física Android/EGL y rendimiento pendientes. Recuperación y límites en `docs/DEVELOPMENT_STATUS.md`.

## Guion vigente — origen local compartido para GPU

- PR #144 integrada tras verificar #618 verde; main `4756ece`, flujo [#619](https://github.com/josxtm/mycad/actions/runs/40760119620) correcto.
- Rama `feature/cad-relative-gpu-origin`: generación local inmutable, cámara double, índices GPU/rayos en el mismo origen y callbacks en coordenadas mundiales.
- Fuentes double de BoxBRep conectadas a la escena y sus previsualizaciones; se conservan exportación, orden global, selección y UUID.
- Contornos/tiradores relativos al origen; caché y callbacks invalidados al sustituir la escena; conservación de cámara al editar.
- Regresiones automáticas de precisión a ±1e9, Android matrices/rayos, cambios de origen y puente B-Rep escalonado.
- Pendiente: ampliar fuentes double a cilindros y demás familias, sin atribuir al conversor recuperación de datos ya redondeados.
- Pruebas físicas Nothing Phone 2a/tablet 12 pulgadas, EGL y rendimiento pendientes. Estado y recuperación actualizados en `docs/DEVELOPMENT_STATUS.md`.

## Fuentes double de cilindros analíticos

- Rama `feature/cad-double-cylinder-preview`, desde main `62cc5de` tras PR #145 integrada con [CI #623](https://github.com/josxtm/mycad/actions/runs/40763061992) verde.
- CylinderSurface/CylinderBRep/PrimitiveBRepBody mantienen coordenadas double hasta aplicar el origen GPU.
- Escena y previsualización Android usan la misma fuente para cajas y cilindros, incluidos cuerpos rotados.
- Digest de compatibilidad de seis fixtures: teselación float publicada idéntica.
- Regresiones de precisión lejana, superficie local cerrada, no mutación y puente real de Android. 60 pruebas Android/IO, geometría completa e integridad superadas localmente; nuevo CI obligatorio antes de integrar.
- Se mantienen los límites del editor y el formato JSON; la exportación float histórica no obtiene precisión double por este cambio.
- Próximo motor B-Rep: fuentes de perfiles/booleanas y degeneración; validación física/EGL y memoria pendientes.

## Motor B-Rep: fuentes double de extrusiones y resultados planares

- Base main `74f7a6b`, PR #147 integrada tras #625 verde. Hito [#626](https://github.com/josxtm/mycad/actions/runs/40764258161) con APK compilada y verificada.
- Rama `feature/cad-double-profile-preview`. ClosedSketchExtrusion retiene coordenadas exactas; PolygonPrismBRep las expone sin alterar la teselación publicada.
- Digest de doce fixtures históricos de perfiles; se conserva winding en XY/XZ/YZ y extrusión positiva/negativa.
- PlanarBooleanView y escena Android conectan fuentes de perfiles vinculados y booleanas nombradas; vista aislada coherente, buffers exportados y JSON sin cambios.
- Regresiones de altura double, no mutación, guardado/reapertura, cascarones múltiples y correspondencia de los índices globales.
- Grids/escalonados, algunas previsualizaciones y kernels con casts internos requieren auditoría posterior. No se atribuye recuperación de detalle ya perdido.
- Validación física Android/EGL en Nothing Phone 2a y tablet 12 pulgadas, memoria y rendimiento pendientes.

## Temas completos — sucesora de #146

- Base main `06c34bc`; inicio verificado con flujo #628 verde y sin commits nuevos.
- Rama `feature/ui-complete-persistent-themes`, MyCAD 0.6.1 (61).
- Seis temas: Claro predeterminado, Oscuro de alto contraste, Azul Técnico, Verde Oliva, Sepia/Papel Técnico y Personalizado.
- Selector con seis vistas previas y editor de doce colores #RRGGBB; Aplicar/Cancelar transaccionales.
- Preferencias persistentes y compatibles con `dark` y los cinco índices de #146.
- Paleta única en barras, botones, iconos, Canvas, cuadrícula/ejes, ViewCube, selección, GPU y diálogos/dropdowns.
- El cambio de estilo no altera archivo CAD, geometría, cámara, origen, picking ni historial.
- 73 pruebas Android/IO, geometría e integridad locales. Capturas Android/Skia de las seis interfaces revisadas y publicadas con informes de CI.
- Aproximación de temas: 95 %, pendiente validación física/EGL, memoria y ergonomía en los dos dispositivos de referencia.
- Siguiente motor B-Rep: UUID repetibles en el puente paramétrico orientativo; problema detectado durante las pruebas y reservado a otra PR.
- No se fusionan #80/#86/#136 ni se modifica la rama de #146. Fusión de la sucesora solo con CI final verde y sin bloqueos.

## Puente paramétrico: identidades estables tras integrar temas

- PR #150 integrada con CI #631 verde; main `5c815ff`, hito #632 correcto con APK myCad0.6.1. #146 cerrada como sustituida, rama original conservada.
- Rama `feature/cad-stable-parametric-preview`: IDs explícitos, canónicos y transaccionales para rectángulos/extrusiones del documento paramétrico.
- La sonda usa UUID de propietarios Android y deriva operaciones de ellos. Serialización completa repetible, incluso tras reinicio; edición y reordenado conservan identidades.
- Se mantienen APIs antiguas con fallback determinista por fila para callers sin propietario; no se promete estabilidad de reordenado sin IDs.
- Duplicados/UUID inválidos/geometría inválida no reemplazan entidades ni crean revisiones parciales. Geometría, poses y formato de snapshot sin cambios.
- La prueba de aislamiento de temas vuelve a comparar el JSON entero, sin excluir datos diagnósticos. 75 Android/IO, integridad y geometría completas locales; CI obligatorio.
- PR #149 de otro agente permanece sin fusionar por CI rojo; no se modifica esa rama.
- Motor global aún parcial; aceptación física/EGL y rendimiento pendientes. No se genera otra APK por esta corrección de metadatos.

## Precisión del puente de booleanas ortogonales con agujeros

- PR #151 integrada, commit `a60f5e1`, tras CI #633 verde. Rama siguiente: `feature/brep-grid-double-coordinate-bridge`.
- Fuente double de cada triángulo extraída directamente de los vértices analíticos y transmitida a PlanarBooleanView/origen relativo GPU.
- Se conservan mallas float, caras, aristas, identificadores, orden, formato de archivo y exportaciones. Hash de regresión obtenido antes de editar el kernel.
- Cobertura: 24 resultados XY/XZ/YZ con alturas firmadas, unión/intersección/diferencia, agujero y múltiples sólidos. Android verifica cámara/visor, selección y guardar/reabrir.
- 77 pruebas Android/IO, geometría completa e integridad locales satisfactorias. CI #634 de main verde; la nueva PR requiere verificar su propio SHA antes de integrar.
- Los límites anteriores de validación float y naming por coordenadas siguen vigentes; no se afirma resolver aquí geometría sub-ULP ni el kernel escalonado.
- Próximo trabajo: naming preciso compatible y coordenadas analíticas de escalonados. Validación física Nothing Phone 2a/tablet 12 pulgadas pendiente.
- Otro agente actualizó #149 a `a1aac91`; CI #635 pendiente al guardar este punto. No se modifica esa rama.

## Continuación de extrusión paramétrica — 2026-10-29

- Base real `ea07cc6`, CI #656 verde, con #153–#159, #149 y #136 integradas. #80/#86 siguen en borrador y se conservan.
- Auditoría distingue B-Rep analítica real de CadIntegrationProbe orientativo. Booleanas implementadas por familias, no geometría curva universal. Datos antiguos de Sketch/Part aún float.
- `feature/cad-closed-profile-transaction`: corrige una publicación obsoleta reproducida con Android/Robolectric. Un cambio de líneas durante el diálogo ya no puede crear un sólido antiguo ni modificar historial/JSON parcialmente.
- Se reutilizan ClosedSketchExtrusion, PolygonPrismBRep y LoopExtrusionFeature. Nuevo preflight inmutable por UUID/coordenadas, fuente double para GPU, rechazo antes de checkpoint y mensajes acordes a la vinculación real.
- 87 pruebas Android/IO, geometría completa e integridad locales correctas. Alturas positivas/negativas, tres planos, edición por menú, IDs, Deshacer/Rehacer y guardar/abrir verificados por software. CI Gradle/Lint obligatorio sobre el SHA publicado.
- Siguiente incremento: conectar el botón Extruir a contornos cerrados de líneas y preparar APK de hito. Nada de validación física/EGL atribuida a compilación o Robolectric.

## Hito 0.6.2: contorno B-Rep desde Extruir

- PR #160 integrada, fuente `c00349e`, main `da1c394`: CI #657 y #658 verdes. Fuente y preflight vigentes antes de publicar cuerpos.
- `feature/cad-closed-profile-toolbar`: botón general y opción estándar de extrusión conectados al motor de contornos existente. Rectángulos/círculos mantienen Push/Pull y deslizador.
- 91 pruebas Android/IO locales correctas. Nuevo recorrido por botones/menús: dos perfiles → extruir → restar → editar cara → regenerar → Deshacer/Rehacer → guardar/abrir, con UUID conservados y alturas double.
- Caso 40×30×10 menos 10×10×10: 11000 mm³; cara lateral +5 mm: 12500 mm³. Altura incompatible del agujero se rechaza atómicamente.
- Versión de hito 0.6.2 (62). Se genera una APK solo tras integración con checks satisfactorios; resultado/artefacto en la PR y ci-status.
- Hito de contornos rectos ~90%, pendiente dispositivo físico/EGL y rendimiento. Motor global parcial; Sketch/Part antiguos aún float, tolerancia de cierre 0,15 mm y booleanas curvas generales pendientes.
