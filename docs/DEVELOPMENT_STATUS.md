# Estado de desarrollo de MyCAD

## Estado actual — preparar validación física de 0.6.2

Comprobado el 29 de octubre de 2026: main y la APK de pruebas corresponden a
`1e2cafa5036e4cdc6a14b333324568f189310b27`.
[android.yml #660](https://github.com/josxtm/mycad/actions/runs/40898190332)
está verde, incluidas compilación, pruebas, Lint, generación y publicación de APK.
[Artefacto 0.6.2](https://github.com/josxtm/mycad/actions/runs/40898190332/artifacts/12908082325).

La rama `docs/android-0.6.2-functional-validation` solo añade documentación y
datos de ensayo. No modifica funciones, fuentes, recursos Android ni workflows.
No se fusiona esta preparación: main permanece igual a la APK que se validará.

- [Guía de validación](ANDROID_0_6_2_VALIDATION.md): 18 casos por dispositivo,
  Nothing Phone 2a y tablet de 12 pulgadas.
- [Resultados](ANDROID_0_6_2_RESULTS.csv): 36 filas PENDIENTE, ninguna prueba física aprobada.
- Proyectos exactos XY/XZ/YZ, cóncavo y resta con agujero; un archivo de versión
  inválida para rechazo. Cinco roundtrips del codec real verificados por software
  y rechazo negativo: cuatro pruebas Robolectric de preparación satisfactorias.
- Los archivos preparados no sustituyen el cierre manual del contorno con el dedo.
  La guía separa el fallo de dibujo de las pruebas independientes del motor/archivo.

Se pausa el desarrollo de nuevas funciones durante esta validación. Tras recibir
y revisar los resultados físicos y sus bloqueos: ViewCube 3D → ampliar operaciones
B-Rep → migración compatible de Sketch/Part antiguos a double. Cualquier PR futura
requiere CI verde, fusionabilidad y ausencia de bloqueos. La CI de esta rama
documental no se presenta como evidencia de pruebas físicas.

## Hito actual — MyCAD 0.6.2, extrusión desde herramientas visibles

Base `da1c3948ee045cda3f8b7edca5191c93e7bdd161`: PR #160 integrada con
[CI #657](https://github.com/josxtm/mycad/actions/runs/40895445172) y
[main #658](https://github.com/josxtm/mycad/actions/runs/40896325233) satisfactorios.
Rama independiente `feature/cad-closed-profile-toolbar`, versión 0.6.2 (62).

El botón lateral «Extruir boceto» y «Extruir seleccionado» del menú ahora reconocen
una línea seleccionada como ancla de un contorno cerrado y abren su extrusión B-Rep.
La geometría se valida antes de publicar. Rectángulos y círculos conservan sus
opciones de arrastre y deslizador. No se ofrece Push/Pull de contornos de líneas
desde el botón contextual: todavía no está implementado ese gesto para esta familia.

91 pruebas Android/IO locales satisfactorias. El recorrido de integración usa
los botones/menús reales para crear dos extrusiones, restarlas, editar una cara,
regenerar la resta, Deshacer/Rehacer y guardar/reabrir. Caso numérico: prisma
40×30×10 mm menos prisma 10×10×10 mm = 11000 mm³; desplazar 5 mm su cara lateral
produce 12500 mm³. Se mantienen UUID de fuentes/operación/caras existentes.
Una edición incompatible de altura del agujero pasante se rechaza sin cambiar
ninguna geometría, historial ni JSON: esta familia exige alturas firmadas iguales.

Geometría completa e integridad se verifican junto con el incremento; CI del SHA
publicado obligatorio antes de integrar. La fusión de hito «release: MyCAD 0.6.2»
solicita una única APK, después de tests, compilación y Lint. Su resultado final,
SHA y artefacto se adjuntan a la PR y ci-status; no se atribuye un APK antes de CI.

Estimación del hito de contornos rectos: ~90%, con casos previstos por software
verificados y aceptación física pendiente. No es el porcentaje del modelador
CAD completo. Nothing Phone 2a y tablet 12 pulgadas: instalar APK, probar dibujo,
selección/diálogo, vistas, edición, cancelación y reapertura; EGL y rendimiento
siguen sin comprobarse en este entorno. Persisten límites float de Sketch/Part
antiguos, tolerancia de unión de extremos de 0,15 mm y geometría curva general.

## Auditoría y continuación — 2026-10-29

Base real: `ea07cc67dac304f4a626949cb2e4a026ffd77f10`, con
[android.yml #656 verde](https://github.com/josxtm/mycad/actions/runs/40882955618).
El commit #104 indicado en instrucciones anteriores ya no es la base actual.
Se incorporan íntegros los avances ajenos #153–#159, #149 y #136. Solo permanecen
abiertas #80 y #86, ambas en borrador; no se tocan ni se fusionan automáticamente.

Auditoría de código y pruebas, distinguiendo implementación real y diagnóstico:

- BoxBRep/PrimitiveBRepBody y PolygonPrismBRep proporcionan entidades analíticas.
  Los contornos cerrados de líneas ya soportan perfiles cóncavos, XY/XZ/YZ,
  alturas firmadas double, caras/aristas con UUID y teselación independiente.
- LoopExtrusionFeature ya enlaza líneas con sólidos. La Activity permite editar
  altura y líneas, guardar/abrir, Deshacer/Rehacer y regenerar booleanas dependientes.
  No se reemplaza esa implementación por otro motor.
- Las booleanas convexas/ortogonales, con varios sólidos, agujeros pasantes y
  alturas escalonadas tienen implementaciones y pruebas delimitadas. No equivalen
  a booleanas B-Rep universales con superficies curvas arbitrarias.
- El visor conserva fuentes double y usa origen relativo compartido con cámara
  y rayos. No todas las familias tienen aún una fuente analítica double completa.
- CadIntegrationProbe/parametricPreview siguen siendo diagnóstico orientativo;
  no son el documento de edición autoritativo. Los sketches y Part Android antiguos
  todavía almacenan float; convertirlos al entrar al kernel no recupera detalle perdido.
- El ViewCube y la navegación existentes se conservan; no se declara aquí un
  ViewCube tridimensional completo ni validación táctil en dispositivo físico.

### Incremento integrado #160: extrusión de contorno transaccional

Rama `feature/cad-closed-profile-transaction`. Una regresión Android falló sobre
la base estable: cambiar una línea mientras estaba abierto el diálogo de altura
publicaba un cuerpo basado en el contorno antiguo y modificaba el historial.

ClosedProfileExtrusionPlan captura UUID, plano y coordenadas double inmutables.
Antes de publicar, compara la selección actual sin depender del orden de la lista,
valida el contorno con el teselador existente y construye su topología analítica.
Rechaza cambios de geometría/plano, líneas añadidas/eliminadas, UUID inválidos o
duplicados, alturas inválidas y desalineación del historial antes del checkpoint.
La fuente double se entrega también al visor. No cambia el formato de proyecto.

El diálogo y menú dejan de describir incorrectamente este flujo como una malla
sin vínculo: ya genera un sólido B-Rep enlazado. No se añade un gesto de arrastre
de contornos que todavía no exista. El Push/Pull de rectángulos/círculos se conserva.

Verificación local satisfactoria: 87 pruebas Android/IO, compilación Java 8/SDK35,
geometría completa de android.yml e integridad. Las pruebas nuevas cubren comparación
double incluso bajo un ULP float, selección por UUID, perfiles rectangulares/cóncavos,
alturas firmadas, tres planos, mapas reordenados, fallo sin mutación y conservación
de identidades al editar altura. Android ejercita menús y diálogos reales,
persistencia, Deshacer/Rehacer y rechazo sin cambiar JSON. El test que antes
reproducía el fallo ahora pasa. CI #657 del SHA publicado y main #658 correctos;
resultados finales adjuntos a la PR y ci-status, evitando commits documentales vacíos.

Limitaciones: el sandbox local impide abrir sockets del daemon Gradle; Java 8/SDK35
y Robolectric permiten pruebas locales y Gradle/Lint completos se exigen en Actions.
No se inspeccionaron vídeos nuevos. Se reutiliza docs/SHAPR3D_ANALISIS.md y sus
fuentes oficiales; actualizar la consulta web falló al conectar al proxy en esta sesión.
EGL, ergonomía, memoria y rendimiento en Nothing Phone 2a/tablet 12 pulgadas pendientes.

El siguiente incremento conecta el botón general con este flujo y prepara el
hito 0.6.2, descrito al inicio del documento. Después: precisión de datos de origen
y modelos dependientes, sin sustituir el motor ni los controles que ya funcionan.

## Registro histórico de temas y precisión — 2026-10-28

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

## Incremento integrado #152: coordenadas de grids con agujeros hasta GPU

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
localmente, con CI #636 y main #637 satisfactorios después de publicar.
El motor global sigue parcial.

## Recuperación

```bash
git clone https://github.com/josxtm/mycad.git
cd mycad
git switch feature/cad-closed-profile-toolbar
git status --short --branch
cat docs/DEVELOPMENT_STATUS.md
bash scripts/verify-project.sh
gradle :app:compileDebugJavaWithJavac :app:testDebugUnitTest :app:lintDebug --no-daemon
```

Si la PR está integrada, crear una rama nueva desde origin/main. No forzar ramas
compartidas ni rebasar trabajo de terceros. Las pruebas nativas generan
`app/build/theme-preview/six-themes.png` y `six-interfaces.png`; su revisión es
software, no una instalación física de la APK.

Checkout local: `/workspace/mycad-profile-toolbar`. Su base materializada comparte el
árbol de main `da1c394`; la publicación por conector usa el padre remoto real,
sin publicar los ancestros sintéticos locales. El experimento antiguo de
propietarios permanece en la rama local WIP `feature/cad-persisted-recipe-owners`
(`b41ec27`), sin pruebas y ajeno a esta fase; no debe fusionarse sin revisión.

Siguiente tarea: ejecutar y registrar la validación física de 0.6.2; después,
ViewCube 3D, ampliar operaciones B-Rep y migrar Sketch/Part a double.
La APK de temas del hito #632 es anterior
a las mejoras B-Rep de esta continuación; sus fuentes corresponden a `5c815ff`.
No se genera otra APK por cada ajuste interno del puente geométrico.
