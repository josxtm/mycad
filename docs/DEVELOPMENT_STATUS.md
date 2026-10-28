# Estado de desarrollo de MyCAD

## Base verificada

- Repositorio exclusivo: `josxtm/mycad`.
- Base de esta continuación: `c3740cd67cf85b93734ca76c06b839f74b4fb22c` (PR #127 integrada).
- Última etapa completada: vinculación de recetas escalonadas con selección de
  parches Android (PR #127, commit `0e3afe7`).
- CI de esa etapa: [android.yml #583](https://github.com/josxtm/mycad/actions/runs/40713496874), correcto,
  incluidos compilación Android, pruebas y Lint. Integrada sin saltar protecciones.
- PR #80 y #86: borradores conservados, sin fusionar.

## Fase actual — A: edición de parámetros desde caras vinculadas

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

La continuación añade esta edición de parámetros sin atribuir una modificación
libre al parche. El diálogo se invalida si cambia el resultado o su fuente. Si
la nueva altura produce una topología no soportada, ninguna malla, altura ni
entrada de undo se modifica. Undo/redo y JSON conservan los UUID.

Los operandos se conservan en el proyecto. En la escena completa cualquier sólido
delante del resultado sigue bloqueando su selección; no se selecciona a través de él.

## Verificación

- Integridad: superada. Se ha actualizado la comprobación del despacho de rayos
  para exigir también el rechazo de generaciones obsoletas y el encuadre aislado.
- Geometría: suite completa de `android.yml` superada; nueva regresión en seis
  combinaciones de plano/signo, ambas operaciones y protección de lotes obsoletos.
- Compilación Java de producción y pruebas contra SDK 35: superada.
- Robolectric Android 35: 32 pruebas superadas (24 originales, 5 de selección y
  3 de edición); incluye el flujo del
  menú, JSON, oclusión, selección de bolsillo y callbacks descartados.
- Gradle local: bloqueado por `SocketException: Operation not permitted` al crear
  el daemon. Compilación Android y Lint superados en #583 para la etapa anterior;
  la continuación requiere su propio CI.
- Dispositivo Android / EGL real: pendiente; no se afirma aceptación física.
- CI y PR de esta rama: pendientes de publicación y verificación.

## Pendiente

La fase A global continúa: faltan una identidad semántica persistente de todas
las caras/aristas/vértices a través de cambios topológicos, operaciones generales
y más rendimiento. El UUID de receta es estable; el ID interno de shell describe
una geometría concreta y cambia al regenerar otra forma. No confundir ambos.

Siguiente tarea: selección e inspección de los bordes topológicos de un parche
escalonado, con rechazo de referencias obsoletas, y validar el recorrido en teléfono
y tablet. La modificación libre de un parche y las aristas escalonadas quedan pendientes.

Las fases B–F siguen siendo parciales. Se conservan dibujo, cotas, Push/Pull,
ViewCube, navegación, historial existente y guardado/apertura. No hay un solver
general de restricciones ni soporte completo de superficies curvas o formatos CAD.

## Recuperación

Rama remota de la continuación: `feature/cad-stepped-parameter-edit`.

```bash
git clone https://github.com/josxtm/mycad.git
cd mycad
git switch feature/cad-stepped-parameter-edit
git status --short --branch
cat docs/DEVELOPMENT_STATUS.md
bash scripts/verify-project.sh
gradle :app:testDebugUnitTest :app:lintDebug --no-daemon
```

Requiere JDK 17, Gradle 8.9 y SDK Android 35. Comprobar las verificaciones del SHA
remoto actual antes de fusionar. Nunca rebasar ni forzar ramas compartidas.

En este entorno el acceso Git por terminal no funciona. Se ha preparado el código
en `/workspace/mycad-stepped-edit`, conservando los checkouts anteriores. El commit
`48348a3` es una materialización local de la base remota, no debe publicarse como
ancestro. La publicación se realiza por el conector GitHub con padre remoto
`c3740cd`, sin cambiar directamente `main`. Para reanudar en un entorno normal, usar la rama
remota, que contiene la historia correcta.
