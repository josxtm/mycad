# MyCAD — Histórico de cambios y compilaciones
Registro iniciado: 2026-10-24. Separar **código subido**, **compilación exitosa** y **validación en móvil**.

| Versión / compilación | Cambios principales | Estado |
| --- | --- | --- |
| v0.3 · Actions #7 | Creación de piezas, selección y movimiento básicos, XYZ, deshacer/rehacer, archivos JSON | Compiló; usuario confirmó funcionamiento |
| v0.4.1 · Actions #8 | Barras laterales compactas, menú e iconos provisionales | Compiló; usuario confirmó funcionamiento |
| v0.4.2 · Actions #9 | Bocetos táctiles de línea, rectángulo y círculo, almacenamiento JSON | Compiló; pruebas de usuario detectaron limitaciones de selección |
| v0.4.2 · Actions #10 | Corregido nombre del artefacto de GitHub a MyCAD-v0.4.2-APK | Cambio subido; resultado de ejecución no verificado aquí |
| v0.4.2 · Actions #11 | Código para seleccionar bocetos en vistas 2D y 3D y resaltarlos | Cambio subido; resultado de ejecución y prueba móvil pendientes |
| Documentación · 2026-10-24 | Creación de DIRECTRIZ_GESTOS.md y este histórico | Documentación; no supone nueva versión funcional |

## Pendientes reportados (NO implementados ni validados)
- Selección persistente y controladores visibles con un toque para toda entidad.
- Dos toques para activar mover; tres toques para edición precisa.
- Extremos de línea, cuatro esquinas y centro de rectángulo, centro y radio de círculo: arrastre continuo y fluido.
- Cotas dinámicas durante la edición.
- Un toque simultáneo con tres dedos para restaurar cámara inicial 3D.
- Revisión de conflictos entre gestos y conservación de selección entre vistas.
- Sincronizar en futuras versiones nombre visible, versionName/versionCode, APK y nombre de artefacto GitHub.

## Norma de publicación
Cada compilación que represente una versión funcional debe documentar versión, fecha, cambios, correcciones, pendientes, commit, URL de ejecución Actions, resultado y estado de pruebas en el móvil. Si la ejecución aún no ha terminado, indicarlo como pendiente; actualizar tras comprobar. No iniciar cambios de código sin autorización tras recopilar fallos.

## v0.4.2 — revisión ampliada de gestos y visualización (2026-10-24)
**Aprobación:** usuario autorizó integrar todos los requisitos recogidos.
**Commits:** `d636942` (controles y ajustes), `b3812c7` (versionName 0.4.2), `1dabba4` (órbita centrada y restauración de cámara).
**Código incorporado para probar:** selección y puntos de control de bocetos; arrastre de puntos; doble toque para mover, triple toque para editar coordenadas; tres dedos para restablecer cámara; ajustes de tema y sensibilidad; modo película con pieza o conjunto, velocidad, repetición y pausa.
**Compilación y prueba Android:** pendientes de verificación final. **No declarar completado ni validado** hasta comprobar Actions y las pruebas en Nothing Phone.
**Limitaciones conocidas para revisar:** detección de gestos múltiples, sensibilidad real, coherencia del tema en todas las barras, controladores de sólidos 3D, guardado/abrir de bocetos y funcionamiento del modo película.

## v0.4.2 — segunda revisión de errores (2026-10-24)
Commit: `3a2e9e9`. GitHub Actions: https://github.com/josxtm/mycad/actions/runs/40128738815
- Decodificación de archivos sin mutar el diseño activo antes de confirmar; recuperación de bocetos en abrir, carga y deshacer/rehacer.
- Selección de pieza en el menú de película y separación de modos pieza/conjunto.
- Ajustes de apariencia extendidos a barra superior y herramientas; arrastre de controladores con sensibilidad.
- Controladores visuales en las piezas 3D y revisión de la secuencia de toques.
- Estado de compilación y validación móvil: pendiente de verificar. Los controladores 3D aún requieren comprobar su edición interactiva real.
