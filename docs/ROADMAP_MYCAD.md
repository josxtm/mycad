# MyCAD: hoja de ruta y evidencia

Actualizado: 2026-10-27. Repositorio exclusivo: `josxtm/mycad`.
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
