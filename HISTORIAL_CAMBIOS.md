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
