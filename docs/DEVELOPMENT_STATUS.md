# Estado de desarrollo de MyCAD

## Base verificada

- Repositorio exclusivo: `josxtm/mycad`. Main protegido, sin escrituras directas.
- Base de esta fase: `a60f5e1e2c972b271e1d36fb679bdb242f01bd5b` (PR #151 integrada).
- Temas integrados tras [CI #631](https://github.com/josxtm/mycad/actions/runs/40770661900) verde.
- Main pasó [hito #632](https://github.com/josxtm/mycad/actions/runs/40771113314), incluida APK 0.6.1 compilada y verificada.
- Tras integrar #151, main pasó [CI #634](https://github.com/josxtm/mycad/actions/runs/40773115344).
- Se revisaron main, los cinco últimos flujos y las PR abiertas. No había avances
  remotos nuevos después de #628 ni cambios locales de código sin guardar.
- PR #146: `db6aec2`, sustituida por #150 tras revisión; rama conservada.
- PR #149 de otro agente: #629 falló; el autor publicó `a1aac91`, con
  [CI #635](https://github.com/josxtm/mycad/actions/runs/40773461656) en ejecución al registrar este punto. Rama intacta, sin fusionar aquí.
- PR #80/#86/#136 conservadas, sin fusionar automáticamente.

## Temas completos y persistentes: integrados

PR #150, fuente `560e9d5`. Versión de prueba: 0.6.1 (código 61).
#146 se cerró como sustituida sin fusionarla ni modificar su rama original.

Se conservan de #146 los nombres, orden de los cinco presets y la preferencia
`themeIndex`. El sexto valor es Personalizado. Claro es el predeterminado; se
respetan preferencias antiguas `dark` cuando aún no existe `themeIndex`.

CadAppearance centraliza doce roles de color, independientes del documento CAD:
fondo general, paneles, dibujo, texto, acento, selección, cuadrícula, ejes X/Y/Z,
cuerpos y ViewCube. La paleta es inmutable y ofrece contraste de texto seguro
para superficies personalizadas, incluso si se mezclan colores claros y oscuros.

Ajustes → Apariencia muestra seis tarjetas con una previsualización dibujada por
Android. Personalizado permite editar los doce colores mediante #RRGGBB, con
muestra en vivo y validación. Los cambios de tarjetas o colores permanecen en
un borrador; Cancelar no cambia la aplicación ni guarda preferencias. Aplicar
persiste la selección y los colores, conservados al recrear/reiniciar la actividad.

La misma paleta alcanza barras, paneles, botones, iconos dibujados, Canvas, su
cuadrícula/ejes, cuerpos y selección, tiradores, ViewCube Canvas/GPU y controles
nativos de diálogos/menús/dropdowns. En Android 15, las barras de sistema
transparentes eligen contraste sobre el fondo real del modo edge-to-edge; en
versiones anteriores se utiliza el color de panel solicitado. Los desplegables mantienen datos, ID y opción
seleccionada. El estilo de los diálogos conserva los listeners originales.
GPU usa una referencia inmutable para fondo, cuerpos, selección y tiradores:
no modifica cámaras, origen relativo, revisión de escena, triángulos ni picking.

## Verificación de temas

- 73 pruebas Android/IO locales, incluidas once nuevas de apariencia y renderizado.
- Java 8 sobre SDK 35/JDK 17; geometría completa de android.yml e integridad.
- Migración de preferencias, seis temas tras recreación, personalizado, hex
  inválido, cancelar/aplicar, colores corruptos, menús y desplegables.
- Aislamiento: archivo CAD guardado idéntico byte a byte, mismos objetos de
  boceto/cuerpo, geometría, IDs, cámara, revisión de picking y pilas undo/redo.
- Dos láminas de comprobación visual renderizadas por Android/Skia (Robolectric
  Native): seis tarjetas y seis interfaces completas, inspeccionadas en este
  entorno. CI publica sus PNG junto con los informes Android.
- Gradle local no puede abrir sockets de daemon en este sandbox. Compilación
  Gradle, tests y Lint completos se exigen en GitHub Actions antes de fusionar.
- Esta documentación se publica junto al código antes de CI; el resultado del
  SHA final se registra en la PR y en [ci-status](https://github.com/josxtm/mycad/blob/ci-status/ESTADO_COMPILACION.md).
- EGL/GPU física, ergonomía, memoria y rendimiento en Nothing Phone 2a y tablet
  Android de 12 pulgadas: pendientes. Renderizar con Skia no sustituye esas pruebas.

Temas: ~95 % estimado, software implementado y probado; falta aceptación física.
No se presenta como un 95 % del modelador CAD completo.

## B-Rep: identidades repetibles en el puente paramétrico, integradas

La prueba de aislamiento detectó un comportamiento anterior ajeno a los temas:
CadIntegrationProbe crea UUID nuevos en cada llamada para su previsualización
paramétrica orientativa. El archivo guardado no cambia al elegir un tema; las
identidades B-Rep reales tampoco. En la PR de temas, la comparación del archivo guardado fue completa y exacta;
la comparación de una nueva serialización excluía solo esos campos orientativos.
La presente corrección retira esa exclusión y vuelve a exigir igualdad del JSON
completo en la misma prueba de aislamiento.

PR #151, fuente `6aeb64e`, rama `feature/cad-stable-parametric-preview`,
integrada tras [CI #633](https://github.com/josxtm/mycad/actions/runs/40772532731) verde.
ParametricSketchDocument admite IDs explícitos y canónicos al añadir rectángulos
y extrusiones. Rechaza identidades duplicadas, malformadas o geometría inválida
antes de publicar una revisión; conserva los métodos anteriores, undo y redo.

CadIntegrationProbe reutiliza UUID reales de bocetos/cuerpos del puente Android.
Los ID de operaciones se derivan del propietario mediante un namespace estable.
Edición dimensional y reordenado no reasignan identidades. Los callers antiguos
sin propietario reciben nombres de diagnóstico deterministas por fila: no se
les atribuye estabilidad ante reordenado cuando no existe UUID de origen.
Los propietarios duplicados fallan explícitamente, sin reemplazar otro cuerpo.

La sonda sigue siendo orientativa, restringida a rectángulos. No se modifica su
cálculo geométrico, las poses existentes ni la política de cobertura parcial.
No se declara por esto terminada la migración al motor paramétrico.

Verificación de la continuación:
- 75 Android/IO locales, incluidas serializaciones completas repetidas, archivo
  guardado/reabierto y propietarios de operaciones tras editar dimensiones.
- Prueba pura para XY/XZ/YZ, alturas firmadas, reordenado, ID duplicado/inválido,
  fallback antiguo, fallo sin commit, snapshot y undo/redo.
- Geometría completa e integridad de android.yml; CI Gradle/Lint obligatorio.
- Mejora delimitada de IDs: 100 % de los casos de software previstos verificados
  localmente y CI sobre el SHA publicado satisfactorio. Motor CAD global parcial.

El motor conserva el origen relativo compartido GPU/cámara/rayos y fuentes double
de cajas, cilindros, perfiles vinculados y booleanas planares nombradas (#145,
#147, #148). Siguen pendientes límites de precisión de grids/escalonados, algunas vistas
previas transitorias, geometría degenerada, B-Rep curva general y ViewCube 3D.
No se atribuye recuperación de detalle perdido por casts históricos. El 93 %
antiguo correspondía a otra fase de identidades, no al CAD completo.

## Fase actual: coordenadas analíticas de grids con agujeros hasta GPU

Rama independiente `feature/brep-grid-double-coordinate-bridge`, basada en #151.
OrthogonalGridHoleBRep ya conserva vértices double, pero su conexión anterior
a PlanarBooleanView partía únicamente de la sopa float. Ahora expone una copia
inmutable de las coordenadas analíticas en el mismo orden de cara/triángulo.
PlanarBooleanView entrega esa fuente al origen relativo GPU ya existente,
tanto para escena completa como para el resultado aislado y su reapertura.

No se cambia la discretización, el orden, el winding, el naming ni el formato
guardado/exportado. Un hash tomado antes del cambio protege los bytes float
de 24 resultados: XY/XZ/YZ, alturas firmadas, unión, intersección, agujero y
diferencia con dos sólidos. La prueba exige correspondencia exacta entre cada
esquina renderizada y su cara analítica, volumen local cerrado y selección.

Este cambio conserva detalle double que ya existe; no amplía artificialmente
el dominio aceptado por la validación float del kernel. Sus límites anteriores
siguen vigentes. El naming por coordenadas de grids/escalonados aún agrupa
fuentes mediante float: precisión sub-ULP en esos nombres y su migración
compatible requieren una fase independiente. La PR #149 sigue ajena a esta rama.

Verificación local: 77 pruebas Android/IO satisfactorias, compilación Java 8/SDK 35,
geometría completa de android.yml e integridad. Las pruebas Android nuevas cubren
escena/aislamiento, seis orientaciones/alturas, selección persistente, rayos del
visor y guardar/reabrir. Esperan explícitamente a los comandos de escena del hilo
del visor antes de comprobar su estado; no sustituyen renderizado EGL físico.
CI Gradle/Lint obligatorio sobre el SHA publicado; resultado final en la PR de
esta rama y ci-status. Puente delimitado: 100 % de los casos previstos comprobados
localmente, pendiente CI. El motor global sigue parcial.

## Recuperación

```bash
git clone https://github.com/josxtm/mycad.git
cd mycad
git switch feature/brep-grid-double-coordinate-bridge
git status --short --branch
cat docs/DEVELOPMENT_STATUS.md
bash scripts/verify-project.sh
gradle :app:compileDebugJavaWithJavac :app:testDebugUnitTest :app:lintDebug --no-daemon
```

Si la PR está integrada, crear una rama nueva desde origin/main. No forzar ramas
compartidas ni rebasar trabajo de terceros. Las pruebas nativas generan
`app/build/theme-preview/six-themes.png` y `six-interfaces.png`; su revisión es
software, no una instalación física de la APK.

Checkout local: `/workspace/mycad-grid-double`. Su base materializada comparte el
árbol de main `a60f5e1`; la publicación por conector usa el padre remoto real,
sin publicar los ancestros sintéticos locales. El experimento antiguo de
propietarios permanece en la rama local WIP `feature/cad-persisted-recipe-owners`
(`b41ec27`), sin pruebas y ajeno a esta fase; no debe fusionarse sin revisión.

Siguiente tarea: coordinar la revisión de la PR #149 cuando su autor resuelva
CI, y continuar precisión/degeneración de perfiles, naming de grids y sólidos
escalonados sin tocar sus ramas activas. La APK de temas del hito #632 es anterior
a las mejoras B-Rep de esta continuación; sus fuentes corresponden a `5c815ff`.
No se genera otra APK por cada ajuste interno del puente geométrico.
