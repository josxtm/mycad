# MyCAD — Directriz de gestos
Versión de documento: 1.0 · 2026-10-24 · Estado: especificación aprobada, implementación parcial

## Principios
- Interacción táctil fluida, respuesta continua y puntos de control grandes para móvil.
- Un gesto de cámara nunca altera geometría. Cada operación de edición se puede deshacer/rehacer.
- La selección persiste al levantar el dedo y al alternar vistas 2D/3D; tocar vacío deselecciona.
- No confundir requisitos con funcionalidades ya implementadas: validar cada gesto en dispositivo.

## Selección de entidades
| Gesto | Resultado previsto |
| --- | --- |
| Un toque sobre línea, rectángulo, círculo o sólido | Seleccionar, resaltar y mostrar inmediatamente todos los puntos de referencia |
| Dos toques sobre la misma entidad | Activar modo mover; arrastre posterior desplaza la entidad completa sin deformarla |
| Tres toques sobre la misma entidad | Abrir edición geométrica y numérica en mm |
| Arrastrar un punto de referencia | Editar la geometría en tiempo real, sin saltos |
| Tocar vacío | Deseleccionar y ocultar controladores |

La detección de toque simple/doble/triple debe evitar acciones prematuras y conflictos con el arrastre. Seleccionar el contorno de un rectángulo selecciona la entidad completa, no cuatro líneas independientes.

## Controladores por geometría
- **Línea:** extremos inicial/final arrastrables de manera independiente.
- **Rectángulo:** cuatro vértices para cambiar tamaño y centro para mover la entidad.
- **Círculo:** centro geométrico para desplazar y punto de radio sobre circunferencia para redimensionar.
- **Sólidos:** selección visible y controles apropiados a la operación; concretar antes de implementar.
- Mostrar cotas dinámicas en milímetros al editar.

## Cámara
| Gesto | Resultado previsto |
| --- | --- |
| Arrastrar un dedo sobre vacío en 3D | Orbitar cámara |
| Pellizcar con dos dedos | Zoom |
| Un toque simultáneo con tres dedos | Cambiar a 3D y restablecer orientación isométrica, zoom y centrado iniciales |
| Botón 2D/3D | Alternar vista conservando selección |

El gesto de tres dedos no crea bocetos, no modifica piezas y no cancela el proyecto.

## Verificación obligatoria por versión
1. Probar línea, rectángulo, círculo y piezas 3D cuando corresponda.
2. Probar selección persistente, controladores, arrastre y cambios de vista.
3. Probar que zoom y gesto de tres dedos no dibujan ni deforman.
4. Probar deshacer/rehacer, guardar/abrir y compatibilidad de archivos anteriores.
5. Registrar en HISTORIAL_CAMBIOS.md qué se implementó, qué queda pendiente, commit, ejecución de GitHub Actions y resultado de prueba en Android.

## Proceso de cambios
Antes de modificar código o lanzar una nueva versión: recopilar incidencias, presentar la lista, preguntar «¿Hay algo más que depurar?» y esperar autorización explícita. Actualizar esta directriz y el historial cuando cambien gestos. No afirmar que algo funciona en móvil sin prueba del usuario.

## Ajustes de interfaz y modo película (aprobado 2026-10-24)
- Menú Ajustes: tema oscuro/claro, sensibilidad de giro, zoom y arrastre; conservar preferencias.
- Modo película: selector **Pieza seleccionada** o **Conjunto completo**; reproducción/pausa, velocidad, repetición 360°, restablecimiento de vista.
- El giro automático afecta solo a la cámara, nunca a las coordenadas geométricas.
- La cámara debe orbitar alrededor del centro de la pieza elegida o del conjunto completo, encuadrándolo.
- Todos los gestos se prueban en Android; una compilación exitosa no equivale a una validación de uso.
