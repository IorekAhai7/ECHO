# Scoring ctc-relative-v1

Un **82** significa únicamente 82 en un índice determinista de coincidencia con este modelo y esta pronunciación objetivo. No significa 82% correcto, 82% de probabilidad ni dominio del inglés. No es GOP calibrado.

Para cada fonema objetivo `p`:

- CTC Viterbi selecciona los frames asignados a `p`.
- En cada frame se calcula `exp(logit(p) − max(logit(q)))`, donde `q` recorre fonemas no especiales. El denominador de softmax se cancela en este cociente de posteriores. Se toma el máximo de los frames asignados (evidencia puntual, no media temporal).
- `match` vale 1 si la alineación de edición de la secuencia greedy coincide con `p`, 0 si lo sustituye.
- `phoneScore = round(100 × (0.60 × relativeEvidence + 0.40 × match))`.
- Si la alineación greedy indica omisión, el score de ese fonema es 0 aunque la ruta forzada encuentre un segmento.
- Score de palabra: media de scores de sus fonemas, redondeada.
- Score total: media de todos los fonemas × `N / (N + adiciones)`, redondeada. `N` es el número de fonemas objetivo.

Los pesos 0.60/0.40 son una **heurística inicial**, no aprendida ni validada. El máximo por frame puede ser demasiado optimista; greedy puede penalizar acentos válidos; forzar una ruta produce atribuciones erróneas. Una sustitución puede recibir hasta 60 y una omisión detectada recibe 0. No cambiar la fórmula para conseguir que todos los audios sintéticos puntúen 100.

Los controles de señal se aplican ANTES de normalizar: silencio, bajo volumen, saturación, audio corto/largo, no finito y ruido de alta tasa de cruces se rechazan sin score. Una salida con menos de `max(1, N/5)` fonemas se rechaza por evidencia insuficiente. No existe un umbral pedagógico calibrado para «bien» o «mal».

Los detalles guardan versión, sonidos observados, segmentos, latencia y adiciones. Comparación inmediata solo entre el mismo ejercicio y versión. Progreso semanal muestra tiempo grabado de intentos válidos (no tiempo total de estudio); no mezcla scores de distintas frases en una media engañosa.

Ver `EngineTest` para exactitud del algoritmo y `OfflineIntegrationTest` para pesos reales. La matriz con humanos y las pruebas de correlación aún no existen.
