# Diagnóstico de componentes sin volumen — MyCAD 0.6.0

El validador `TriangleMeshHealth.inspect` identifica componentes de triángulos conectados por aristas. Una superficie de dos caras coplanares con orientaciones opuestas puede satisfacer las comprobaciones de aristas (dos usos por arista, orientaciones opuestas) y aun así no delimitar volumen 3D.

Se añaden `Report.shells` y `Report.zeroVolumeShells`. El método `isWatertight()` devuelve falso cuando una componente tiene volumen orientado **exactamente cero**, incluso si otra componente tiene volumen no nulo. La suma `signedVolume` se conserva con signo, usando origen local por componente y compensación numérica.

Pruebas: dos triángulos coplanares enfrentados, mezcla de tetraedro válido y lámina colapsada, y tres tetraedros separados (caso válido). Los sólidos con componentes cerradas de orientación inversa siguen admitidos: su suma de volumen puede anularse sin que ninguna componente individual sea nula.

**Límite deliberado:** esto no demuestra ausencia de autointersecciones ni rechaza todos los volúmenes físicamente degenerados; no se aplica umbral relativo arbitrario para evitar falsos rechazos de sólidos delgados. La verificación Java/Android de CI no equivale a prueba táctil en dispositivo. No se genera APK en este incremento.
