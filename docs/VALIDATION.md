# Validación y rendimiento de la primera sesión

## Estado

**Alpha técnica, no motor pedagógico validado.** No hay mocks en el runtime. Los tests unitarios sí usan logits artificiales para verificar algoritmos; los tests Android usan pesos ONNX y WAVs reales generados con TTS. La evidencia sintética no permite distinguir con confianza pronunciación humana correcta, cercana e incorrecta.

## Comprobaciones ejecutadas

- Build Android debug ARM64, análisis estático y tests JVM.
- Tests JVM: silencio, duración corta, volumen bajo, saturación, NaN, ruido blanco, normalización, WAV roundtrip, vocabulario y audio de los 24 ejercicios, omisión, sustitución, adición, alineamiento de fonemas repetidos, ruta imposible, evidencia ambigua y determinismo.
- Integración en emulador Pixel 3a / API 34 / ARM64: motor real sin INTERNET, modo avión y Wi-Fi apagado. Referencia «I like this ship.» → **100**, audio «sheep» contra esa frase → **18**. Repetir el mismo WAV produce el mismo score. Se verificó guardado, reapertura de SQLite, eliminación individual del audio y borrado del historial aislado.
- Revisión visual de bienvenida, home y práctica; navegación por UI y permiso de micrófono solicitado en contexto. El emulador se inició sin audio del host; no se afirma haber probado calidad de entrada/salida física.
- APK sin INTERNET, backups desactivados, reglas de extracción excluyen datos de nube y transferencia.

## Medidas observadas (no garantías)

| Medida | Observación |
| --- | --- |
| Mac Apple M1, Python ORT CPU 4 hilos | inferencia de las 24 referencias: 0.084–0.145 s por clip de 1.175–2.15 s |
| Carga modelo en Mac | 1.24 s |
| Pico RSS del proceso de spike en Mac | 706,019,328 bytes (~673 MiB) |
| Emulador ARM64, CPU 2 hilos | primer análisis de frase de 1.725 s: 1,878 ms |
| Inicio Activity en emulador | 1.16–1.42 s hasta dibujo; NO incluye garantía de modelo listo |
| Memoria emulador con modelo | dumpsys mostró ~746 MB PSS contabilizado incluyendo ~723 MB swap; sistema con presión de memoria, no medida de pico en teléfono |
| Pesos | 355 MB decimales, INT8; copia adicional local para ORT |

[Resultados crudos del spike](spike-results.json). Esos resultados también muestran las confusiones de vocales; no se seleccionaron solo ejemplos favorables. Las cifras de emulador son observaciones puntuales, no p50/p95 ni rendimiento de teléfono real.

Presupuestos provisionales para validar: pantalla inicial <1.5 s, modelo listo <4 s, análisis de frases de 2–4 s <2 s mediana/<4 s p95, memoria pico <800 MiB y APK ARM64 <400 MiB. No todos están verificados; no hay medida de batería ni prueba larga de 12 s en hardware real. Reducir el coste del modelo es prioridad.

## Matriz pendiente de humanos

Recolectar voluntariamente, con consentimiento y términos compatibles, al menos 20 hablantes hispanohablantes de distintos niveles y un grupo de referencia. Obtener 3 intentos por ejercicio y evaluación ciega por dos evaluadores. Separar hablantes de calibración y test. Incluir variaciones americanas legítimas, omisiones/adiciones anotadas, teléfonos/micrófonos distintos y ruido controlado.

Medir: falsos avisos por fonema, recall de omisiones, correlación con evaluación humana e intervalo de confianza, acuerdo entre evaluadores, estabilidad de repeticiones y efecto de ruido/volumen. No validar usando solamente el mismo TTS que creó los ejercicios. Definir umbrales de abstención según estos datos. Una variante de acento aceptable no debería convertirse en un «error».

## Checklist en un teléfono físico

1. Instalar APK, modo avión, Wi-Fi apagado; completar todo el flujo y repetir.
2. Denegar/permitir micrófono; probar silencio, bajo volumen, ruido, palabras omitidas y frase incompleta.
3. Escuchar referencia y grabación; comprobar que nunca se graban simultáneamente.
4. Recibir llamada/cambiar de app/rotar/bloquear durante grabación y procesamiento.
5. Revisar TalkBack, texto al 200%, temas, modo horizontal y hápticos desactivados.
6. Reabrir app, comparar mismo ejercicio, borrar audio individual y todos los datos.
7. Medir p50/p95, memoria pico, calentamiento y batería con 20 repeticiones.

## Limitaciones explícitas

Sin iOS, modelo grande, UI con coordinación aún centralizada, referencias sintéticas no revisadas por un fonetista, scoring no calibrado, variantes de acento incompletas, VAD/ruido simplificados, sin prosodia ni transcripción ortográfica. No hay implementación simulada presentada como terminada. La grabación humana en teléfono y la validez pedagógica siguen abiertas; por tanto el criterio de éxito completo del producto todavía no está satisfecho.
