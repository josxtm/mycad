# MyCAD 0.6.0 — Previsualización táctil de caras y aristas

La nueva acción **Proyecto → Previsualizar cara/arista (controles táctiles)** permite probar desplazamientos sin tocar las medidas del cuerpo, el historial ni el JSON hasta **Aplicar**. Elige primero una cara o arista en el visor CAD experimental.

- En caras aparece un deslizador; en aristas aparecen dos, cada uno desplazando una de sus caras adyacentes.
- Cada gesto recalcula desde el cuerpo original mediante `PrimitiveMovePreview`, evitando acumulación de error por aplicar múltiples incrementos.
- Se representa **toda la escena**, sustituyendo temporalmente solo la geometría de la pieza editada. Las mallas importadas, booleanas y demás piezas se conservan.
- `MeshEngine.previewMesh` cambia la malla en el hilo de OpenGL sin recentrar la cámara durante el arrastre.
- Un movimiento inválido deshabilita **Aplicar**. Cancelar o pulsar Atrás restaura la geometría efectiva, sin registrar deshacer ni guardar.
- En **Aplicar**, comprueba UUID/identidad/posición/rotación/dimensiones contra el estado original y vuelve a validar la geometría antes del `checkpoint`. Después persiste una única operación.

El rango automático de los deslizadores depende del tamaño de la pieza y está acotado para evitar saltos enormes. Los cuadros de edición numérica existentes siguen disponibles para medidas exactas.

**Alcance:** solo primitivas independientes del boceto, visor CAD GPU abierto; la interfaz no arrastra todavía el controlador directamente encima del modelo. No hay modificación paramétrica de sólidos booleanos arbitrarios.

Pruebas: `PrimitiveMovePreviewTest` y verificaciones del guion de integración en la CI sin APK. Falta prueba táctil en Nothing Phone 2a.
