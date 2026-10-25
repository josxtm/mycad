# Contribuir a MyCAD (motor Android)

## Flujo obligatorio de trabajo a partir de 0.6.0

1. Actualizarse con `main` y crear una rama temática: `feature/<objetivo>`, `fix/<problema>` o `maintenance/<tarea>`.
2. Agrupar cambios relacionados en commits descriptivos y atómicos. No modificar simultáneamente piezas no relacionadas del motor.
3. Incluir pruebas unitarias Java/Android y documentación cuando cambie geometría, persistencia o comportamiento.
4. Abrir una **Pull Request** hacia `main` usando la plantilla de `.github/PULL_REQUEST_TEMPLATE.md`.
5. **No fusionar** sin revisar el diff, verificar que el job de Actions **verify** está en **success** para el último commit de la PR y asegurarse de que no introduce regresiones de formatos, selección ni deshacer.
6. Fusionar mediante **Squash and merge**, salvo que sea imprescindible mantener varios commits independientes. Evitar pushes directos a `main`.
7. Comprobar el workflow generado por el merge y registrar resultados. Las ramas de trabajo fusionadas pueden eliminarse.

### CI y creación de APK

- En PR y commits ordinarios, `.github/workflows/android.yml` ejecuta `scripts/verify-project.sh`, pruebas geométricas Java, `:app:testDebugUnitTest` y `:app:lintDebug`.
- **No generar APK automáticamente** durante desarrollo ordinario.
- Un hito necesita **ejecución manual del workflow** con `generar_apk=true` o un commit aprobado de tipo `release: MyCAD <versión> ...`, siempre tras CI correcta. Esto genera APK debug, no una versión final certificada.
- Probar los hitos en **Nothing Phone 2a** y registrar los resultados antes de afirmar compatibilidad real.

## Protección efectiva de la rama main: configuración de GitHub

La plantilla y esta política **no impiden técnicamente** que alguien con permisos escriba directamente en `main`. Debe habilitarse una **ruleset** o regla de protección en GitHub:

1. Ir a **Settings → Rules → Rulesets → New branch ruleset** (o **Settings → Branches → Add branch protection rule**).
2. Apuntar a la rama `main`.
3. Activar **Require a pull request before merging**.
4. Activar **Require status checks to pass before merging**, seleccionando el job **verify** del workflow «MyCAD · Verificación continua y APK por hitos». GitHub suele ofrecer el nombre de la comprobación tras haberse ejecutado una PR.
5. Activar **Require branches to be up to date before merging** si se desea verificar siempre el último estado de `main`.
6. Activar la aplicación a administradores si la interfaz y el plan lo permiten. No exigir una revisión externa si solo existe un mantenedor disponible; valorar una revisión obligatoria cuando haya otros revisores.

**Estado:** el flujo por PR está definido y probado cuando una PR aparece con CI correcta; la protección de GitHub es un ajuste de repositorio que debe confirmarse por separado. No declarar `main` protegida antes de verificar la ruleset.

## Limpieza de ramas

Antes de borrar una rama, comparar `main...<rama>` y asegurarse de que no hay trabajo útil exclusivo. Las ramas de experimentación divergentes pueden conservar commits preliminares (incluso defectuosos) que hayan sido sustituidos por commits validados en `main`: registrar este hecho y borrar la rama desde **Code → Branches**. Nunca borrar `main` ni reescribir su historial para limpiar una rama experimental.
