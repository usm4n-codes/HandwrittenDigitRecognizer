# Beginner Code Walkthrough
## MainActivity
An Activity is a screen controller with lifecycle callbacks. Android invokes onCreate during screen creation. setContentView inflates activity_main.xml; findViewById connects XML IDs to Java View objects. Click listeners handle Predict and Clear. Predict validates input and displays output. onDestroy closes native model resources.

## DrawingView
A custom View implements drawing. Paint configures the rounded stroke; Path stores touch coordinates; onTouchEvent handles down/move/up. invalidate requests onDraw. Canvas draws white background and Path. getDrawingBitmap renders the stroke into pixels.

## DigitClassifier
The constructor maps digit_model.tflite from assets and initializes Interpreter. It checks input [1,28,28,1] and output [1,10]. Preprocessing finds dark pixels, resizes within 20x20, centers in 28x28, inverts polarity and scales to [0,1]. interpreter.run fills ten scores; the max index is the prediction. Confidence is a score, not a guarantee.

## Execution
Launch -> inflate XML -> draw Path -> snapshot Bitmap -> preprocess tensor -> run TFLite -> display. One image has shape [1,28,28,1]; ten scores have shape [1,10].
