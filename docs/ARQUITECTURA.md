# Arquitectura técnica — evaluación inicial

## Estado actual (0.4.8)

- **MainActivity.java:** concentra UI Android, modelo de Part/Sketch, eventos táctiles, proyección Canvas y JSON. Es un acoplamiento alto que dificulta pruebas y ampliación.
- **MeshEngine.java:** visor OpenGL ES para geometría mallada; no es un kernel B-Rep.
- **MeshIO.java:** importación/exportación STL/OBJ de mallas.
- **GitHub Actions:** compilación Android en la nube y publicación de artefactos APK.

## Arquitectura objetivo propuesta

```text
app/
  ui/                 Pantallas, herramientas contextuales y controles
  input/              Gestos, selección y máquina de estados
  camera/             Cámara y proyección
  document/           Modelo versionado y persistencia
  sketch/             Entidades 2D, cotas, restricciones y perfiles
  features/           Extrusión y operaciones de modelado
  geometry/           Geometría, mallas y futuras booleanas
  render/             Visualización y resaltado de selección
  io/                 JSON, STL, OBJ y futuros formatos
  history/            Deshacer/rehacer y dependencias
```

Esta estructura es una **propuesta**, no refleja todavía paquetes creados.

## Decisiones abiertas

1. **Kernel geométrico:** evaluar biblioteca viable en Android, licencia, tamaño de APK y soporte de operaciones; no asumir que OpenGL es un motor CAD.
2. **Render:** mantener temporalmente OpenGL y aislarlo tras interfaces para poder cambiar implementación.
3. **Sketch solver:** comenzar con restricciones mínimas comprobables y ampliar según necesidades.
4. **Identidad estable:** entidades y features necesitan IDs persistentes para referencias y edición.
5. **Migración:** importar JSON v1 a nuevo esquema sin destruir diseños previos.
6. **Pruebas:** separar validación geométrica de pruebas táctiles y visuales.

## Estrategia de migración

- Conservar un punto de restauración de 0.4.8.
- Implementar componentes nuevos de forma incremental, sin eliminar el APK estable.
- Añadir pruebas de regresión para JSON v1, movimientos XYZ, rotaciones y STL/OBJ.
- Solo retirar código antiguo cuando el reemplazo esté compilado y validado.
