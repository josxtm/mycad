# Estado de desarrollo de MyCAD

## Base y alcance

- Repositorio exclusivo: `josxtm/mycad`.
- Base remota: `0c57f8e7d8d44032ffb4754f4a38ce0c2354a72b`, PR #132 integrada.
- Última etapa integrada: referencias tipadas y reconciliación de topología.
- [android.yml #594](https://github.com/josxtm/mycad/actions/runs/40725744292): correcto.
- El hito anterior #590 conserva su APK; esta continuación requiere su propio CI.
- Las PR #80/#86 permanecen en borrador, sin fusionar.

## Fase A actual: identidades de intersecciones convexas no ortogonales

Rama: `feature/cad-named-convex-intersections`.

`NamedGridHoleBody` asigna nombres persistentes a vértices, aristas y caras de
booleanas ortogonales de igual altura, incluidos agujeros pasantes y varios
componentes. Las recetas existentes UNION_WITH_HOLES, DIFFERENCE_WITH_HOLES e
INTERSECTION_WITH_HOLES pueden regenerar su topología nombrada.

El visor Android conserva una asociación exacta entre receta, malla renderizada y
topología. Selecciona la cara quad del resultado visible y muestra su área e
identidad desde **Inspeccionar caras**. En **Resultados booleanos → Ver solo esta
malla** se puede seleccionar sin los operandos originales delante. Las selecciones
y el inspector rechazan sustituciones de receta/malla y geometría adulterada.

`SourceTopologyNames` comparte el nombrado con los sólidos escalonados conservando
exactamente el formato v1 publicado. La prueba de compatibilidad congela el digest
SHA-256 de todos los nombres de doce fixtures de `b74c4e7`. No se cambia el JSON,
los UUID persistidos, la teselación ni el orden global de triángulos.

Los nombres sobreviven a cambios dimensionales mientras se conservan los
operandos ordenados, operación, plano y agrupaciones de coordenadas de origen.
Los cambios topológicos pueden eliminar nombres y crear otros: no se transfieren
por proximidad a entidades diferentes. La versión geométrica sigue separada de
la identidad y no autoriza reutilizar impactos de rayos/triángulos anteriores.

`TopologySnapshot` indexa vértices, aristas y caras con tipo, propietario e identidad,
y calcula una versión geométrica SHA-256 que incluye los datos analíticos exactos.
Cubre cuerpos escalonados, grids con agujeros, prismas poligonales de líneas y
primitivas de caja/cilindro. El cilindro tiene dos bordes circulares y tres caras;
no se agregan vértices artificiales de costura o de teselación.

Las referencias pueden restaurarse como descriptores, pero se resuelven después:
`resolveExact` exige la misma versión geométrica y `resolvePersistent` requiere
una decisión explícita de vincular la misma identidad al estado actual. Ninguno
permite reutilizar un rayo o índice GPU de una generación anterior.

`TopologyReconciliation` informa por tipo qué entidades se conservan, se crean o
se eliminan. Solo vuelve a vincular referencias de su snapshot anterior exacto.
El visor publica todos sus índices tras validar la escena completa y conserva el
último cambio para los inspectores. Elimina los registros de cuerpos borrados.
Un fallo de geometría no publica índices parciales ni altera JSON/undo.

`NamedConvexIntersection` cubre la receta histórica INTERSECTION entre prismas
convexos, incluidos lados inclinados. Los vértices conservados se vinculan a sus
esquinas exactas de origen y los nuevos cruces a las líneas de soporte activas.
La incidencia admite el redondeo float de la vista previa; no compara posiciones
con una geometría anterior. Los casos agudos lejos del origen y las cadenas
colineales tienen regresiones específicas.

Las tapas conservan su rol base/superior aunque cambie el número de esquinas del
perfil. Cambiar qué operando limita la altura invalida los nombres superiores.
Las caras laterales se identifican por sus soportes de origen. Propietarios,
planos u orden de operandos diferentes no comparten el namespace.

`PlanarBooleanView` permite que agujeros e intersecciones usen la misma selección
e inspección. Resalta todos los triángulos de una cara (incluidas tapas de más de
dos triángulos). La asociación también exige la revisión de la escena realmente
instalada: una previsualización temporal o un visor oculto no pueden reutilizarla.
El JSON, las recetas y la teselación se conservan.

## Verificación de esta revisión

- Integridad y suite geométrica completa de android.yml: superadas localmente.
- Motor: planos XY/XZ/YZ, ambos signos, agujero → apertura lateral, componentes
  independientes, edición dimensional, versiones obsoletas y compatibilidad v1.
- Java SDK 35, lenguaje/bytecode Java 8 y 47 pruebas Robolectric/IO: superadas.
- Regresiones de reconciliación: agujero → apertura lateral mediante el diálogo
  real de editar perfil, undo, referencias creadas/eliminadas, tipo incorrecto,
  otro propietario, versión anterior, cilindros y prismas no ortogonales.
- Cambios analíticos menores que la precisión float de la malla también cambian
  la versión del índice. Las colecciones y los descriptores son inmutables.
- Intersección inclinada: seis combinaciones plano/signo, cambios dimensionales,
  traslaciones, nacimiento/desaparición de vértices, cambio del límite de altura,
  perfil colineal y triángulo agudo a coordenadas altas.
- Android: tapa completa, persistencia, edición real del perfil origen,
  previsualización temporal y eventos pendientes de un visor oculto.
- Gradle local sigue restringido por sockets del daemon; compilación Android y
  Lint obligatorios se verifican en la PR de esta rama antes de fusionar.
- El documento se publica con el código antes de CI. Consultar el SHA de la PR
  y el [estado vivo de main](https://github.com/josxtm/mycad/blob/ci-status/ESTADO_COMPILACION.md).
- Validación física Android/EGL y rendimiento en dispositivos: pendientes.
  No bloquean las pruebas automáticas ni se consideran realizados.

El error Java 8 de #591 quedó corregido y verificado por #592 antes de integrar
#131. La comprobación local usa `-source 8 -target 8`; no se desactivaron pruebas.

## Próximas tareas y límites

1. Ampliar el nombrado a uniones/restas y a las rutas restantes (por ejemplo
   UNION_ANALYTIC, INTERSECTION_ANALYTIC y resultados poligonales cóncavos), antes
   de declarar completada la fase A por software. La nueva ruta convexa se activa
   para INTERSECTION; no sustituye silenciosamente las demás.
2. Definir la identidad de componentes al separarse/fusionarse y ampliar el
   soporte de B-Rep curva reconstruible, manteniendo invalidez explícita.
3. Después continuar operaciones CAD del motor con regeneración transaccional.

Fase A estimada en 90 %: aún parcial. Las caras con agujeros siguen siendo quads
del complejo celular; las intersecciones tienen tapas poligonales y caras laterales.
No hay selección interactiva de todos sus bordes. No se atribuye soporte general de NURBS o cambios topológicos
arbitrarios. Se conservan bocetos, cotas, extrusión, ViewCube, navegación, edición,
undo/redo, guardar/abrir e intercambio existente.

## Recuperación

```bash
git clone https://github.com/josxtm/mycad.git
cd mycad
git switch feature/cad-named-convex-intersections
git status --short --branch
cat docs/DEVELOPMENT_STATUS.md
bash scripts/verify-project.sh
gradle :app:testDebugUnitTest :app:lintDebug --no-daemon
```

JDK 17, Gradle 8.9 y SDK Android 35. Si la PR ya fue integrada, crear una nueva
rama desde origin/main y leer este estado antes de modificar código. No rebasar,
forzar ramas compartidas ni modificar main directamente.

En este entorno el checkout recuperable es `/workspace/mycad-convex-identities`.
Git por terminal no tiene acceso de red; la publicación usa el conector GitHub,
con padre remoto `0c57f8e`. Los ancestros de materialización local no se publican.
La historia remota de la rama es la referencia para reanudar en un entorno normal.
