# Estado de desarrollo de MyCAD

## Base verificada

- Repositorio exclusivo: `josxtm/mycad`.
- Base de esta continuación: `f3cbb300724999bf150e2ded81551862993c2273` (PR #129 integrada).
- Última etapa completada: selección e inspección de bordes B-Rep (#129), tras
  vinculación de recetas (#127) y edición transaccional de alturas (#128).
- CI del hito: [android.yml #588](https://github.com/josxtm/mycad/actions/runs/40716562543), correcto,
  incluidos compilación Android, pruebas y Lint. Integrada sin saltar protecciones.
- PR #80 y #86: borradores conservados, sin fusionar.

## Fase actual — A: identidad de entidades separada de la versión geométrica

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

### Identidades de origen

`NamedSteppedBody` mantiene el UUID de receta como identidad del cuerpo y genera
nombres de vértices, aristas, caras quad y parches desde los UUID de vértices de los
operandos que originan cada coordenada del grid. Los inspectores de caras/aristas
Android muestran estos nombres junto con la versión geométrica correspondiente.

Los nombres sobreviven a cambios de altura/anchura mientras se conservan los
operandos ordenados, operación, plano y agrupaciones de coordenadas de origen.
Una coincidencia que aparece/desaparece cambia los nombres afectados; cambiar
operación u orden de operandos invalida el namespace. Se rechazan nombres ambiguos.
No hay transferencia por proximidad geométrica ni promesa de identidad universal
tras cambios topológicos arbitrarios.

Los nombres se reconstruyen desde los UUID ya guardados: no se cambia el JSON ni
se guardan tablas redundantes. El ID interno del shell sigue describiendo una
geometría concreta. Una identidad persistente válida no autoriza a reutilizar un
rayo, índice de triángulo o parche de otra versión.

La continuación añade esta edición de parámetros sin atribuir una modificación
libre al parche. El diálogo se invalida si cambia el resultado o su fuente. Si
la nueva altura produce una topología no soportada, ninguna malla, altura ni
entrada de undo se modifica. Undo/redo y JSON conservan los UUID.

Los operandos se conservan en el proyecto. En la escena completa cualquier sólido
delante del resultado sigue bloqueando su selección; no se selecciona a través de él.

## Verificación

- Integridad: superada. Se ha actualizado la comprobación del despacho de rayos
  para exigir también el rechazo de generaciones obsoletas y el encuadre aislado.
- Geometría: suite completa superada; nueva regresión de bordes superada
  en los tres planos y ambos signos (incluye exclusión de seams y referencias obsoletas).
  Identidades comprobadas en seis combinaciones de plano/signo: dimensiones editadas,
  coordenadas coincidentes, cambios de operación/operandos y referencias obsoletas.
  La receta tiene regresiones en seis
  combinaciones de plano/signo, ambas operaciones y protección de lotes obsoletos.
- Compilación Java de producción y pruebas contra SDK 35: superada.
- Robolectric Android 35: 36 pruebas superadas (24 originales, 5 de selección,
  3 de edición, 2 de bordes y 2 de identidad); incluye el flujo del
  menú, JSON, oclusión, selección de bolsillo y callbacks descartados.
- Gradle local: bloqueado por `SocketException: Operation not permitted` al crear
  el daemon. Compilación Android y Lint superados en #588 para la etapa anterior;
  la continuación requiere su propio CI.
- Dispositivo Android / EGL real: pendiente; no se afirma aceptación física.
- CI de esta revisión: verificar el SHA de la PR de
  `feature/cad-stepped-entity-identities` antes de integrar; la publicación de este
  documento precede a su ejecución. Estado del último main integrado y enlace directo
  en [ci-status](https://github.com/josxtm/mycad/blob/ci-status/ESTADO_COMPILACION.md).

## Pendiente

La fase A global continúa: faltan identidad de subentidades en más familias de
sólidos y ante cambios topológicos generales, operaciones generales y rendimiento.
El UUID de receta y los nombres de origen son distintos del ID interno de shell.

Siguiente tarea: validar el hito en teléfono/tablet y ampliar selección/identidad
a más familias geométricas. Antes de operaciones libres sobre parches o aristas,
definir cómo invalidar dependencias cuando desaparece una entidad de origen.
El desplazamiento libre de parches y aristas escalonados sigue pendiente.

Estimación de la fase A: aproximadamente 75 % del alcance de integración, con
soporte geométrico limitado a las familias actualmente implementadas. No representa
cobertura en dispositivo ni finalización del motor CAD general.

Las fases B–F siguen siendo parciales. Se conservan dibujo, cotas, Push/Pull,
ViewCube, navegación, historial existente y guardado/apertura. No hay un solver
general de restricciones ni soporte completo de superficies curvas o formatos CAD.

## Recuperación

Rama remota de la continuación: `feature/cad-stepped-entity-identities`.

```bash
git clone https://github.com/josxtm/mycad.git
cd mycad
git switch feature/cad-stepped-entity-identities
git status --short --branch
cat docs/DEVELOPMENT_STATUS.md
bash scripts/verify-project.sh
gradle :app:testDebugUnitTest :app:lintDebug --no-daemon
```

Requiere JDK 17, Gradle 8.9 y SDK Android 35. Comprobar las verificaciones del SHA
remoto actual antes de fusionar. Nunca rebasar ni forzar ramas compartidas.

En este entorno el acceso Git por terminal no funciona. Se ha preparado el código
en `/workspace/mycad-stepped-identities`, conservando los checkouts anteriores. El commit
`48348a3` es una materialización local de la base remota, no debe publicarse como
ancestro. La publicación se realiza por el conector GitHub con padre remoto
`f3cbb30`, sin cambiar directamente `main`. Para reanudar en un entorno normal, usar la rama
remota, que contiene la historia correcta.
