package ma.netcourse;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.View;
import android.widget.TextView;
import java.util.Locale;

/** Affichage non interactif: les clics passent intégralement vers inDrive. */
final class BannerOverlay {
    private final WindowManager wm;
    private final TextView view;
    private final WindowManager.LayoutParams lp;
    private boolean added=false;
    private int bg=0xff455a64;
    private String last="";
    BannerOverlay(AccessibilityService service) {
        wm=(WindowManager)service.getSystemService(AccessibilityService.WINDOW_SERVICE);
        view=new TextView(service);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(Color.WHITE);
        view.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        view.setTextSize(18);
        int pad=(int)(12*service.getResources().getDisplayMetrics().density);
        view.setPadding(pad,pad,pad,pad);
        lp=new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        lp.gravity=Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y=(int)(64*service.getResources().getDisplayMetrics().density);
    }
    void show(String text,int color) {
        last=text;bg=color;
        view.setText(text);
        GradientDrawable shape=new GradientDrawable();
        shape.setColor(bg);
        shape.setCornerRadius(20);
        view.setBackground(shape);
        if(!added) {
            try {wm.addView(view,lp);added=true;}
            catch(Exception ignored) {added=false;}
        }
        else view.setVisibility(View.VISIBLE);
    }
    void hideForScreenshot() {if(added) view.setVisibility(View.GONE);}
    void restore() {if(added) view.setVisibility(View.VISIBLE);}
    void hide() {
        if(added) {
            try { wm.removeView(view); } catch(Exception ignored) { }
            added=false;
        }
    }
}
