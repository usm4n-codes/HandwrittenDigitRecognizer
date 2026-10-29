package com.example.handwrittendigitrecognizer;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

/** Loads the MNIST model, preprocesses drawings, and executes TensorFlow Lite inference. */
public class DigitClassifier implements AutoCloseable {
    private static final String MODEL_FILE = "digit_model.tflite";
    private static final int IMAGE_SIDE = 28;
    private static final int DIGIT_BOX_SIDE = 20;

    private final Interpreter interpreter;

    public DigitClassifier(Context context) throws IOException {
        // Reuse two CPU threads for the small convolutional network.
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(2);
        interpreter = new Interpreter(loadModel(context), options);

        // Confirm the bundled model matches the arrays passed to interpreter.run().
        int[] inputShape = interpreter.getInputTensor(0).shape();
        int[] outputShape = interpreter.getOutputTensor(0).shape();
        if (!Arrays.equals(inputShape, new int[]{1, 28, 28, 1})
                || !Arrays.equals(outputShape, new int[]{1, 10})) {
            interpreter.close();
            throw new IOException("The model has an unexpected input or output shape.");
        }
    }

    /** Run the model and return the class index with its associated softmax score. */
    public Prediction predict(Bitmap drawing) {
        float[][][][] input = preprocess(drawing);
        float[][] scores = new float[1][10];

        // TFLite reads the 28x28 input and writes ten digit scores into this output array.
        interpreter.run(input, scores);

        int bestDigit = 0;
        for (int digit = 1; digit < scores[0].length; digit++) {
            if (scores[0][digit] > scores[0][bestDigit]) {
                bestDigit = digit;
            }
        }
        return new Prediction(bestDigit, scores[0][bestDigit]);
    }

    /**
     * MNIST has bright digit pixels on a dark background. The screen has dark
     * ink on white, so this method finds the ink, resizes it into a 20x20 box,
     * centers it in a 28x28 field, then reverses and normalizes pixel values.
     */
    private float[][][][] preprocess(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        // Find the bounding rectangle around pixels darker than the white paper.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (Color.red(bitmap.getPixel(x, y)) < 245) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            throw new IllegalArgumentException("The drawing is empty.");
        }

        int boxWidth = maxX - minX + 1;
        int boxHeight = maxY - minY + 1;
        // Keep the original proportions while fitting the digit into 20 x 20.
        float scale = Math.min((float) DIGIT_BOX_SIDE / boxWidth,
                (float) DIGIT_BOX_SIDE / boxHeight);
        int scaledWidth = Math.max(1, Math.round(boxWidth * scale));
        int scaledHeight = Math.max(1, Math.round(boxHeight * scale));
        int left = (IMAGE_SIDE - scaledWidth) / 2;
        int top = (IMAGE_SIDE - scaledHeight) / 2;

        // Array dimensions follow the model's [batch, height, width, channel] tensor.
        float[][][][] input = new float[1][IMAGE_SIDE][IMAGE_SIDE][1];
        for (int y = 0; y < scaledHeight; y++) {
            int sourceY = minY + Math.min(boxHeight - 1, Math.round(y / scale));
            for (int x = 0; x < scaledWidth; x++) {
                int sourceX = minX + Math.min(boxWidth - 1, Math.round(x / scale));
                int gray = Color.red(bitmap.getPixel(sourceX, sourceY));
                // White paper becomes 0 and black ink becomes 1, matching MNIST float inputs.
                input[0][top + y][left + x][0] = (255 - gray) / 255f;
            }
        }
        return input;
    }

    /** Memory-map the uncompressed asset so the full model is not copied into another array. */
    private static MappedByteBuffer loadModel(Context context) throws IOException {
        try (AssetFileDescriptor asset = context.getAssets().openFd(MODEL_FILE);
             FileInputStream stream = new FileInputStream(asset.getFileDescriptor());
             FileChannel channel = stream.getChannel()) {
            return channel.map(FileChannel.MapMode.READ_ONLY,
                    asset.getStartOffset(), asset.getLength());
        }
    }

    /** Release the interpreter's native resources when the Activity is destroyed. */
    @Override
    public void close() {
        interpreter.close();
    }

    /** Keep the predicted class and its score together as one result. */
    public static final class Prediction {
        public final int digit;
        public final float confidence;

        Prediction(int digit, float confidence) {
            this.digit = digit;
            this.confidence = confidence;
        }
    }
}