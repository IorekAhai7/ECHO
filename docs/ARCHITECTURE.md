# Arquitectura

Una Activity nativa mantiene un flujo vertical: bienvenida → lista → práctica → grabación → procesamiento → resultado → repetir. No hay capa web ni servicio. Una cola de trabajo separada prepara ONNX, ejecuta inferencia y guarda intentos. El micrófono tiene un hilo de lectura acotado a 12 segundos. La interfaz nunca genera puntuaciones por su cuenta.

| Capa | Contrato / implementación |
| --- | --- |
| Presentación | `MainActivity`, estados y disclosure mediante diálogo por palabra |
| Dominio | `Exercise`, `Word`, `Assessment`, `WordScore`, `PhoneScore` |
| Contenido | JSON local validado por `Content` |
| Captura | `AudioCapture` / `Microphone` |
| Reproducción | `AudioPlayback` / `Player` |
| Reconocimiento fonético | `AcousticModel` / `OnnxModel` |
| Análisis | `PronunciationEngine` / `CtcEngine`, control de señal |
| Alineamiento | `CtcAlignment` temporal; `PhoneAlignment` de edición |
| Scoring | cálculo puro sobre logits y fonemas, versión explícita |
| Feedback | `Feedback`, consejos españoles locales |
| Persistencia | `History` / SQLite + WAV privados |
| Gestión del modelo | `ModelManager`, copia local atómica, archivo pinneado en build |
| Ajustes | SharedPreferences: onboarding y hápticos |

No hay reconocedor ortográfico: el producto no dice «entendí las palabras». Las palabras de la pantalla provienen del ejercicio; los sonidos observados sí provienen del audio. Separar posteriormente un ASR para inteligibilidad exige otra interfaz y otra métrica visible.

`core` no importa Android ni ONNX. Se puede portar a Kotlin Multiplatform o traducir a Swift conservando los tests del contrato. Audio, SQLite y UI son adaptadores Android. No se promete una versión iOS compilable en esta entrega.

Estados inválidos: escuchar bloquea grabar; grabar bloquea escuchar; procesar bloquea navegación; salir durante grabación cancela y descarta el buffer. No hay servicio de grabación en segundo plano. Los recursos se liberan al cerrar la Activity. La persistencia solo guarda intentos evaluados; rechazos de calidad no contaminan el progreso. Si el proceso muere durante grabación, ese intento no se conserva.

Deuda deliberada: la Activity concentra la coordinación de pantallas, la lista de historial aún no pagina, y SQLite usa un esquema v1 sin migraciones. Separar un controlador de estado y un repositorio abstracto antes de crecer a más plataformas. Las preferencias y el último resultado no se conservan ante muerte del proceso; los intentos guardados sí.
