# MyCAD 0.6.2 — cobertura automatizada y aceptación física

Los 18 casos se repiten en dos dispositivos: **las 36 filas de
[resultados físicos](ANDROID_0_6_2_RESULTS.csv) siguen PENDIENTE**.
La automatización ejecuta el código de esta rama en JVM/Robolectric API 35,
sin instalar la APK ni utilizar un controlador EGL real. La APK de referencia
sigue siendo `1e2cafa5036e4cdc6a14b333324568f189310b27`, flujo
[android.yml #660](https://github.com/josxtm/mycad/actions/runs/40898190332).
Las comprobaciones actuales incorporan `main` `8b3e580` (#166 y #167).

## Matriz de los 36 casos

Cada fila siguiente cubre la parte automatizable de las dos filas físicas del
mismo caso. No representa dos pruebas ejecutadas en hardware. Los nombres de
clase enlazan las pruebas; los métodos concretos se indican cuando la cobertura
se limita a un comportamiento.

| Caso (2 dispositivos) | Evidencia automatizada | Pendiente en dispositivo |
| --- | --- | --- |
| INST-01 | Compilación Android/Lint; creación de Activity en [Release062ValidationAndroidTest](../app/src/test/java/es/josxtm/mycad/Release062ValidationAndroidTest.java). | Instalación/actualización de la APK, inicio real y modelo/API. |
| DIB-01 | [SketchPushPullAndroidTest](../app/src/test/java/es/josxtm/mycad/SketchPushPullAndroidTest.java): `drawDimensionExtrudeEditSaveAndReopenProductionJson` para dibujo/edición; fixtures de líneas y extrusión en `actualFixturesExtrudeAcrossPlanesSignsAndConcavity`. | Dibujo de cuatro líneas con el dedo, cierre y ergonomía. |
| DIB-02 | [ClosedProfileTransactionAndroidTest](../app/src/test/java/es/josxtm/mycad/ClosedProfileTransactionAndroidTest.java): `removedSourceCannotPublishAStaleContour`; [ClosedSketchExtrusionTest](../tests/ClosedSketchExtrusionTest.java): rechazo de contorno abierto. | Dibujar la U y comprobar mensaje/selección táctil. |
| EXT-01 | `Release062ValidationAndroidTest.actualFixturesExtrudeAcrossPlanesSignsAndConcavity`: fixture XY real, ±10 mm, 12000 mm³, topología y reapertura. | Apariencia de tapas y caras en GPU real. |
| EXT-02 | El mismo test con fixture XZ, signos ±, eje y volumen. | Orientación visual en XZ. |
| EXT-03 | El mismo test con fixture YZ, signos ±, eje y volumen. | Orientación visual en YZ. |
| EXT-04 | El mismo test con fixture cóncavo, ±10 mm y 8000 mm³. | Apariencia de la L, sin huecos ni rellenos. |
| EXT-05 | `ClosedProfileTransactionAndroidTest.invalidHeightsAndCancelPreserveTheDocument`: 0, NaN, Infinity, 10001 y cancelación sin mutación. | Accesibilidad del diálogo y mensajes. |
| NAV-01 | `SketchPushPullAndroidTest`: `backgroundDragOrbitsAndFollowingTapSelectsProjectedProfile`, `secondFingerCancelsPreviewAndPansWithoutJump`, `secondFingerCanZoomAfterCancellingExtrusion`; [RelativeGpuCoordinatesAndroidTest](../app/src/test/java/es/josxtm/mycad/RelativeGpuCoordinatesAndroidTest.java) para cámara/rayo. | Gestos en GPU, precisión táctil, latencia y rendimiento. |
| NAV-02 | [CameraOrientationSyncTest](../tests/CameraOrientationSyncTest.java), [CubeCameraPresetTest](../tests/CubeCameraPresetTest.java) y `SketchPushPullAndroidTest.cubeTakesOwnershipBeforeExtrusion`. | Concordancia visual Canvas/GPU y control SUP/FREN/DER/ISO. |
| EDI-01 | `ClosedProfileTransactionAndroidTest.productionMenusExtrudeEditUndoRedoAndReopenEveryPlaneWithExactHeights`: identidad, cotas, volumen, deshacer/rehacer y codec real. | Secuencia táctil 10 → 12 → deshacer/rehacer. |
| PER-01 | `Release062ValidationAndroidTest.externalRoundtripAndConfirmedNewDesignRefreshGpuWithoutToggling` y `openFileReplacesTheVisibleGpuSceneAndItsSelection`: callbacks reales de archivo, datos persistidos e instalación de escena. | Selector SAF y proveedor de archivos reales; dibujo EGL. |
| PER-02 | `Release062ValidationAndroidTest.savedDraftSurvivesAFreshActivityWithStableBodyIdentity`. | Forzar detención real, relanzar proceso y comprobar borrador. |
| PER-03 | Roundtrip del fixture/archivo mediante el codec de producción, identidad y volumen. | Transferencia teléfono ↔ tablet, proveedores y almacenamiento de ambos. |
| PER-04 | `Release062ValidationAndroidTest.invalidVersionAndCancelledSelectorsPreserveGeometryHistoryAndGpu`: versión 999 y cancelaciones con geometría/historial/visor intactos. | Selectores reales, mensajes y recuperación tras fallos del proveedor. |
| DIS-01 | `SketchPushPullAndroidTest.phoneModeControlsRemainTouchableAndScrollable` y `tabletModeControlsPreserveCanvasSpace` (tamaños simulados); recuperación con una Activity nueva. | Rotación real, teclado, insets del dispositivo y selección. |
| BREP-01 | `Release062ValidationAndroidTest.openFileReplacesTheVisibleGpuSceneAndItsSelection`: fixture 05, agujero y 11000 mm³; pruebas de topología/malla del motor. | Aislar resultado y ver el agujero mediante EGL real. |
| BREP-02 | `ClosedProfileTransactionAndroidTest.toolbarSolidsSubtractRegenerateAfterFaceEditAndReopenWithoutLosingTopology`: 11000 → 12500 mm³, IDs, deshacer/rehacer y reapertura. | Seleccionar y desplazar la cara X=40 con el dedo. |

## Ejecución reproducible

Desde la raíz del repositorio, con JDK 17, Android SDK 35 y Gradle 8.9:

```sh
bash scripts/verify-project.sh
gradle :app:testDebugUnitTest :app:lintDebug --no-daemon --stacktrace
```

El paso «Probar geometría sin Android» de `.github/workflows/android.yml`
ejecuta además las 98 entradas de prueba Java del motor. La clase nueva se
descubre automáticamente por `testDebugUnitTest`; no requiere modificar CI.
El test `fixtureBytesMatchThePublishedManifest` verifica los seis archivos de
ensayo contra sus hashes originales. No se regeneran fixtures para ocultar fallos.

Los resultados XML Android, informes Lint y logs geométricos del **commit exacto**
están en los artefactos del flujo Android asociado a la PR. Una ejecución verde
acredita únicamente estas comprobaciones automatizadas. Las PR #163–#165 siguen
sin fusionar hasta la confirmación física acordada; las ramas y pruebas pueden avanzar.
