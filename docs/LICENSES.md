# Licencias y procedencia

Revisión de fuentes primarias: 2026-09-23. Las licencias de runtime y pesos se revisan por separado. No se usa ningún servicio de pago ni una dependencia con restricción no comercial en el producto.

| Recurso | Versión/procedencia | Licencia declarada | Uso |
| --- | --- | --- | --- |
| ECHO | código/contenido original | MIT | app |
| Kotlin stdlib | 1.9.24, JetBrains | Apache-2.0 | runtime |
| ONNX Runtime Android | 1.22.0, Microsoft | MIT y avisos transitivos | runtime |
| wav2vec2 phoneme | Meta/Facebook `wav2vec2-xlsr-53-espeak-cv-ft` | Apache-2.0 | pesos |
| ONNX INT8 del anterior | `iwillsolvehardestproblem`, revisión `1bc86aec48933fdced56df9e16bc132152d208e8` | Apache-2.0 | pesos, sin modificar |
| Kokoro-82M v1.0 | hexgrad | Apache-2.0 | generar WAVs fuera de app |
| Kokoro ONNX y voz af_heart | onnx-community, revisión `1939ad2a8e416c0acfeecc08a694d14ef25f2231` | Apache-2.0 | solo build-time |
| Android SDK / AGP | SDK 34 / AGP 8.5.1 | herramientas/plataforma con sus términos; AGP Apache-2.0 | compilación |
| Gradle | 8.8 | Apache-2.0 | wrapper/build |
| JUnit | 4.13.2 | EPL-1.0 | tests, no APK de producción |
| AndroidX test runner / ext junit | 1.5.2 / 1.1.5 | Apache-2.0 | APK de tests |
| org.json | 20240303 | public domain según proyecto | tests JVM |
| Python NumPy / ONNX Runtime | ver `tools/requirements.txt` | BSD-3-Clause / MIT | herramientas opcionales |

Referencias primarias: [Meta](https://huggingface.co/facebook/wav2vec2-xlsr-53-espeak-cv-ft), [export ONNX](https://huggingface.co/iwillsolvehardestproblem/wav2vec2-xlsr-53-espeak-cv-ft-onnx), [Kokoro](https://huggingface.co/hexgrad/Kokoro-82M), [Kokoro ONNX](https://huggingface.co/onnx-community/Kokoro-82M-v1.0-ONNX), [ORT MIT](https://github.com/microsoft/onnxruntime/blob/v1.22.0/LICENSE), [ORT avisos](https://github.com/microsoft/onnxruntime/blob/v1.22.0/ThirdPartyNotices.txt), [Kotlin](https://github.com/JetBrains/kotlin/blob/v1.9.24/license/LICENSE.txt), [JUnit](https://github.com/junit-team/junit4/blob/r4.13.2/LICENSE-junit.txt), [JSON-java](https://github.com/stleary/JSON-java/blob/20240303/LICENSE).

La app muestra atribuciones en Ajustes → Licencias. Los textos completos están en `app/src/main/assets/licenses/`. Los WAVs son salida sintética generada específicamente para ECHO; no son grabaciones humanas ni audio extraído de sitios. No se redistribuye Kokoro, no se integra eSpeak como biblioteca y no se descarga CMUdict.

**Excluidos:** L2-ARCTIC (CC BY-NC 4.0). Common Voice y CMUdict se investigaron, no se incorporaron. Ninguna imagen/fuente/sonido propietario de Niagara. Los widgets y fuentes son del sistema.

Los modelos se distribuyen bajo lo declarado por sus autores; estas declaraciones no constituyen una auditoría independiente de cada dato de entrenamiento. Conservar la procedencia, los hashes y los avisos al redistribuir. No convertir la licencia permisiva del runtime en una afirmación sobre modelos nuevos: revisarlos individualmente.
