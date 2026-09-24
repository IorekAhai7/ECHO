# Decisiones — 2026-09-23

## Plataforma

| Alternativa | Decisión |
| --- | --- |
| Android Kotlin nativo | Elegido para probar ONNX CPU, audio y permisos con herramientas disponibles. UI de widgets nativos, poca dependencia y recursos de sistema. |
| Flutter/Dart | Viable para ambas plataformas, pero un puente ONNX/audio añade integración antes de demostrar el núcleo. SDK local antiguo tampoco aporta una ventaja inmediata. No descartado para producto posterior. |
| React Native | Requiere puente nativo de audio/ML y runtime adicional; sin ventaja para este spike. |
| Swift/iOS | Adecuado para iOS, pero priorizamos un APK instalable sin firma de distribución. Portar adaptadores posteriormente. |
| C/C++ compartido | ONNX ya ejecuta cómputo nativo; un motor propio C++ sería coste innecesario ahora. |

## Evaluación offline

| Motor/enfoque | Evaluación |
| --- | --- |
| [ONNX Runtime Mobile](https://onnxruntime.ai/docs/tutorials/mobile/) + wav2vec2 fonético | Elegido: acceso directo a logits fonéticos y control de alineamiento. Peso/memoria altos; errores importantes de vocales. |
| [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) | Apache-2.0, soporte móvil offline sólido para ASR/TTS. No basta por sí solo para validar GOP; necesitamos salida fonética/posteriores y modelo apropiado. No integrado. |
| [whisper.cpp](https://github.com/ggml-org/whisper.cpp) | Inferencia local posible; transcripción de palabras no es evidencia suficiente de precisión articulatoria. No usado como juez. |
| TFLite | Runtime móvil posible, pero no encontramos para esta sesión un modelo fonético con el mismo camino de exportación verificado. No se probó conversión. |
| Forced alignment solo | Da tiempos condicionados al texto, puede «encontrar» palabras ausentes. Insuficiente; combinado con decodificación independiente. |
| MFCC/DTW de audio bruto | Fácil de ejecutar offline, demasiado sensible a voz, ritmo y canal para diagnosticar sonidos. No usado para inventar scores. |
| GOP clásico / modelo MDD calibrado | Objetivo de investigación posterior. Esta fórmula no se etiqueta como GOP validado. |
| APIs cloud | Excluidas por privacidad, offline y coste; no se evaluó una compra. |

## Recursos

[CMUdict](https://github.com/cmusphinx/cmudict/blob/master/LICENSE) tiene términos permisivos con atribución; no se incluye porque 24 ejercicios manuales bastan y el motor usa otro inventario fonético. [Common Voice legacy](https://huggingface.co/datasets/legacy-datasets/common_voice) declara CC0; sirve para ASR, no aporta automáticamente etiquetas de errores de pronunciación. No se descargó un corpus.

[L2-ARCTIC](https://psi.engr.tamu.edu/l2-arctic-corpus/) tiene anotaciones útiles y hablantes españoles, pero declara CC BY-NC 4.0. **No se integra ni descarga** para un proyecto con posible distribución comercial. No se necesita resolver esa licencia para construir la alpha; validar con un corpus propio consentido es la siguiente vía.

## Referencias

WAVs sintetizados localmente con [Kokoro-82M](https://huggingface.co/hexgrad/Kokoro-82M), modelo Apache-2.0; [ONNX y voz](https://huggingface.co/onnx-community/Kokoro-82M-v1.0-ONNX), Apache-2.0. Se distribuyen solo 24 audios pequeños. TTS del sistema no garantiza voz descargada ni consistencia; TTS en runtime aumenta tamaño y latencia innecesarios. Corpus pregrabados implicarían buscar cada frase y revisar atribuciones. Revisión fonética humana de los WAVs pendiente.

## Producto

Lista vertical, texto dominante, acciones inferiores al alcance del pulgar, pocos colores y sin gamificación. Identidad original (marca tipográfica y símbolo E/eco); no assets ni layouts copiados de Niagara. Sin animaciones decorativas, por lo que reduced-motion no necesita excepciones. Hápticos desactivables y sin efectos sonoros. Esta es una primera implementación, no una auditoría de accesibilidad certificada.

### Regenerar audios (opcional)

Los WAVs ya están en Git. Para regenerarlos localmente, usa Python 3.14 y los paquetes fijados en `tools/requirements.txt` (versiones del entorno de spike). Otros Python pueden necesitar versiones compatibles del runtime; eso no afecta la app.

```sh
python3 -m venv .venv
.venv/bin/pip install -r tools/requirements.txt
python3 tools/fetch_tts.py
.venv/bin/python tools/generate_reference.py --tts-dir work/tts
.venv/bin/python tools/spike.py --output work/spike-results.json
```

`tools/tts-manifest.json` fija hashes del modelo, voz y tokenizador. El config original usa una URL de rama con hash obligatorio: si cambia, falla y exige revisión explícita. La preparación usa red; la síntesis y el spike posteriores se ejecutan localmente. No ejecutes estos scripts sobre grabaciones privadas si agregas cambios que llamen a servicios externos.
