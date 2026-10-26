# MyCAD 0.6.0 — Diferenciar tocar geometría y navegar

Durante `ACTION_MOVE`, el visor GPU movía la cámara incluso si el desplazamiento total del dedo estaba por debajo del umbral de 12dp. Esto podía cambiar la orientación ligeramente al intentar seleccionar una cara o una arista B-Rep, haciendo el picking menos preciso.

Ahora `TouchDragThreshold.moved` establece de forma persistente el gesto de órbita únicamente si el desplazamiento desde `ACTION_DOWN` **supera** los 12dp. Mientras el dedo solo oscila ligeramente, la cámara queda inmóvil y la liberación se trata como un toque. Una vez iniciado el giro, el estado sigue siendo giro hasta levantar el dedo; no puede generar una selección accidental.

Se comprueba también el desplazamiento en `ACTION_UP`, porque algunos controladores pueden reportar una posición final diferente sin un último evento `MOVE`. Si supera el umbral, no se hace picking. No se modifica la lógica de multitáctil, los manipuladores de cara/arista ni la escena guardada.

Pruebas: `TouchDragThresholdTest` cubre toques, movimiento diagonal, límite exacto, distintas densidades, continuidad del arrastre y entradas inválidas. El guion CI verifica la integración Android. Sin APK hasta el siguiente hito funcional; calibración en Nothing Phone 2a pendiente.
