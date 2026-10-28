# Estado de desarrollo de MyCAD

## Base y alcance

- Repositorio exclusivo: `josxtm/mycad`.
- Base remota: `8340165bacb4feefe7f164226c8ca73d8487342c`, PR #130 integrada.
- Última etapa integrada: identidades de origen para sólidos escalonados.
- [android.yml #590](https://github.com/josxtm/mycad/actions/runs/40719663983): correcto, incluida APK del hito anterior.
- Las PR #80/#86 permanecen en borrador, sin fusionar.

## Fase A actual: identidades con agujeros y varios componentes

Rama: `feature/cad-named-hole-topology`.

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

## Verificación de esta revisión

- Integridad y suite geométrica completa de android.yml: superadas localmente.
- Motor: planos XY/XZ/YZ, ambos signos, agujero → apertura lateral, componentes
  independientes, edición dimensional, versiones obsoletas y compatibilidad v1.
- Java SDK 35 con nivel de lenguaje y bytecode Java 8 y 40 pruebas Robolectric/IO:
  comprobación local alineada con Android.
- Gradle local sigue restringido por sockets del daemon; compilación Android y
  Lint obligatorios se verifican en la PR de esta rama antes de fusionar.
- El documento se publica con el código antes de CI. Consultar el SHA de la PR
  y el [estado vivo de main](https://github.com/josxtm/mycad/blob/ci-status/ESTADO_COMPILACION.md).
- Validación física Android/EGL y rendimiento en dispositivos: pendientes.
  No bloquean las pruebas automáticas ni se consideran realizados.

CI #591 pasó integridad/geometría y detectó un uso de `var` en una prueba Android.
Se sustituyó por el tipo explícito; la compilación local ahora también usa
`-source 8 -target 8`. La corrección requiere su nuevo CI, sin desactivar pruebas.

## Próximas tareas y límites

1. Reconciliar explícitamente entidades conservadas, creadas y eliminadas.
2. Ofrecer referencias tipadas que distingan identidad persistente y versión
   geométrica, también para prismas poligonales y primitivas analíticas.
3. Ampliar el nombrado a las rutas restantes, especialmente resultados de perfiles
   no ortogonales, antes de declarar completada la fase A por software.
4. Después continuar operaciones CAD del motor con regeneración transaccional.

Fase A estimada en 80 %: aún parcial. Las caras de esta nueva familia son quads
del complejo celular, sin agrupación en parches coplanares ni selección interactiva
de todos sus bordes. No se atribuye soporte general de NURBS o cambios topológicos
arbitrarios. Se conservan bocetos, cotas, extrusión, ViewCube, navegación, edición,
undo/redo, guardar/abrir e intercambio existente.

## Recuperación

```bash
git clone https://github.com/josxtm/mycad.git
cd mycad
git switch feature/cad-named-hole-topology
git status --short --branch
cat docs/DEVELOPMENT_STATUS.md
bash scripts/verify-project.sh
gradle :app:testDebugUnitTest :app:lintDebug --no-daemon
```

JDK 17, Gradle 8.9 y SDK Android 35. Si la PR ya fue integrada, crear una nueva
rama desde origin/main y leer este estado antes de modificar código. No rebasar,
forzar ramas compartidas ni modificar main directamente.

En este entorno el checkout recuperable es `/workspace/mycad-hole-identities`.
Git por terminal no tiene acceso de red; la publicación usa el conector GitHub,
con padre remoto `8340165`. Los ancestros de materialización local no se publican.
La historia remota de la rama es la referencia para reanudar en un entorno normal.
