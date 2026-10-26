# MyCAD 0.6.0 — Selección robusta de superficies 3D

El visor GPU realiza selección de caras mediante raycasting sobre los triángulos visibles de la escena. El motor de selección `CadRayPicker` normaliza la dirección del rayo antes de intersectar, por lo que la distancia devuelta corresponde a **milímetros del espacio 3D** y no depende de la escala del vector near→far del viewport.

Se descartan triángulos corruptos con vértices no finitos, triángulos degenerados y resultados numéricos no finitos. Un triángulo dañado no bloquea la selección de otros triángulos válidos. Se preserva el índice original de triángulo para vincular el impacto a la cara analítica correcta. Un rayo de longitud cero o no finito sigue rechazándose.

**Regresión:** `CadRayPickerTest` verifica una caja trasladada y girada, distancia física, rayos de escalas muy diferentes, fallos de selección, triángulos dañados, degenerados, malla vacía y argumentos inválidos.

**Alcance:** mejora la fiabilidad del picking de caras sobre mallas GPU; no sustituye el picking analítico directo de aristas ni constituye un kernel topológico general. Sin APK hasta el próximo hito.
