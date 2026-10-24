# Roadmap de MyCAD

**Principio:** primero comportamiento verificable, después ampliación de herramientas.

| Fase | Alcance | Entregable | Estado |
|---|---|---|---|
| 0.4.8 | Creación de primitivas, movimiento y rotación XYZ | APK experimental | Validado en móvil por el usuario |
| 0.5.0-A | Análisis de referencias y arquitectura | Documentación | En curso |
| 0.5.0-B | Nueva GUI móvil y estados de interacción | Prototipo navegable | Pendiente |
| 0.5.0-C | Bocetos sobre planos XY/XZ/YZ | Editor 2D | Pendiente |
| 0.5.0-D | Detección de perfiles y cotas | Selección de regiones | Pendiente |
| 0.5.0-E | Extrusión táctil y numérica | Sólido desde boceto | Pendiente |
| 0.5.0-F | Persistencia, migración y pruebas | APK candidata 0.5.0 | Pendiente |
| 0.6.x | Booleanas, selección de caras/aristas | Modelado avanzado | Propuesto |
| 0.7.x | Fillets, chamfers, shell, patrones, historial | Herramientas avanzadas | Propuesto |

## Puertas de calidad

- **Diseño:** casos de uso, estados y controles descritos.
- **Implementación:** código separado por responsabilidades.
- **CI:** compilación y pruebas automáticas satisfactorias.
- **Dispositivo:** comprobación manual en Nothing Phone 2a.
- **Entrega:** changelog, APK y limitaciones conocidas.

Si el motor actual no admite una operación fiable, registrar la limitación y decidir sustitución antes de añadir parches.
