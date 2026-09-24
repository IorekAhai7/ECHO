# Motor de pronunciación

## Pipeline real

1. PCM mono 16 kHz, 0.25–12 s; validación de finitud, RMS, saturación y una criba simple de ruido por cruces por cero.
2. Normalización de media/varianza de toda la grabación.
3. ONNX CPU ejecuta wav2vec2 y produce logits por frame y fonema. No genera texto.
4. CTC greedy sin texto objetivo genera sonidos observados, colapsando repeticiones y blanks.
5. Alineamiento global de edición identifica posibles sustituciones, omisiones y adiciones de fonemas.
6. Viterbi CTC alinea la secuencia objetivo con frames, incluyendo blanks y restricciones de fonemas repetidos.
7. Se compara evidencia acústica relativa y coincidencia de la secuencia; agregación por palabra y total.

El alineamiento es forzado: por sí solo puede asignar segmentos aunque la frase sea incorrecta. Por eso existe la comparación independiente con los fonemas greedy. Aun así, una omisión puede confundirse con sustitución y una palabra añadida se representa como sonidos extra, **no** como transcripción fiable de esa palabra. Las fronteras en milisegundos son aproximadas (reparto temporal por frame) y no se muestran como medidas exactas al alumno.

## Modelo y límites

[Modelo original Meta](https://huggingface.co/facebook/wav2vec2-xlsr-53-espeak-cv-ft): etiquetas fonéticas multilingües, entrada 16 kHz, Apache-2.0. [Conversión ONNX](https://huggingface.co/iwillsolvehardestproblem/wav2vec2-xlsr-53-espeak-cv-ft-onnx): INT8 MatMul, revisión y hash fijados en `tools/fetch_model.py`. No entrenamos pesos nuevos. Su vocabulario es eSpeak IPA, no ARPABET.

El spike de ECHO demuestra que el modelo corre y distingue sonidos; no valida su uso educativo. En referencias sintéticas confundió /ɪ/ con /e/ o /ɛ/, /æ/ con /ɛ/ y vocales reducidas. Incluso una pronunciación nativa válida puede obtener un score bajo. `water` tiene variantes legítimas (incluida /ɑ/ vs /ɔ/); la alpha no tiene una red de variantes y puede penalizarlas. No escondemos estas diferencias ni ajustamos los objetivos para inflar el benchmark.

Las referencias sintéticas se usan para escuchar y para smoke tests. Las secuencias objetivo fueron escritas manualmente. Un especialista debe revisar ambas; un TTS no es un patrón humano de oro.

No se miden estrés, ritmo, pitch, entonación ni duración fonética educativa. Hay consejos sobre esos aspectos, pero **ninguna puntuación de prosodia**. Tampoco hay score ASR de inteligibilidad. La criba de ruido no reemplaza un VAD ni estima SNR; ruido de baja frecuencia puede pasarla.

## Sustituir el motor

`PronunciationEngine.assess(samples, exercise)` es el contrato. `AcousticModel.infer` permite cambiar solo el backend acústico; valida vocabulario y normalización. Para otro tokenizer o pronunciaciones alternativas, reemplaza también el alineamiento. Mantén rechazos explícitos y versiona la escala. No comparar puntuaciones entre versiones sin recalibrar.
