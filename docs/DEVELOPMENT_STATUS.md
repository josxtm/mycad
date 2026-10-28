# Estado de desarrollo de MyCAD

## Estado vigente y base comprobada

- Repositorio exclusivo: `josxtm/mycad`; no se trabaja en CADCraft ni directamente en main.
- PR #144 revisada e integrada desde `43fcf01ea9238e67aa3990fab60cda810e661ea7`.
- Base main: `62cc5de24ef0c7c470e14f9433fae48f6c3dd8da` (PR #145 integrada).
- PR #144: [android.yml #618](https://github.com/josxtm/mycad/actions/runs/40758696428), correcto.
- Main: [android.yml #619](https://github.com/josxtm/mycad/actions/runs/40760119620), correcto.
- PR #80/#86 antiguas y PR #136 conservadas; no se fusionan por esta continuación.

## Fase A del guion vigente: precisión e integración GPU

Rama de trabajo: `feature/cad-double-cylinder-preview`.
La etapa previa de origen compartido se integró en PR #145 (`e0700dc`), con
[android.yml #623](https://github.com/josxtm/mycad/actions/runs/40763061992) verde,
sin conflictos ni revisiones pendientes. El CI posterior de main se consulta en ci-status.

`RelativeMeshCoordinates` (#144) resta un origen double antes de convertir a
float. `RelativeSceneCoordinates` ahora mantiene una generación de escena con
un único origen, calculado a partir de los límites double. Genera las posiciones
locales de todos los meshes sin alterar la geometría mundial ni su orden.

MeshEngine utiliza esa generación tanto para los buffers GPU como para los
índices de rayos. La cámara conserva su foco mundial double y resta el mismo
origen antes de construir la matriz float. Contornos, tiradores y proyección de
los tiradores usan la misma transformación. Los impactos y el origen del rayo
vuelven a coordenadas mundiales antes de llegar a la selección B-Rep existente;
la distancia, dirección e índices globales no cambian.

Las sustituciones y las previsualizaciones comparten la instalación de escena:
invalidan los buffers y callbacks anteriores. Cambiar la geometría conservando
la cámara no vuelve a centrar el foco, zoom u orientación. Las entradas inválidas
se rechazan antes de incrementar la revisión pública. La caché GPU y el índice
de rayos se reutilizan mientras no cambia la generación.

BoxBRep expone triángulos double independientes de la vista previa mundial float.
Los prismas analíticos Android retienen esos datos al formar la escena y las
previsualizaciones. El buffer float histórico se conserva para exportación y
herramientas anteriores; se comprueba que coincide con el redondeo de la fuente
double. La selección visible utiliza los mismos triángulos locales que la GPU.

`CylinderSurface.triangleCoordinates` añade la fuente double de los cilindros,
con exactamente la misma teselación y orden que la ruta float histórica.
CylinderBRep y PrimitiveBRepBody la exponen al visor. La escena y las vistas
previas de Android conservan la fuente double de ambas primitivas sin modificar
el JSON, los límites de los editores ni los buffers mundiales de intercambio.

La compatibilidad de seis fixtures publicados (12/48/128 segmentos y poses
diferentes) queda congelada en el digest
`efa2199fc0211246566d29720d693f0835f387152b8497962a524bedba428c73`.
Las pruebas también comprueban cilindros locales cerrados de 0,01 mm a ±1e9,
la no mutación y la fuente exacta de una previsualización Android rotada.

### Límites explícitos

- El conversor y los rayos locales se prueban con detalle de 0,01 mm a ±1e9 mm.
  Esto no amplía los límites de coordenadas de los editores Android.
- BoxBRep mantiene su rechazo de una exportación/vista previa mundial float que
  colapse una cara. La nueva ruta no oculta pérdidas existentes en archivos float.
- Recetas malladas y varios resultados persistidos aún llegan al visor
  desde buffers float; trasladarlos después no recupera detalle ya perdido.
- Próximo paso del motor B-Rep: ampliar fuentes de perfiles/resultados nombrados
  sin alterar teselaciones, identidades, JSON ni selección; auditar degeneración.

## Verificación

- Integridad: se actualizan las comprobaciones del puente GPU para exigir la
  conversión local, el uso de un origen compartido y la devolución del rayo mundial.
- Suite geométrica completa de android.yml, incluida RelativeSceneCoordinatesTest.
- Pruebas nuevas de precisión, no mutación, offsets/oclusión, caché y entradas inválidas.
- 60 pruebas Android/IO superadas localmente (58 de la etapa #145): suite existente más instalación real de escena, matrices Android,
  proyección, cámara conservada, callbacks mundiales, revisión obsoleta y puente
  de selección de parches B-Rep escalonados.
- Compilación local: JDK 17, lenguaje y bytecode Java 8, SDK 35.
- Gradle local no puede crear sockets del daemon en este sandbox. Compilación
  Gradle, tests y Lint completos son obligatorios en CI antes de fusionar.
- Este documento acompaña al código antes de CI: el SHA y resultado final se
  registran en la PR y en [ci-status](https://github.com/josxtm/mycad/blob/ci-status/ESTADO_COMPILACION.md).
- EGL y validación física en Nothing Phone 2a/tablet Android de 12 pulgadas:
  pendientes; también pendientes mediciones de rendimiento y memoria reales.

El primer CI de #145 (#620) detectó tres fallos de carga de clases al buscar
métodos del GLSurfaceView por reflexión. La revisión correctiva `8130b15`
expone al paquete los mismos puntos de proyección/picking para invocación
directa desde los tests. Se conservan todas las pruebas y no se añaden
simulaciones GL ni dependencias. Esta continuación incorpora esa corrección.

La revisión correctiva `8130b15` pasó [CI #622](https://github.com/josxtm/mycad/actions/runs/40762489338).
La revisión final añade una regresión de encuadre de mallas importadas grandes:
se conserva la distancia histórica de ajuste, sin introducir el límite de zoom
interactivo en el cálculo inicial de encuadre. El commit final `e0700dc` superó #623 antes de integrar #145.

## Plan y pendientes

1. A: origen compartido y fuentes double de cajas/cilindros implementados; exigir
   CI verde. Mantener auditoría de fuentes de perfiles y recetas malladas.
2. B: continuar B-Rep, booleanas, regeneración, degeneración e identidades. Todas
   las rutas LinkedConvexIntersection tienen adaptación nombrada; la cobertura
   global B-Rep sigue parcial (perforaciones curvas aún basadas en mallas).
3. C: integración táctil de selección y edición conservando navegación y archivo.
4. D: ViewCube tridimensional, caras/aristas/esquinas, sincronización Canvas/GPU.
5. E: APK de hito tras CI verde; aceptación física y rendimiento independientes.

El 93 % anterior correspondía a la fase de identidades B-Rep del guion anterior,
no a la finalización del nuevo plan de precisión ni a un CAD general completo.
No se consideran terminadas familias sin pruebas.

## Recuperación

```bash
git clone https://github.com/josxtm/mycad.git
cd mycad
git switch feature/cad-double-cylinder-preview
git status --short --branch
cat docs/DEVELOPMENT_STATUS.md
bash scripts/verify-project.sh
gradle :app:compileDebugJavaWithJavac :app:testDebugUnitTest :app:lintDebug --no-daemon
```

Si la PR ya está integrada, partir de origin/main en una rama nueva. No rebasar
ni forzar ramas compartidas. El checkout local de esta continuación está en
`/workspace/mycad-double-cylinder`; su base materializada tiene el mismo árbol que
main `62cc5de` tras los cambios de la PR #145. La publicación mediante conector usa el padre remoto real y no
publica los ancestros sintéticos locales.

Un experimento anterior de propietarios de recetas quedó conservado localmente
en `feature/cad-persisted-recipe-owners`, commit `b41ec27`, como WIP sin pruebas.
No forma parte de esta fase ni debe fusionarse: main contiene avances posteriores
sobre la misma área y requiere comparación antes de recuperar ese experimento.
