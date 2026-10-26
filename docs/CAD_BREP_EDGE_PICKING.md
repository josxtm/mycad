# MyCAD 0.6.0 — Selección de aristas sobre B-Rep analítica

La malla GPU detecta el punto mundial de impacto de un toque (`CadRayPicker`). El nuevo `AnalyticEdgePicker` clasifica ese punto por distancia geométrica a las **12 aristas lineales del prisma** o las **2 circunferencias analíticas del cilindro**, en coordenadas XYZ incluso con rotación. No interpreta líneas de triangulación como bordes editables.

El visor experimental mantiene la selección previa de caras, pero cuando el toque cae suficientemente cerca de una arista, almacena su ID persistente y lo muestra en la banda de información. La opción **Proyecto → Inspeccionar sólido B-Rep (caras y aristas)** identifica con ▶ la arista seleccionada.

## Pruebas y seguridad

`AnalyticEdgePickerTest` cubre cuerpos rotados, distancias y tolerancias, segmentos finitos, cap superior/inferior del cilindro, estabilidad de IDs tras redimensionar, valores NaN o infinitos y ausencia de modificación del punto devuelto. Las pruebas se añaden al script `scripts/verify-project.sh` ejecutado por CI normal, sin generar APK.

## Límites

La tolerancia está en **milímetros de modelo**, no píxeles de pantalla. Con zoom o piezas muy pequeñas puede requerir ajustes táctiles. El resaltado del visor sigue siendo por triángulos; **no hay aún trazado resaltado de arista 3D**. Mallas importadas y booleanas no tienen B-Rep recuperable. La experiencia en Nothing Phone 2a permanece pendiente de validación.
