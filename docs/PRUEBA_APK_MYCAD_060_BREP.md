# myCAD 0.6.0 · Hito de prueba: Booleanas B-Rep con agujeros

Estado: **vista previa técnica**, no versión estable. Fecha: 27 de octubre de 2026.

## Alcance de este instalador

- Extrusión de contornos de boceto XY/XZ/YZ, incluyendo perfiles cóncavos.
- Selección de caras de contornos B-Rep; ajuste de altura y desplazamiento lateral, con validación previa.
- Intersección convexa, unión/resta de rectángulos, operaciones entre perfiles ortogonales cóncavos.
- Resultados de varios cuerpos separados por operaciones booleanas.
- **NUEVO:** unión, resta e intersección ortogonales con agujeros *pasantes*, con representación analítica de caras planas, aristas gemelas y semiaristas; la malla GPU se reconstruye sin abrir el sólido.
- Historial de recetas por UUID, guardado JSON y deshacer/rehacer; cambios compatibles de los bocetos originales regeneran los resultados.

## Protocolo breve de aceptación en Nothing Phone 2a

1. Abrir myCAD y dibujar un rectángulo exterior de 60 × 60 mm sobre XY.
2. Crear otro rectángulo de 20 × 20 mm dentro del primero y extruir los dos con altura idéntica, por ejemplo 30 mm.
3. Seleccionar el sólido exterior y elegir **Booleanas B-Rep con agujeros pasantes → Resta con agujeros**; seleccionar el sólido interior como cortador.
4. Verificar que aparece una placa/cuadro con hueco central *atravesando toda la altura*. Debe poder girarse con navegación 3D. Los dos sólidos originales se conservan.
5. Guardar, cerrar y volver a abrir myCAD. Verificar que la malla con agujero continúa presente.
6. Probar deshacer y rehacer; volver a abrir el proyecto.
7. Cambiar las medidas del rectángulo interior de forma compatible y verificar la regeneración del agujero.
8. Repetir en XZ y YZ, y con extrusiones firmadas negativas. Comprobar los contornos de las paredes internas y la ausencia de caras ausentes.
9. Exportar el resultado como STL u OBJ y revisarlo en un visor externo, comprobando que es un modelo cerrado.

## Límites de esta versión

- Solo operaciones ortogonales, desde el **mismo plano y origen** y con igual altura firmada. Los perfiles inclinados, curvas/NURBS y agujeros ciegos **no** están implementados por este motor.
- Máximo de 16 componentes y 16 huecos pasantes en el nuevo núcleo.
- La malla resultante tiene historial enlazado a sus fuentes, pero **no** admite selección paramétrica de caras interiores derivadas.
- El proyecto compila y pasa pruebas automáticas. La **ejecución real en Nothing Phone 2a debe confirmarla el usuario**; las pruebas de CI no equivalen a esa validación física.

## APK

El flujo de GitHub **«MyCAD · Verificación continua y APK por hitos»** genera el APK únicamente al publicar un commit de hito cuyo mensaje empiece exactamente por `release: MyCAD ` o mediante el botón manual con `generar_apk=true`.

Si supera las verificaciones y el ensamblado, GitHub Actions guarda el instalador como *artifact* de la ejecución. La aplicación usa el identificador de vista previa `es.josxtm.mycad.preview`, con nombre visible myCAD y `versionName=0.6.0`. No se publica en tiendas.
