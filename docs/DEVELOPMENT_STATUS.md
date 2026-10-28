# Estado de desarrollo de MyCAD

## Base verificada

- Repositorio exclusivo: `josxtm/mycad`. Main protegido, sin escrituras directas.
- Base de esta fase: `06c34bcd8e9ac34f99fba1fa60c32293c9072013`.
- Última integración conocida al empezar: PR #148; [android.yml #628](https://github.com/josxtm/mycad/actions/runs/40765637243) verde, incluida APK.
- Se revisaron main, los cinco últimos flujos y las PR abiertas. No había avances
  remotos nuevos después de #628 ni cambios locales de código sin guardar.
- PR #146: `db6aec2`, CI verde pero implementación parcial, sin personalizado ni
  propagación completa. Su rama permanece intacta; esta fase crea una sucesora.
- PR #80/#86/#136 conservadas, sin fusionar automáticamente.

## Fase actual: temas completos y persistentes

Rama: `feature/ui-complete-persistent-themes`. Versión de prueba: 0.6.1 (código 61).

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

## Siguiente fase B-Rep delimitada

La prueba de aislamiento detectó un comportamiento anterior ajeno a los temas:
CadIntegrationProbe crea UUID nuevos en cada llamada para su previsualización
paramétrica orientativa. El archivo guardado no cambia al elegir un tema; las
identidades B-Rep reales tampoco. La comparación de contenido CAD excluye esos
campos diagnósticos volátiles y verifica el archivo guardado completo por separado.

Tras integrar temas con CI verde, estabilizar las identidades de ese puente en
una PR independiente: identificadores de origen explícitos, edición/regeneración,
serialización repetible y regresiones. No mezclar esa corrección del motor con
el cambio de apariencia. Mantener la geometría y los límites soportados.

El motor conserva el origen relativo compartido GPU/cámara/rayos y fuentes double
de cajas, cilindros, perfiles vinculados y booleanas planares nombradas (#145,
#147, #148). Siguen pendientes precisión de grids/escalonados, algunas vistas
previas transitorias, geometría degenerada, B-Rep curva general y ViewCube 3D.
No se atribuye recuperación de detalle perdido por casts históricos. El 93 %
antiguo correspondía a otra fase de identidades, no al CAD completo.

## Recuperación

```bash
git clone https://github.com/josxtm/mycad.git
cd mycad
git switch feature/ui-complete-persistent-themes
git status --short --branch
cat docs/DEVELOPMENT_STATUS.md
bash scripts/verify-project.sh
gradle :app:compileDebugJavaWithJavac :app:testDebugUnitTest :app:lintDebug --no-daemon
```

Si la PR está integrada, crear una rama nueva desde origin/main. No forzar ramas
compartidas ni rebasar trabajo de terceros. Las pruebas nativas generan
`app/build/theme-preview/six-themes.png` y `six-interfaces.png`; su revisión es
software, no una instalación física de la APK.

Checkout local: `/workspace/mycad-themes`. Su base materializada comparte el
árbol de main `06c34bc`; la publicación por conector usa el padre remoto real,
sin publicar los ancestros sintéticos locales. El experimento antiguo de
propietarios permanece en la rama local WIP `feature/cad-persisted-recipe-owners`
(`b41ec27`), sin pruebas y ajeno a esta fase; no debe fusionarse sin revisión.
