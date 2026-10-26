# MyCAD 0.6.0 — Zoom y panorámica multitáctil reales

El visor GPU sustituye el antiguo gesto con dos dedos basado únicamente en movimiento vertical por navegación geométrica: **separar/juntar dos dedos** acerca/aleja proporcionalmente la cámara; **mover ambos dedos juntos** desplaza el centro de la vista por el plano horizontal/vertical de cámara manteniendo la orientación de los cuerpos. Con un dedo se conserva el giro de cámara existente.

`CameraGestureMath` usa la FOV de 45° del renderer para calcular mm por píxel en el foco y las direcciones del sistema XYZ según yaw/pitch. Limita factores extremos de zoom y valida movimientos inválidos. `MeshEngine` registra el par de punteros, su distancia y su centro, evita saltos cuando termina el gesto y no convierte la salida de multitáctil en un toque de selección accidental.

Si comienza multitáctil durante un arrastre de asa de cara/arista, ese arrastre se cancela antes de modificar ninguna pieza, evitando comprometer el proyecto.

Pruebas en `CameraGestureMathTest`: zoom proporcional y límites, escalado con resolución, pan XYZ con yaw 0/90 y pitch 45, reversibilidad, entradas no finitas y altura de pantalla inválida. CI comprueba la integración de los eventos Android y Lint. Sin generar APK.

**Pendiente:** validación de la sensibilidad en Nothing Phone 2a y gestos con más de dos dedos, que todavía no se interpretan como acciones CAD independientes.
