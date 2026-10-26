# MyCAD 0.6.0 — Tres dedos para recuperar vista inicial 3D

Se incorpora el gesto solicitado: un toque rápido con tres dedos sobre el visor OpenGL recupera la cámara inicial, con yaw 35°, pitch 25°, y encuadra nuevamente las piezas visibles. No altera geometría, cotas ni archivos.

El tercer dedo debe llegar dentro de 250 ms desde el primer dedo y sin que los dos dedos iniciales hayan iniciado un gesto de movimiento. Al levantar el primer dedo, el tiempo total del gesto no debe superar 500 ms ni debe haber desplazamientos individuales mayores a 12dp. De lo contrario no se cambia la cámara.

Con tres dedos se bloquean tanto el pellizco como el picking y la órbita. Si hay un manipulador de cara o arista activo, se cancela su previsualización sin guardar. Después de levantar el primer dedo, los dedos restantes no generan movimientos ni selecciones hasta terminar el contacto.

`ThreeFingerHomePolicyTest` verifica tiempos, distancias, densidad de pantalla y rechazos; las reglas de integración y Gradle/Lint de Android corren en CI sin APK.

Pendiente validación táctil en Nothing Phone 2a y ajuste de sensibilidad según experiencia real.
