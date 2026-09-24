# Offline y privacidad

La APK incluye WAVs, ejercicios, consejos, vocabulario y modelo. La primera apertura copia los pesos a `Context.noBackupFilesDir/phonemes-v1.onnx` y abre ONNX; no conecta a Internet. La descarga y las dependencias suceden en la máquina de desarrollo, antes de generar la APK.

El manifiesto de producción solicita **solo RECORD_AUDIO**. No declara INTERNET ni acceso al almacenamiento compartido. ONNX Runtime usa `setTelemetry(false)` y no tiene permiso de red en este proceso. No hay servidor local ni remoto, WebView, analytics, login, anuncios o upload. Los permisos de herramientas de desarrollo y del emulador no son permisos de la aplicación.

| Dato | Almacenamiento Android privado |
| --- | --- |
| Ejercicios, referencias, pesos originales | assets de APK |
| Copia de pesos para inferencia | `no_backup/phonemes-v1.onnx` |
| Historial y detalles | `databases/echo.db` |
| Audio de intentos válidos | `files/recordings/<UUID>.wav` |
| Onboarding/hápticos | `shared_prefs/settings.xml` |
| Buffer de grabación | memoria, acotado; descartado si se cancela/rechaza |

Backups de Android desactivados (`allowBackup=false`). No se implementó exportación ni sincronización. Borrar una grabación conserva su score; borrar historial elimina todos sus audios; borrar todos mis datos elimina historial, audios y preferencias, conservando recursos de la app. Desinstalar o «Borrar almacenamiento» de Android elimina también la copia privada del modelo. No se garantiza borrado forense del medio flash.

Prueba reproducible: instalar la APK completa, activar modo avión y apagar Wi-Fi, abrir, escuchar, grabar, analizar y repetir. La integración automatizada hace inferencia/persistencia con recursos reales sin INTERNET; la captura de una persona en dispositivo físico queda pendiente. La primera carga y los errores de modelo son visibles, no se sustituyen por resultados falsos.
