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
- Gradle Wrapper 8.14.4 (incluído no repositório).
- Android Gradle Plugin 8.13.2.
- Kotlin 2.4.21.

## Compilar e testar

No Windows:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Para executar a verificação de código (Lint):

```powershell
.\gradlew.bat lint
```

No Linux ou macOS:

```bash
./gradlew testDebugUnitTest assembleDebug
```

Para executar a verificação de código (Lint):

```bash
./gradlew lint
```

O APK de depuração é gerado em `app/build/outputs/apk/debug/`.  
Os relatórios de testes e Lint são gerados em `app/build/reports/`.

## Compatibilidade

- Android mínimo: API 24 (Android 7.0 - Nougat).
- Target SDK: API 34 (Android 14).
- Compile SDK: API 36.
- Java: 17.

## Estrutura

- `app/src/main/java/br/com/clockscreen/`: código Kotlin (telas, widget e desenho dos segmentos).
- `app/src/main/res/layout/`: layouts XML.
- `app/src/main/res/drawable/`: recursos visuais e vetores.
- `app/src/main/res/values/`: temas, cores e textos.
- `app/src/test/`: testes unitários.
