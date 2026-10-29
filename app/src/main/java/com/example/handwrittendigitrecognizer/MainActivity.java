package com.example.handwrittendigitrecognizer;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

/** Controls the screen and connects drawing actions to the digit classifier. */
public class MainActivity extends AppCompatActivity {
    private DrawingView drawingView;
    private DigitClassifier classifier;
    private TextView resultText;
    private TextView confidenceText;

    /** Android calls this when it creates the Activity screen. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate the XML layout so its declared controls become Android View objects.
        setContentView(R.layout.activity_main);

        // Apply system-bar padding so the header is not covered by the status bar.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootView), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // Find the controls by the IDs that are declared in activity_main.xml.
        drawingView = findViewById(R.id.drawingView);
        resultText = findViewById(R.id.resultText);
        confidenceText = findViewById(R.id.confidenceText);
        MaterialButton predictButton = findViewById(R.id.predictButton);
        MaterialButton clearButton = findViewById(R.id.clearButton);

        // The model lives in app/src/main/assets, so initialization works offline.
        try {
            classifier = new DigitClassifier(this);
        } catch (Exception error) {
            predictButton.setEnabled(false);
            confidenceText.setText("Model could not be loaded.");
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
        }

        // A click listener responds when the user presses Predict.
        predictButton.setOnClickListener(view -> predictDigit());

        // Clear removes the Path and resets the result labels to their initial state.
        clearButton.setOnClickListener(view -> {
            drawingView.clear();
            resultText.setText("-");
            confidenceText.setText(R.string.placeholder);
        });
    }

    /** Validate the drawing, run one inference, then show the winning digit and score. */
    private void predictDigit() {
        if (!drawingView.hasDrawing()) {
            Toast.makeText(this, "Draw a digit first.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (classifier == null) {
            Toast.makeText(this, "The digit model is unavailable.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // DrawingView makes a Bitmap; the classifier prepares its tensor and runs TFLite.
            DigitClassifier.Prediction prediction =
                    classifier.predict(drawingView.getDrawingBitmap());
            resultText.setText(String.valueOf(prediction.digit));
            confidenceText.setText(String.format(
                    java.util.Locale.getDefault(), "%.1f%% confidence",
                    prediction.confidence * 100f));
        } catch (RuntimeException error) {
            Toast.makeText(this, "Could not analyze that drawing. Please try again.",
                    Toast.LENGTH_LONG).show();
        }
    }

    /** Release the native model interpreter when Android destroys this screen. */
    @Override
    protected void onDestroy() {
        if (classifier != null) {
            classifier.close();
            classifier = null;
        }
        super.onDestroy();
    }
}