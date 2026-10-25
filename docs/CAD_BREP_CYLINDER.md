# MyCAD 0.6.0 — B-Rep analítica para cilindros

`CylinderBRep.java` representa un cilindro circular recto orientado en XYZ mediante **tres caras** (lateral, tapa inferior y superior) y **dos aristas circulares periódicas** que comparten las caras adyacentes. Las aristas no se aproximan como segmentos en el modelo: los triángulos se generan exclusivamente para mostrar/exportar y no alteran los IDs.

Los identificadores son compatibles con `PrimitiveTopologyMap` (`/face/CYLINDER_SIDE`, `/face/CYLINDER_BOTTOM`, `/face/CYLINDER_TOP`) y son independientes de la resolución de triangulación.

Se exponen áreas, volumen, centro y evaluación paramétrica de los círculos, así como normales geométricas de caras bajo cualquier rotación XYZ. Una operación pura `offsetFace` desplaza la lateral (cambia radio), la tapa superior (cambia altura) o la inferior (cambia la base preservando la posición de la tapa contraria). No muta el sólido previo.

Pruebas `CylinderBRepTest`: cierre periódico, adyacencias, invariancia ante triangulación, normales rotadas, área/volumen, conservación de la cara opuesta, IDs estables, validaciones contra sólidos degenerados y aislamiento de mallas. Incorporadas a `scripts/verify-project.sh`; **no generan APK**.

**Limitación:** esta es una representación analítica restringida a cilindros primitivos y no constituye un núcleo B-Rep general; no admite trimado, unión B-Rep, chaflanes ni redondeos. La edición táctil Android y la persistencia de sus comandos se integran como fase separada.
