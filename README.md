# ClockScreen

Aplicativo Android nativo de relógio digital, com protetor de tela em tela cheia e widget para a tela inicial.

## Funcionalidades

- Relógio digital em tela cheia com dígitos de sete segmentos.
- Efeito visual vermelho com brilho.
- Deslocamento discreto periódico dos dígitos para reduzir o risco de retenção de imagem.
- Widget com hora, dia da semana e data.
- Interface e datas em português do Brasil.
- Funcionamento local, sem conexão com a internet ou acesso à localização.

## Requisitos de desenvolvimento

- JDK 17.
- Android SDK Platform 36.
- Gradle Wrapper 8.13 (incluído no repositório).
- Android Gradle Plugin 8.13.2.
- Kotlin 2.4.21.

## Compilar e testar

No Windows:

```powershell
.gradlew.bat testDebugUnitTest assembleDebug
```

No Linux ou macOS:

```bash
./gradlew testDebugUnitTest assembleDebug
```

O APK de depuração é gerado em `app/build/outputs/apk/debug/`.

## Compatibilidade

- Android mínimo: API 24 (Android 7.0).
- Target SDK: API 34.
- Java: 17.

## Estrutura

- `app/src/main/java/br/com/clockscreen/`: código Kotlin.
- `app/src/main/res/layout/`: layouts XML.
- `app/src/main/res/drawable/`: recursos visuais.
- `app/src/test/`: testes unitários.
