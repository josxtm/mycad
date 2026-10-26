# MyCAD 0.6.0 — Intercambio DXF de bocetos XY

## Alcance

El editor Android incluye acciones **Importar bocetos DXF (XY)** y
**Exportar bocetos DXF (XY)** en el menú Proyecto. El subconjunto
implementado es ASCII DXF 2D, unidades milímetros (variable DXF
$INSUNITS = 4; se acepta archivo sin unidades explícitas como mm):

- LINE → boceto de línea, no extruible por sí solo.
- CIRCLE → boceto circular, con centro y radio preservados.
- LWPOLYLINE cerrada de cuatro vértices ortogonales → boceto
  rectangular, utilizable para extrusión en el motor actual.

Rechaza importaciones con entidades no soportadas, geometría fuera
del plano XY, contornos no ortogonales y unidades explícitas distintas
de milímetros. Esto evita pérdidas parciales silenciosas.

## Seguridad e integración

- Límite: 2 MB de entrada, 2000 bocetos sumando los existentes.
- La lectura y validación ocurren en hilo secundario.
- Solo después de validar todos los perfiles se añaden con IDs nuevos
  en una única operación deshacible. Un fallo conserva el diseño original.
- Los perfiles XZ/YZ no se incluyen en una exportación XY. Se mantienen
  en el proyecto nativo .mycad.json.
- La exportación contiene los bocetos XY, no cuerpos 3D ni operaciones
  booleanas. No sustituye exportación STEP ni DXF industrial completo.
- CI incluye SketchDxfIOTest con pruebas de ida/vuelta, límites,
  entidades incompatibles, Z distinto de cero y unidades incorrectas.

**Estado:** cambios de código verificados por CI; falta probar importación,
selector de documentos y representación visual en Nothing Phone 2a.
No se publica APK automáticamente por cada incremento.
