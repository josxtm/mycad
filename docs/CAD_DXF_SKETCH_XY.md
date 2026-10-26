# MyCAD 0.6.0 — Intercambio DXF de bocetos XY

## Alcance

El editor Android incluye acciones **Importar bocetos DXF (XY)** y
**Exportar bocetos DXF (XY)** en el menú Proyecto. El subconjunto
implementado es ASCII DXF 2D, unidades milímetros (variable DXF
$INSUNITS = 4; los archivos sin unidad declarada se rechazan):

- LINE → boceto de línea, no extruible por sí solo.
- CIRCLE → boceto circular, con centro y radio preservados.
- LWPOLYLINE cerrada de cuatro vértices ortogonales → boceto
  rectangular, utilizable para extrusión en el motor actual.
- **Otras LWPOLYLINE cerradas de segmentos rectos (3–2000 vértices)** →
  importación de cada arista como boceto de línea conectado. Así se
  importan triángulos y contornos cóncavos (por ejemplo, forma de L),
  que pueden extruirse con la herramienta de contorno cerrado. Los
  segmentos se pueden editar de forma individual.

Rechaza importaciones con entidades no soportadas, geometría fuera
del plano XY, polilíneas abiertas, arcos DXF (bulge), anchuras/grosores
no nulos, vértices degenerados, ausencia o duplicación de $INSUNITS
y unidades distintas de milímetros (incluido el código 0, sin unidad). Los polígonos cerrados no rectangulares se conservan como
líneas; no se convierten erróneamente en rectángulos. Los polígonos
con agujeros, autointersecciones o ramas todavía no pueden extruirse.
Esto evita pérdidas parciales silenciosas.

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
  entidades incompatibles, Z distinto de cero, unidades ausentes,
repetidas o incorrectas; el script de CI comprueba que Android valide
antes de añadir bocetos o registrar deshacer.
- DxfContourExtrusionIntegrationTest comprueba el recorrido real de
  un contorno L cóncavo desde DXF hasta una malla 3D cerrada con
  volumen positivo.

**Estado:** cambios de código verificados por CI; falta probar importación,
selector de documentos y representación visual en Nothing Phone 2a.
No se publica APK automáticamente por cada incremento.
