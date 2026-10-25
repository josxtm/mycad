# Informes automáticos de compilación — MyCAD

Cada ejecución de **APK Android** publica, tanto si termina bien como si falla, el artefacto `informe-mycad-N` durante 30 días y un resumen en la página del run.

## Dónde consultar
1. Abrir https://github.com/josxtm/mycad/actions
2. Entrar en la última ejecución de **APK Android**.
3. Leer el resumen de ejecución (Job summary).
4. Descargar **Artifacts → informe-mycad-N**. Incluye:
   - `INFORME.md`: commit, rama, resultados por fase y errores destacados.
   - `integridad.log`: chequeos básicos.
   - `geometria.log`: compilación y ejecución de pruebas geométricas.
   - `gradle.log`: compilación, tests Android y Lint con detalle.

El informe se publica incluso ante errores ordinarios en la compilación; errores de inicialización del runner o incidencias del propio servicio GitHub pueden impedirlo.

El APK se publica solo cuando los controles anteriores finalizan correctamente. El informe permite revisar las causas del fallo, pero no garantiza que un conector externo pueda enumerar por sí mismo todas las ejecuciones.

## Estados
- `success`: paso completado.
- `failure`: paso fallido.
- `skipped`: paso omitido porque falló una dependencia previa.
- `unknown`: no disponible.

El informe de ejecución es evidencia de las pruebas automáticas, no de validación táctil en teléfono real.
