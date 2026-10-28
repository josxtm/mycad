# Estado de desarrollo de MyCAD

## Base verificada

- Repositorio exclusivo: `josxtm/mycad`.
- Base de esta continuación: `bb429e05cad41ec316e6f113b2572069ad2b65f5` (PR #128 integrada).
- Última etapa completada: edición transaccional de alturas desde el resultado
  B-Rep seleccionado (PR #128, commit `de1861e`); selección integrada en #127.
- CI de esa etapa: [android.yml #585](https://github.com/josxtm/mycad/actions/runs/40714473648), correcto,
  incluidos compilación Android, pruebas y Lint. Integrada sin saltar protecciones.
- PR #80 y #86: borradores conservados, sin fusionar.

## Fase actual — A: selección e inspección de bordes topológicos

La ruta de pantalla a rayo, selección visible y registro de parches ya existía.
Faltaba asociarla con las recetas del editor. Este cambio incorpora:

- Recetas explícitas `UNION_STEPPED` y `DIFFERENCE_STEPPED`, con UUID de operación
  y referencias a las dos extrusiones originales. Se guardan en el JSON existente.
- Regeneración validada de malla y topología desde una misma receta, sin cambiar
  las rutas ni la teselación de las operaciones antiguas.
- Escena GPU dividida en fragmentos que conservan exactamente el orden global de
  triángulos usado por selección, primitivas y exportación.
- Registro de shells solo si su vista previa coincide exactamente con la malla
  persistida. Una malla obsoleta se rechaza antes de registrarse.
- Entrega de selección mediante los listeners reales de Android, con identificación
  del resultado por UUID de receta, área del parche y resaltado de todos sus triángulos.
- Generaciones separadas de escena solicitada y escena renderizada: los callbacks
  antiguos se descartan tras sustituir la escena o iniciar una previsualización.

### Flujo disponible

1. Extruir dos contornos de líneas ortogonales, en el mismo plano y sentido.
2. Seleccionar un contorno y tocarlo otra vez para sus acciones.
3. Elegir **Unir / restar con alturas distintas**, operación y segundo contorno.
4. Abrir **Resultados booleanos → resultado → Ver solo esta malla** para tocar
   las caras del resultado sin que los operandos originales las oculten.
5. Guardar y abrir: se conservan receta, UUID y geometría; se reconstruye el registro.
6. Tocar otra vez el mismo parche (o **Editar cara seleccionada**) abre los parámetros
   de sus dos perfiles de origen. Elegir una altura y aplicar regenera todas las
   dependencias antes de guardar. La edición vuelve a la escena completa.
7. Tocar cerca de un borde real del parche lo resalta y muestra su longitud.
   **Inspeccionar aristas** ofrece longitud, extremos XYZ, adyacencia y UUID de receta.

La nueva selección busca exclusivamente entre los bordes de los bucles del parche
visible. Excluye diagonales trianguladas y divisiones internas entre quads coplanares.
La tolerancia táctil usa la profundidad del impacto, altura del viewport, densidad
y tamaño del shell; es aproximada bajo perspectiva y no modifica la geometría.
El borde se invalida al cambiar escena/selección y el inspector rechaza referencias
de una regeneración anterior. No permite aún desplazar una arista escalonada.

La continuación añade esta edición de parámetros sin atribuir una modificación
libre al parche. El diálogo se invalida si cambia el resultado o su fuente. Si
la nueva altura produce una topología no soportada, ninguna malla, altura ni
entrada de undo se modifica. Undo/redo y JSON conservan los UUID.

Los operandos se conservan en el proyecto. En la escena completa cualquier sólido
delante del resultado sigue bloqueando su selección; no se selecciona a través de él.

## Verificación

- Integridad: superada. Se ha actualizado la comprobación del despacho de rayos
  para exigir también el rechazo de generaciones obsoletas y el encuadre aislado.
- Geometría: suite completa anterior superada; nueva regresión de bordes superada
  en los tres planos y ambos signos (incluye exclusión de seams y referencias obsoletas).
  Suite completa de la continuación superada localmente. La receta tiene regresiones en seis
  combinaciones de plano/signo, ambas operaciones y protección de lotes obsoletos.
- Compilación Java de producción y pruebas contra SDK 35: superada.
- Robolectric Android 35: 34 pruebas superadas (24 originales, 5 de selección,
  3 de edición y 2 de bordes); incluye el flujo del
  menú, JSON, oclusión, selección de bolsillo y callbacks descartados.
- Gradle local: bloqueado por `SocketException: Operation not permitted` al crear
  el daemon. Compilación Android y Lint superados en #585 para la etapa anterior;
  la continuación requiere su propio CI.
- Dispositivo Android / EGL real: pendiente; no se afirma aceptación física.
- CI de esta revisión: verificar el SHA de la PR de
  `feature/cad-stepped-boundary-selection` antes de integrar; la publicación de este
  documento precede a su ejecución. Estado del último main integrado y enlace directo
  en [ci-status](https://github.com/josxtm/mycad/blob/ci-status/ESTADO_COMPILACION.md).

## Pendiente

La fase A global continúa: faltan una identidad semántica persistente de todas
las caras/aristas/vértices a través de cambios topológicos, operaciones generales
y más rendimiento. El UUID de receta es estable; el ID interno de shell describe
una geometría concreta y cambia al regenerar otra forma. No confundir ambos.

Siguiente tarea: definir y probar identidad semántica de subentidades a través de
cambios topológicos, antes de persistir selecciones de caras/aristas entre regeneraciones.
Validar selección, pan/zoom/ViewCube y edición en teléfono/tablet. El desplazamiento
libre de parches y aristas escalonados sigue pendiente.

Estimación de la fase A: aproximadamente 65 % del alcance de integración, con
soporte geométrico limitado a las familias actualmente implementadas. No representa
cobertura en dispositivo ni finalización del motor CAD general.

Las fases B–F siguen siendo parciales. Se conservan dibujo, cotas, Push/Pull,
ViewCube, navegación, historial existente y guardado/apertura. No hay un solver
general de restricciones ni soporte completo de superficies curvas o formatos CAD.

## Recuperación

Rama remota de la continuación: `feature/cad-stepped-boundary-selection`.

```bash
git clone https://github.com/josxtm/mycad.git
cd mycad
git switch feature/cad-stepped-boundary-selection
git status --short --branch
cat docs/DEVELOPMENT_STATUS.md
bash scripts/verify-project.sh
gradle :app:testDebugUnitTest :app:lintDebug --no-daemon
```

Requiere JDK 17, Gradle 8.9 y SDK Android 35. Comprobar las verificaciones del SHA
remoto actual antes de fusionar. Nunca rebasar ni forzar ramas compartidas.

En este entorno el acceso Git por terminal no funciona. Se ha preparado el código
en `/workspace/mycad-stepped-boundary`, conservando los checkouts anteriores. El commit
`48348a3` es una materialización local de la base remota, no debe publicarse como
ancestro. La publicación se realiza por el conector GitHub con padre remoto
`bb429e0`, sin cambiar directamente `main`. Para reanudar en un entorno normal, usar la rama
remota, que contiene la historia correcta.
