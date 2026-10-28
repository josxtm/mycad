# Estado de desarrollo de MyCAD

## Base y alcance

- Repositorio exclusivo: `josxtm/mycad`.
- Base remota: `0a443419f1ca4d4e0ddf4934ee9ee07c65f47d11`, PR #133 integrada.
- Última etapa integrada: intersecciones convexas nombradas y referencias tipadas.
- [android.yml #597](https://github.com/josxtm/mycad/actions/runs/40730118057): correcto.
- El hito anterior #597 conserva su APK; esta continuación requiere su propio CI.
- Las PR #80/#86 permanecen en borrador, sin fusionar.

## Fase A actual: identidades de booleanas planares y componentes

Rama: `feature/cad-named-planar-booleans`.

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

`NamedPlanarBoolean` añade las once rutas restantes de recetas planares:
UNION/INTERSECTION/DIFFERENCE_ANALYTIC, UNION/DIFFERENCE_ORTHOGONAL,
UNION/INTERSECTION/DIFFERENCE_POLYGONAL y las tres MULTISHELL. Cada ruta llama
exactamente a su kernel histórico y conserva las coordenadas, el orden de los
cascarones y todos los triángulos persistidos. Incluye la unión inclinada convexa,
la unión anidada que reutiliza un cascarón origen y las intersecciones cóncavas de
altura desigual. La identidad del propietario sigue siendo el UUID de la receta.

Las caras laterales incluyen las identidades de sus extremos para distinguir
segmentos cóncavos separados sobre el mismo plano. Las tapas de un solo cascarón
mantienen su rol semántico. En MULTISHELL cada componente se identifica por su
contorno de procedencia, sin índices ni coordenadas: una separación/fusión retira
las tapas anteriores y crea referencias para las nuevas. Es una política
conservadora: otros cambios del contorno también pueden invalidar sus tapas.
No se adivina una transferencia por proximidad o volumen. Las entidades de
frontera que conservan procedencia pueden permanecer entre componentes.

El adaptador común conecta estas rutas al visor, al inspector, a la selección de
la cara completa y a la reconciliación transaccional. Rechaza mallas adulteradas,
versiones obsoletas y cambios de propietario. Los namespaces publicados de
`NamedSteppedBody`, `NamedGridHoleBody` y `NamedConvexIntersection` no se alteran.

## Verificación de esta revisión

- Integridad y suite geométrica completa de android.yml: superadas localmente.
- Motor: planos XY/XZ/YZ, ambos signos, agujero → apertura lateral, componentes
  independientes, edición dimensional, versiones obsoletas y compatibilidad v1.
- Java SDK 35, lenguaje/bytecode Java 8 y 51 pruebas Robolectric/IO: superadas.
- Regresiones de reconciliación: agujero → apertura lateral mediante el diálogo
  real de editar perfil, undo, referencias creadas/eliminadas, tipo incorrecto,
  otro propietario, versión anterior, cilindros y prismas no ortogonales.
- Cambios analíticos menores que la precisión float de la malla también cambian
  la versión del índice. Las colecciones y los descriptores son inmutables.
- Intersección inclinada: seis combinaciones plano/signo, cambios dimensionales,
  traslaciones, nacimiento/desaparición de vértices, cambio del límite de altura,
  perfil colineal y triángulo agudo a coordenadas altas.
- Once rutas: comparación exacta contra las mallas históricas, todos los planos
  y signos, unión inclinada, U cóncava, separación/fusión y reordenado de componentes.
- Android: selección/reapertura de las once rutas, separación mediante edición
  real del lado de origen, undo y rechazo transaccional de mallas adulteradas.
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

1. Auditar las recetas de perforación circular y de extrusión de cara que aún
   almacenan mallas sin propietario UUID persistido. Ampliar su representación
   analítica/topológica sin presentar facetas de teselación como caras curvas.
2. Revisar la cobertura de cadenas de operaciones y la persistencia explícita
   de referencias de subentidades, con migración de proyectos anteriores.
3. Completar la aceptación automática de la fase A antes de pasar a ampliar
   las operaciones CAD de la fase B. La validación física sigue independiente.

Fase A estimada en 93 %: aún parcial. Todas las operaciones de
`LinkedConvexIntersection` tienen ya adaptación nombrada, pero esto no equivale
al soporte de todas las familias CAD. Las perforaciones circulares existentes
siguen produciendo mallas; no son todavía una B-Rep curva completa. No hay
selección interactiva de todas las aristas ni soporte general de NURBS/cambios
topológicos arbitrarios. Se conservan bocetos, cotas, extrusión, ViewCube,
navegación, edición, undo/redo, guardar/abrir e intercambio existente.

## Recuperación

```bash
git clone https://github.com/josxtm/mycad.git
cd mycad
git switch feature/cad-named-planar-booleans
git status --short --branch
cat docs/DEVELOPMENT_STATUS.md
bash scripts/verify-project.sh
gradle :app:testDebugUnitTest :app:lintDebug --no-daemon
```

JDK 17, Gradle 8.9 y SDK Android 35. Si la PR ya fue integrada, crear una nueva
rama desde origin/main y leer este estado antes de modificar código. No rebasar,
forzar ramas compartidas ni modificar main directamente.

En este entorno el checkout recuperable es `/workspace/mycad-planar-identities`.
Git por terminal no tiene acceso de red; la publicación usa el conector GitHub,
con padre remoto `0a44341`. Los ancestros de materialización local no se publican.
La historia remota de la rama es la referencia para reanudar en un entorno normal.
