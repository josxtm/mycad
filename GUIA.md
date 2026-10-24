# MyCAD: uso y desarrollo

## v0.2: de la idea al archivo
1. Nuevo: vacía el lienzo después de confirmar. Guarda antes para conservar el trabajo.
2. Rectángulo o Círculo: medidas y espesor en milímetros; Aplicar crea el sólido.
3. Piezas: selección (turquesa). Editar: medidas y posición X. Eliminar borra la pieza.
4. 2D/3D: planta o volumen. Arrastrar gira en 3D; pellizcar cambia zoom.
5. Guardar: abre el selector de Android, escribe un nombre, elige carpeta y confirma Guardar. Formato propio .mycad.json, no STL ni STEP.
6. Abrir: confirma reemplazo y elige el archivo. Solo se reemplaza el diseño después de validar todo el contenido.
El borrador interno es automático y se recupera al iniciar. No es un archivo exportado. El archivo externo no se actualiza automáticamente al editar.

## Prueba de aceptación en Nothing Phone (2a)
- Crear rectángulo 40 × 30 × 10 mm y círculo de diámetro 20, espesor 5.
- Guardar como prueba.mycad.json en Descargas y comprobar que existe.
- Cambiar ancho a 60; abrir el archivo anterior: debe volver a 40.
- Cerrar la app y abrirla: comprobar recuperación del borrador.
- Cancelar el selector de guardar y el de abrir: conservar las piezas.
- Intentar abrir un archivo de texto ajeno: mostrar error y conservar el diseño.
- Crear segundo archivo con otras medidas y alternar entre ambos.
- Probar con pantalla girada y teclado abierto.

## Instalación
v0.2 se instala como MyCAD Pruebas junto a v0.1 para conservar el borrador de la versión anterior. No migra ese borrador automáticamente. Anota las medidas de las piezas antiguas para recrearlas.
Firma debug: esta fase aún no dispone de clave de distribución estable; las siguientes compilaciones pueden requerir reinstalar. Guarda siempre un archivo externo antes.

## Plan propuesto, sin fechas comprometidas
| Fase | Resultado | Criterio de aceptación |
|---|---|---|
| 0.2 | Archivos y ayuda | Guardar, recuperar y cancelar sin pérdida |
| 0.3 | Control de edición | Deshacer/rehacer, tocar para seleccionar, mover X/Y/Z, encuadrar conjunto |
| 0.4 | Bocetos | Líneas y figuras táctiles con cotas y ajuste |
| Motor CAD | Operaciones sólidas | Extrusión de perfiles; unión y corte robustos; estudiar motor geométrico |
| Fabricación | Exportaciones | STL verificado en laminador y PDF con escala/cotas; STEP condicionado al motor CAD |
| Tablet | Interfaz adaptada | Probar con el modelo real disponible en Navidad |

## Alcance
Es un prototipo de introducción numérica de formas, no un equivalente a Shapr3D.
Máximo 500 piezas por documento, archivo de hasta 2 MiB, medidas positivas hasta 10000 mm.
No hay todavía agujeros, exportación CAD, dibujo libre, deshacer ni gestor de proyectos.
La cámara y la selección no forman parte del archivo. Centrar restablece la cámara al origen, no encuadra automáticamente piezas alejadas.
