# ECHO

**Offline English Pronunciation Coach — Android technical alpha, 0.1.0.**

Escucha una referencia, graba tu voz, compara fonemas localmente y vuelve a intentarlo. Interfaz en español, 24 ejercicios, sin cuenta, servidores, anuncios ni permiso de Internet.

> El flujo usa un modelo acústico real; no hay puntuaciones simuladas. **La calidad pedagógica del scoring aún no está validada.** El modelo confunde vocales incluso en algunas referencias sintéticas. No uses los números como diagnóstico, certificación ni porcentaje de pronunciación correcta.

## Qué funciona

- Referencias WAV incluidas: no requieren una voz del sistema ni descargas al practicar.
- Grabación PCM16 mono a 16 kHz, hasta 12 segundos; permiso solicitado al pulsar Hablar.
- Inferencia ONNX en CPU fuera del hilo de interfaz; fonemas CTC, alineamiento temporal, comparación acústica y resultados por palabra/sonido.
- Consejos en español, repetición y comparación con el último intento del mismo ejercicio y versión de scoring.
- SQLite privado: historial, puntuación, detalles y grabaciones; reproducción y borrado de audio, historial y preferencias.
- Tema claro/oscuro del sistema, texto escalable, controles de al menos 52 dp, hápticos opcionales.

**Pendiente:** validación humana, teléfono físico, calibración, variantes de acento, discriminación robusta de ruido, prosodia, iOS y publicación en tiendas. Consulta [validación](docs/VALIDATION.md).

## Ejecutar desde cero

Requisitos: JDK 17, Android SDK 34 y Build Tools 34.0.0, Python 3.11+ para descargar el modelo; Android Studio es opcional. La build actual es **ARM64**, Android 8/API 26 o posterior. Usa un emulador ARM64 o un teléfono ARM64; un emulador x86 requiere añadir `x86_64` a `abiFilters`.

```sh
git clone https://github.com/IorekAhai7/ECHO.git
cd ECHO
# Configura JAVA_HOME a tu JDK 17 y ANDROID_HOME a tu Android SDK.
# Alternativa para SDK: local.properties con sdk.dir=/ruta/al/sdk
python3 tools/fetch_model.py
./gradlew testDebugUnitTest lintDebug assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n app.echo/.MainActivity
```

Primera preparación: Internet para Gradle y la descarga de **355 MB** de pesos. El script fija revisión y SHA-256. Los pesos se excluyen de Git y se empaquetan en la APK. Los audios ya están versionados. **Una vez instalada la APK no se descarga nada.** Reserva aproximadamente 1 GB libre durante instalación/preparación; el modelo se copia al directorio privado para ONNX. El dispositivo debería tener al menos 3 GB de RAM; esto es una recomendación provisional, no una compatibilidad medida.

La APK debug tiene firma de desarrollo y sirve para instalación manual. `./gradlew assembleRelease` genera una build sin una firma de distribución configurada; no hay claves ni credenciales incluidas. No está preparada para Google Play (target SDK, entrega de modelos, firma y límites de tamaño requieren trabajo adicional).

## Probar

```sh
# Algoritmos, silencio/ruido/volumen, audio y todos los ejercicios
./gradlew testDebugUnitTest
# Con un emulador/teléfono ARM64 conectado (usa un dispositivo de pruebas)
adb shell cmd connectivity airplane-mode enable
adb shell svc wifi disable
./gradlew connectedDebugAndroidTest
# Inspección estática
./gradlew lintDebug
```

La integración ejecuta los pesos reales sobre un WAV incluido, compara una frase incorrecta, verifica repetibilidad, SQLite persistente, borrado y ausencia del permiso INTERNET. Usa una base de datos y carpeta de pruebas separadas; no borra intentos personales. La prueba de referencia no sustituye grabaciones humanas ni garantiza que el micrófono de cada dispositivo funcione bien.

## Investigación y stack

Android nativo/Kotlin + APIs de audio Android + ONNX Runtime 1.22.0 + SQLite. Motor acústico: `facebook/wav2vec2-xlsr-53-espeak-cv-ft`, conversión INT8 de `iwillsolvehardestproblem`, Apache-2.0. Referencias generadas durante el desarrollo con Kokoro-82M v1.0 (`af_heart`), no con APIs remotas.

La elección permite probar el núcleo acústico móvil sin un puente de plugins. Flutter y React Native siguen siendo opciones posteriores; las interfaces del dominio no dependen de Android. No se usa ASR de texto para simular calidad de pronunciación. [Comparación de alternativas](docs/DECISIONS.md).

## Desarrollo

| Necesidad | Ubicación |
| --- | --- |
| UI y estados | `app/src/main/java/app/echo/MainActivity.kt` |
| Modelo de dominio e interfaces | `core/Engine.kt` |
| CTC, edición y scoring | `core/Engine.kt` |
| Consejos | `core/Feedback.kt` |
| Captura/reproducción/WAV | `platform/Audio.kt` |
| ONNX y preparación del modelo | `platform/OnnxModel.kt` |
| Persistencia | `platform/History.kt` |
| Contenido | `app/src/main/assets/exercises.json` |
| Pesos | `app/src/main/assets/models/` (descarga verificada) |
| Referencias | `app/src/main/assets/reference/` |

Para agregar un ejercicio: crea un ID único, texto, grupo, consejo, palabras y fonemas presentes en `vocab.json`; añade `reference/<id>.wav` PCM16 mono. La regla actual de prueba espera 24 ejercicios: actualízala al ampliar el catálogo. Revisa con una persona los fonemas y el audio antes de publicar nuevo contenido. `tts` contiene la transcripción para el generador Kokoro y no se usa al practicar.

Para cambiar scoring modifica `CtcEngine` y aumenta su versión. Para cambiar el modelo conserva `AcousticModel` si produce los mismos logits, o implementa `PronunciationEngine` completo. No mezcles índices de versiones distintas en comparaciones.

## Documentación

- [Arquitectura](docs/ARCHITECTURE.md)
- [Motor fonético](docs/PRONUNCIATION_ENGINE.md)
- [Qué significa el score](docs/SCORING.md)
- [Offline y privacidad](docs/OFFLINE_MODE.md)
- [Licencias y atribuciones](docs/LICENSES.md)
- [Decisiones y alternativas](docs/DECISIONS.md)
- [Validación, rendimiento y próximos pasos](docs/VALIDATION.md)

## Roadmap de salida de alpha

1. Validar referencias y un corpus consentido de hispanohablantes, con evaluación ciega de especialistas.
2. Medir falsos avisos por sonido, acento y nivel; abstenerse cuando el motor no sea fiable. Incorporar variantes y calibrar antes de prometer mejoras pedagógicas.
3. Medir captura, memoria, latencia p50/p95 y batería en teléfonos reales; probar interrupciones, Bluetooth y accesibilidad con TalkBack.
4. Reducir modelo y tamaño de instalación; conservar inferencia offline e integridad del recurso.
5. Añadir prosodia solo tras validación y portar la plataforma a iOS.
