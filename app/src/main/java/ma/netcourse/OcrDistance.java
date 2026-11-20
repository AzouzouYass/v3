package ma.netcourse;

import android.graphics.Bitmap;
import android.graphics.Rect;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reconnaissance locale du texte uniquement dans un rectangle de couleur donné. */
final class OcrDistance {
    private static final Pattern KM=Pattern.compile("(?i)(\\d{1,3}(?:[.,]\\d{1,2})?)\\s*(?:km|k m|kn)");
    private final TextRecognizer recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    void read(Bitmap screenshot, ColorBoxDetector.Box box, Consumer<Double> callback) {
        if(box==null) { callback.accept(-1.0);return; }
        int left=Math.max(0,box.x-9), top=Math.max(0,box.y-9);
        int right=Math.min(screenshot.getWidth(),box.right+9);
        int bottom=Math.min(screenshot.getHeight(),box.bottom+9);
        if(right-left<20 || bottom-top<20) {callback.accept(-1.0);return;}
        Bitmap crop=Bitmap.createBitmap(screenshot,left,top,right-left,bottom-top);
        InputImage image=InputImage.fromBitmap(crop,0);
        recognizer.process(image)
            .addOnSuccessListener(result -> callback.accept(parse(result.getText())))
            .addOnFailureListener(err -> callback.accept(-1.0))
            .addOnCompleteListener(task -> crop.recycle());
    }
    static double parse(String text) {
        if(text==null) return -1;
        Matcher m=KM.matcher(text.replace('\n',' '));
        while(m.find()) {
            try {
                double v=Double.parseDouble(m.group(1).replace(',','.'));
                if(v>=0.1 && v<=300) return v;
            } catch (NumberFormatException ignored) { }
        }
        return -1;
    }
    void close() { recognizer.close(); }
}
