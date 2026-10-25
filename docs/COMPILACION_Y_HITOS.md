# MyCAD — verificación continua sin APK y entregas por hitos

## Política de desarrollo (25 octubre 2026)

Los commits y solicitudes de cambio normales ejecutan **integridad + geometría Java + tests Android + Lint**. El comando habitual de Gradle es `testDebugUnitTest lintDebug`: **NO invoca `assembleDebug` y NO publica una APK**. Se conservan los informes, los fallos de CI y el archivo de estado en `ci-status`. El estado mostrará «Generación APK: skipped» en una verificación sin entrega.

## Cuándo generar una APK

Solo tras un **hito funcional importante** que merezca probarse en el Nothing Phone 2a: por ejemplo un editor de bocetos sustancialmente nuevo, un motor de operaciones sólidas más amplio, o una revisión de estabilidad que reúna varias fases.

Se puede generar de dos formas:

1. **Integración explícita de hito:** título del PR de lanzamiento comienza exactamente por `release: MyCAD ` (por ejemplo, `release: MyCAD 0.7.0`). La condición comprueba únicamente el **inicio** del mensaje del commit y no textos en el cuerpo o comentarios. Así, mencionar una APK en una PR ordinaria no provoca un empaquetado accidental. No utilizar el prefijo en PRs corrientes.
2. **Ejecución manual sin cambiar código:** GitHub → Actions → «MyCAD · Verificación continua y APK por hitos» → «Run workflow» → `main` → marcar `generar_apk = true`.

El flujo vuelve a ejecutar pruebas, Gradle, Lint y `assembleDebug`, comprueba el nombre/archivo y publica el artefacto `myCad<version>`. Si la verificación falla, no publica el artefacto de APK.

## Reglas de comunicación

- «Bloque integrado, CI correcta» **NO** implica que exista una APK nueva.
- El asistente recomendará cuándo una funcionalidad justifica un hito y lo comunicará expresamente; no pedirá probar cada pequeña iteración.
- Los cambios de versión de la aplicación deberían reservarse para hitos, junto con pruebas manuales de funciones y compatibilidad de archivos.
- Mantener tests automáticos habituales para descubrir fallos pronto; omitir totalmente la compilación de código Android perdería esa protección.

## Limitaciones

Aunque no se invoque `assembleDebug`, Gradle puede compilar código y recursos necesarios para tests o Lint y tardar un tiempo. La mejora es eliminar el ensamblado/publicación de APK redundantes, no evitar todas las tareas de Android. La compilación CI tampoco verifica gestos táctiles ni rendimiento real.

Nota correctiva: la ejecución #315 generó una APK no deseada porque el texto `[apk]` aparecía dentro del mensaje del squash merge. Este requisito de prefijo estricto evita ese falso positivo.
