# Estado de desarrollo de MyCAD

## Base y alcance

- Repositorio exclusivo: `josxtm/mycad`.
- Base remota: `3c3c09b9189ffa69a4e6688f5dbb89c68fbe4e37`, PR #131 integrada.
- Última etapa integrada: identidades con agujeros pasantes y varios componentes.
- [android.yml #592](https://github.com/josxtm/mycad/actions/runs/40724016163): correcto.
- El hito anterior #590 conserva su APK; esta continuación requiere su propio CI.
- Las PR #80/#86 permanecen en borrador, sin fusionar.

## Fase A actual: referencias tipadas y reconciliación de topología

Rama: `feature/cad-topology-reference-reconciliation`.

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

## Verificación de esta revisión

- Integridad y suite geométrica completa de android.yml: superadas localmente.
- Motor: planos XY/XZ/YZ, ambos signos, agujero → apertura lateral, componentes
  independientes, edición dimensional, versiones obsoletas y compatibilidad v1.
- Java SDK 35, lenguaje/bytecode Java 8 y 43 pruebas Robolectric/IO: superadas.
- Regresiones de reconciliación: agujero → apertura lateral mediante el diálogo
  real de editar perfil, undo, referencias creadas/eliminadas, tipo incorrecto,
  otro propietario, versión anterior, cilindros y prismas no ortogonales.
- Cambios analíticos menores que la precisión float de la malla también cambian
  la versión del índice. Las colecciones y los descriptores son inmutables.
- Gradle local sigue restringido por sockets del daemon; compilación Android y
  Lint obligatorios se verifican en la PR de esta rama antes de fusionar.
- El documento se publica con el código antes de CI. Consultar el SHA de la PR
  y el [estado vivo de main](https://github.com/josxtm/mycad/blob/ci-status/ESTADO_COMPILACION.md).
- Validación física Android/EGL y rendimiento en dispositivos: pendientes.
  No bloquean las pruebas automáticas ni se consideran realizados.

El error Java 8 de #591 quedó corregido y verificado por #592 antes de integrar
#131. La comprobación local usa `-source 8 -target 8`; no se desactivaron pruebas.

## Próximas tareas y límites

1. Ampliar el nombrado a las rutas restantes, especialmente resultados de perfiles
   no ortogonales, antes de declarar completada la fase A por software.
2. Usar incidencias de aristas fuente para nombrar vértices nuevos de intersecciones,
   sin transferencias por proximidad a entidades distintas.
3. Después continuar operaciones CAD del motor con regeneración transaccional.

Fase A estimada en 85 %: aún parcial. Las caras de esta nueva familia son quads
del complejo celular, sin agrupación en parches coplanares ni selección interactiva
de todos sus bordes. No se atribuye soporte general de NURBS o cambios topológicos
arbitrarios. Se conservan bocetos, cotas, extrusión, ViewCube, navegación, edición,
undo/redo, guardar/abrir e intercambio existente.

## Recuperación

```bash
git clone https://github.com/josxtm/mycad.git
cd mycad
git switch feature/cad-topology-reference-reconciliation
git status --short --branch
cat docs/DEVELOPMENT_STATUS.md
bash scripts/verify-project.sh
gradle :app:testDebugUnitTest :app:lintDebug --no-daemon
```

JDK 17, Gradle 8.9 y SDK Android 35. Si la PR ya fue integrada, crear una nueva
rama desde origin/main y leer este estado antes de modificar código. No rebasar,
forzar ramas compartidas ni modificar main directamente.

En este entorno el checkout recuperable es `/workspace/mycad-topology-references`.
Git por terminal no tiene acceso de red; la publicación usa el conector GitHub,
con padre remoto `3c3c09b`. Los ancestros de materialización local no se publican.
La historia remota de la rama es la referencia para reanudar en un entorno normal.
