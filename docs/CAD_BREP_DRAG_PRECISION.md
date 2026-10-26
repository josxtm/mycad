# MyCAD 0.6.0 — Imán de precisión para arrastre sobre sólido

Se añade **Proyecto → Precisión de arrastre (imán)**. Elige entre Libre, 0,1 mm, 0,5 mm o 1 mm; la preferencia se conserva localmente en el dispositivo.

Tanto el asa de caras como la de aristas leen esa preferencia antes de iniciar la manipulación. `MeshEngine.faceMovement` calcula el delta a partir de la proyección del gesto y lo pasa a `DragPrecision.quantize` **antes** de previsualizar, medir o confirmar el cambio. Así, la pieza mostrada y la posición confirmada usan exactamente la misma distancia, sin saltos al soltar.

El snap es simétrico para movimientos positivos/negativos y nunca supera el límite del desplazamiento admisible. No altera coordenadas del boceto ni geometría guardada hasta que el usuario acepta la edición. Cambiar el imán no modifica la pieza.

CI: `DragPrecisionTest` cubre desplazamiento libre, decimales, medias unidades, simetría, topes, valores no finitos y validación de datos. Verificación de integración con Android sin generar APK.

**Pendiente:** prueba de sensibilidad y estabilidad de gesto en Nothing Phone 2a; el imán es para el desplazamiento del manipulador, no para operaciones globales de boceto ni para booleanas generales.
