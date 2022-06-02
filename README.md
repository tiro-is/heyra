# Heyra - Icelandic speech recognition for Android

The Heyra project provides two loosely coupled components, an implementation of
Android's speech recognition interface and an intent handler activity for speech
recognition actions from other applications.

Heyra provides an implementaion of
[`android.speech.RecognitionService`](https://developer.android.com/reference/kotlin/android/speech/RecognitionService)
that interfaces with an online gRPC speech recognition service. It can be any
service that implements
[tiro.speech.v1alpha.Speech](https://github.com/tiro-is/tiro-speech-core/blob/master/proto/tiro/speech/v1alpha/speech.proto),
but by default it connects to `speech.tiro.is`.

To use the Heyra service in other applications one can either use it directly by
calling
[SpeechRecognizer#createSpeechRecognizer](https://developer.android.com/reference/kotlin/android/speech/SpeechRecognizer#createSpeechRecognizer(android.content.Context)):

```kotlin
recognizer = SpeechRecognizer.createSpeechRecognizer(
    context,
    ComponentName.unflattenFromString("is.tiro.heyra/RecognitionService")
)
```

or alternatively launch an activity with the
[RecognizerIntent.ACTION_RECOGNIZE_SPEECH](https://developer.android.com/reference/kotlin/android/speech/RecognizerIntent#ACTION_RECOGNIZE_SPEECH)
intent which will pop up a modal that performs the recognition returns the
recognized results in an [extra
bundle](https://developer.android.com/reference/android/kotlin/speech/RecognizerIntent#EXTRA_RESULTS).

The development is in early stages, so expect bugs.

## Acknowledgements

This project was funded by the Language Technology Programme for Icelandic
2019-2023. The programme, which is managed and coordinated by Almannarómur, is
funded by the Icelandic Ministry of Education, Science and Culture.

## License

All new code is Copyright © 2022 Tiro ehf and licensed under the [Apache 2.0
LICENSE](LICENSE) license.
