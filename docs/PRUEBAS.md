# Plan de pruebas y aceptación

## Matriz de estado

| Caso | Versión | Estado |
|---|---|---|
| Crear prisma desde formulario | 0.4.8 | Validado en móvil |
| Mover por X/Y/Z | 0.4.8 | Validado en móvil |
| Girar con aros X/Y/Z | 0.4.8 | Validado en móvil |
| Ángulos numéricos XYZ | 0.4.8 | Validado en móvil |
| Importar STL/OBJ de prueba | 0.4.8 | Pendiente de confirmación |
| Dibujar boceto en plano elegido | 0.5.0 | Pendiente de implementar |
| Seleccionar perfil cerrado | 0.5.0 | Pendiente de implementar |
| Extruir con previsualización | 0.5.0 | Pendiente de implementar |
| Cancelar extrusión sin cambios | 0.5.0 | Pendiente de implementar |
| Guardar/reabrir features y cotas | 0.5.0 | Pendiente de implementar |
| Migrar JSON v1 | 0.5.0 | Pendiente de implementar |

## Prueba real del usuario — MyCAD 0.5.0 (Nothing Phone 2a)

**Origen:** informe directo del usuario, 2026-10-25. **Resultado:** 6 correctas, 0 fallidas, 2 sin probar.

| Nº | Prueba | Resultado |
|---|---|---|
| 1 | Inicio de APK | FUNCIONA |
| 2 | Selector de planos | FUNCIONA |
| 3 | Dibujar boceto | FUNCIONA |
| 4 | Acotar | FUNCIONA |
| 5 | Extruir | SIN PROBAR |
| 6 | Navegar | FUNCIONA |
| 7 | Guardar y abrir | FUNCIONA |
| 8 | Otros planos XZ/YZ | SIN PROBAR |

**Alcance de las confirmaciones:** el usuario no indicó que la prueba de guardado incluyera extrusiones, por lo que no se certifica persistencia de features. El selector de planos funciona, pero no se ha confirmado dibujar/seleccionar en XZ/YZ.

### Revisión preventiva posterior

- Hallazgo en código: `hitSketch` y `drawHandles` empleaban proyección fija XY aunque el boceto pudiera estar en XZ/YZ. Esto podía impedir seleccionar o editar entidades en esos planos.
- Corrección: proyección mediante `sketchPoint` en selección y controladores, commit `f5e061f`.
- Ajuste de texto: menú de bocetos identifica el plano seleccionado en lugar de mostrar siempre XY.
- **Estado:** corrección implementada, pendiente de resultado de CI y validación real en móvil. No procede afirmar que el usuario haya encontrado este fallo.

### Pruebas siguientes prioritarias

1. En XY, rectángulo 60 × 30 mm → Extruir seleccionado → 20 mm; verificar sólido y dimensiones.
2. Guardar, cerrar y reabrir ese diseño extruido; verificar que sigue presente.
3. En XZ y YZ, dibujar, salir de herramienta, seleccionar y acotar un rectángulo; después intentar extruir.
4. Cancelar diálogo de extrusión; verificar que no crea ningún cuerpo.
5. Si falla, aportar captura, paso y resultado esperado/real.

## Escenario de prueba 0.5.0

1. Instalar APK en Nothing Phone 2a.
2. Crear documento y elegir XY.
3. Dibujar rectángulo; introducir 60 y 30 mm.
4. Seleccionar región y extruir 20 mm.
5. Orbitar, hacer zoom y pan; verificar dimensiones intactas.
6. Guardar proyecto, cerrar aplicación y reabrir.
7. Exportar STL y OBJ; comprobar dimensiones con visor independiente.
8. Repetir extrusión cancelándola y verificar que no aparece nuevo cuerpo.
9. Abrir proyecto JSON v1 anterior.
10. Registrar captura, versión, resultado y error reproducible.

## Registro de incidencias

Cada incidencia deberá contener: versión, dispositivo, pasos, resultado esperado, resultado real, captura/log y commit de corrección.
