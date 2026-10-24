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
