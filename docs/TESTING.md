# Testing Checklist and Results

## Build and device
- Gradle 8.11.1, Temurin JDK 21.0.13+9, Android SDK 35: build succeeded.
- Installed and launched the debug APK on the Pixel 6 AVD.
- Emulator page size: 16384 bytes (16 KB). Android showed a compatibility warning for the TensorFlow Lite JNI library; the app ran in the emulator's page-size compatibility mode after the prompt was dismissed.
- The app requests no network permission. The model is bundled in the APK.

## TensorFlow Lite model check
Loaded digit_model.tflite in the TensorFlow Lite interpreter. Input tensor [1,28,28,1] and output [1,10]. One held-out MNIST test image for each class was run; actual digits 0 through 9 each produced the matching class.

## Pixel 6 drawing checks
- Hand-drawn 7 -> 7, confidence 53.6% (docs/screenshots/pixel6_digit7.png)
- Hand-drawn 1 -> 7, confidence 49.8% (misclassified; docs/screenshots/pixel6_digit1.png)
- Hand-drawn 2-shaped stroke -> 2, confidence 99.6% (docs/screenshots/pixel6_digit2.png)
- Blank input handling and Clear interaction were exercised; the app stayed open.
- Clean launch screenshot: docs/screenshots/pixel6_ready.png

The three screen drawings were made as emulator touch gestures. The 1 test demonstrates that a valid model score is not a guarantee of correctness. The runtime's 16 KB compatibility warning is documented above; this dependency can be migrated to a 16 KB-aligned LiteRT runtime for a future production build.