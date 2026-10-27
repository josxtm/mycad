# Motor CAD — siguiente hito B-Rep general

## Estado inicial comprobado
`OrthogonalPolygonBRepBoolean` solo admite perfiles ortogonales en el mismo plano con altura firmada idéntica. Rechaza resultados con huecos o regiones desconectadas cuando usa `PolygonPrismBRep`. El proyecto dispone de otras rutas limitadas para agujeros y múltiples componentes, pero no de un motor booleano B-Rep general.

## Objetivo
Extender el núcleo geométrico **sin sustituir silenciosamente B-Rep por mallas trianguladas**.

## Orden de implementación
1. Definir casos de prueba reproducibles: perfiles convexos no ortogonales, alturas distintas, contacto tangencial, intersección vacía, agujero y componentes desconectadas.
2. Separar intersección de curvas/segmentos, clasificación interior/exterior y reconstrucción topológica de caras, bucles y semiaristas.
3. Añadir una representación de sólido capaz de contener múltiples bucles por cara y múltiples shells, con invariantes de orientación y manifold.
4. Implementar primero intersección planar de perfiles simples convexos no ortogonales con altura común; rechazar explícitamente los casos no soportados.
5. Ampliar a perfiles cóncavos y agujeros; luego alturas y planos diferentes.
6. Mantener compatibilidad de JSON, undo/redo, STL/OBJ y selección por ID; migraciones explícitas si cambia el formato.
7. Ejecutar pruebas Java puras, validación topológica y CI Android antes de abrir APK de prueba.

## Invariantes
- No aceptar sólidos con aristas sin pareja o semiaristas twin inconsistentes.
- No introducir volumen negativo o cero por errores de orientación.
- No alterar operandos cuando falla una booleana.
- No afirmar compatibilidad con STEP/NURBS hasta que exista implementación y pruebas.
- No fusionar cambios con CI rojo.

## Validación Android pendiente
Probar en Nothing Phone 2a y tablet de 12 pulgadas cuando esté disponible.
