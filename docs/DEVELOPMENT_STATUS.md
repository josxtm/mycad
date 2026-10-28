# Estado de desarrollo de MyCAD

## Estado vigente y base comprobada

- Repositorio exclusivo: `josxtm/mycad`; no se trabaja en CADCraft ni directamente en main.
- PR #144 revisada e integrada desde `43fcf01ea9238e67aa3990fab60cda810e661ea7`.
- Base main: `4756ece7e1cf3fd41b760e7d28831cfbfd022608`.
- PR #144: [android.yml #618](https://github.com/josxtm/mycad/actions/runs/40758696428), correcto.
- Main: [android.yml #619](https://github.com/josxtm/mycad/actions/runs/40760119620), correcto.
- PR #80/#86 antiguas y PR #136 conservadas; no se fusionan por esta continuación.

## Fase A del guion vigente: precisión e integración GPU

Rama de trabajo: `feature/cad-relative-gpu-origin`.

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

### Límites explícitos

- El conversor y los rayos locales se prueban con detalle de 0,01 mm a ±1e9 mm.
  Esto no amplía los límites de coordenadas de los editores Android.
- BoxBRep mantiene su rechazo de una exportación/vista previa mundial float que
  colapse una cara. La nueva ruta no oculta pérdidas existentes en archivos float.
- Cilindros, recetas malladas y varios resultados persistidos aún llegan al visor
  desde buffers float; trasladarlos después no recupera detalle ya perdido.
- Próximo paso del motor: ampliar las fuentes double sin alterar teselaciones,
  identidades, JSON ni selección; empezar por cilindros analíticos.

## Verificación

- Integridad: se actualizan las comprobaciones del puente GPU para exigir la
  conversión local, el uso de un origen compartido y la devolución del rayo mundial.
- Suite geométrica completa de android.yml, incluida RelativeSceneCoordinatesTest.
- Pruebas nuevas de precisión, no mutación, offsets/oclusión, caché y entradas inválidas.
- 57 pruebas Android/IO superadas localmente: suite existente más instalación real de escena, matrices Android,
  proyección, cámara conservada, callbacks mundiales, revisión obsoleta y puente
  de selección de parches B-Rep escalonados.
- Compilación local: JDK 17, lenguaje y bytecode Java 8, SDK 35.
- Gradle local no puede crear sockets del daemon en este sandbox. Compilación
  Gradle, tests y Lint completos son obligatorios en CI antes de fusionar.
- Este documento acompaña al código antes de CI: el SHA y resultado final se
  registran en la PR y en [ci-status](https://github.com/josxtm/mycad/blob/ci-status/ESTADO_COMPILACION.md).
- EGL y validación física en Nothing Phone 2a/tablet Android de 12 pulgadas:
  pendientes; también pendientes mediciones de rendimiento y memoria reales.

## Plan y pendientes

1. A: completar fuentes double y auditar la precisión de todos los puentes GPU.
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
git switch feature/cad-relative-gpu-origin
git status --short --branch
cat docs/DEVELOPMENT_STATUS.md
bash scripts/verify-project.sh
gradle :app:compileDebugJavaWithJavac :app:testDebugUnitTest :app:lintDebug --no-daemon
```

Si la PR ya está integrada, partir de origin/main en una rama nueva. No rebasar
ni forzar ramas compartidas. El checkout local de esta continuación está en
`/workspace/mycad-relative-gpu`; su base materializada tiene el mismo árbol que
main `4756ece`. La publicación mediante conector usa el padre remoto real y no
publica los ancestros sintéticos locales.

Un experimento anterior de propietarios de recetas quedó conservado localmente
en `feature/cad-persisted-recipe-owners`, commit `b41ec27`, como WIP sin pruebas.
No forma parte de esta fase ni debe fusionarse: main contiene avances posteriores
sobre la misma área y requiere comparación antes de recuperar ese experimento.
