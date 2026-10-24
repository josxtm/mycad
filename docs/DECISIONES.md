# Registro de decisiones (ADR ligero)

## D-001 — Documentación versionada

**Fecha:** 2026-10-25  
**Decisión:** mantener documentación de fases, arquitectura, pruebas y análisis de referencias en `docs/` dentro de GitHub.  
**Motivo:** trazabilidad y evitar cambios improvisados.

## D-002 — Mantener 0.4.8 como referencia funcional

**Fecha:** 2026-10-25  
**Decisión:** no sustituir indiscriminadamente el código existente antes de definir arquitectura y pruebas.  
**Motivo:** movimiento y rotación XYZ ya validados en móvil.

## D-003 — El boceto será origen del sólido

**Fecha:** 2026-10-25  
**Decisión:** priorizar boceto → perfil → extrusión en 0.5.0.  
**Motivo:** acercarse a un flujo CAD editable, no solo a primitivas por formulario.

## D-004 — Separar render y geometría

**Fecha:** 2026-10-25  
**Decisión propuesta:** aislar visor y kernel geométrico tras interfaces.  
**Estado:** pendiente de selección de tecnología.

## D-005 — Inspiración funcional, implementación propia

**Fecha:** 2026-10-25  
**Decisión:** estudiar la experiencia de Shapr3D, pero diseñar recursos, código e identidad propios de MyCAD.
