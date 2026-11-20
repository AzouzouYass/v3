package ma.netcourse;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Bitmap;
import android.hardware.HardwareBuffer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.Locale;

/** Analyse la course affichée; aucune automatisation des boutons de réservation. */
public final class NetCourseService extends AccessibilityService {
    private static final String TARGET="sinet.startup.inDriver";
    private static final int NEUTRAL=0xff455a64, GREEN=0xff0f7a4a,
        ORANGE=0xffa15a00, RED=0xffa32828;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Runnable scheduled=this::scan;
    private BannerOverlay banner;
    private OcrDistance ocr;
    private Prefs prefs;
    private boolean takingScreenshot=false;
    private long lastScreenshot=0;

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        prefs=new Prefs(this);
        banner=new BannerOverlay(this);
        ocr=new OcrDistance();
        log("Service connecté, reconnaissance locale activée");
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if(event==null || event.getPackageName()==null) return;
        if(!TARGET.equals(event.getPackageName().toString())) return;
        main.removeCallbacks(scheduled);
        main.postDelayed(scheduled,550);
    }
    private void scan() {
        if(banner==null || ocr==null || takingScreenshot) return;
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root==null || root.getPackageName()==null ||
            !TARGET.equals(root.getPackageName().toString())) {
            banner.hide(); return;
        }
        InDriveReader.Screen screen=InDriveReader.read(root);
        if(!screen.detail) {banner.hide();return;}
        if(screen.price<0 || screen.approach<0) {
            banner.show("NetCourse : "+screen.error,NEUTRAL);
            log(screen.error);
            return;
        }
        long now=SystemClock.elapsedRealtime();
        // Captures espacées pour respecter les limites Android.
        long elapsed=now-lastScreenshot;
        if(lastScreenshot>0 && elapsed<2500) {
            main.postDelayed(scheduled,2500-elapsed);
            return;
        }
        lastScreenshot=now;
        takingScreenshot=true;
        final InDriveReader.Screen selected=screen;
        banner.hideForScreenshot();
        main.postDelayed(() -> {
            try {
                takeScreenshot(Display.DEFAULT_DISPLAY, getMainExecutor(),
                    new TakeScreenshotCallback() {
                        @Override public void onSuccess(ScreenshotResult result) {
                            Bitmap screenshot=null;
                            try {
                                HardwareBuffer buffer=result.getHardwareBuffer();
                                if(buffer!=null) {
                                    Bitmap hardware=Bitmap.wrapHardwareBuffer(buffer,result.getColorSpace());
                                    if(hardware!=null) screenshot=hardware.copy(Bitmap.Config.ARGB_8888,false);
                                    buffer.close();
                                }
                            } catch(Exception e) {log("Capture non convertie: "+e.getClass().getSimpleName());}
                            banner.restore();
                            if(screenshot==null) {
                                noVerdict("Capture écran indisponible");
                                takingScreenshot=false;
                                return;
                            }
                            analyze(screenshot,selected);
                        }
                        @Override public void onFailure(int errorCode) {
                            banner.restore();
                            noVerdict("Capture refusée par Android (code "+errorCode+")");
                            takingScreenshot=false;
                        }
                    });
            } catch(Exception e) {
                banner.restore();
                noVerdict("Capture impossible: "+e.getClass().getSimpleName());
                takingScreenshot=false;
            }
        },160);
    }
    private void analyze(Bitmap screenBitmap,InDriveReader.Screen selected) {
        int bottom=selected.mapBottom>0?selected.mapBottom:(int)(screenBitmap.getHeight()*0.55);
        // Android peut rendre une image plus grande que les coordonnées de l'arbre,
        // mais la grande majorité des appareils utilisent le même repère.
        ColorBoxDetector.Found found=ColorBoxDetector.detect(screenBitmap,bottom);
        if(found.green==null) {
            noVerdict("Rectangle vert introuvable. Aucune estimation fiable.");
            log("Approche="+selected.approach+"km, prix="+selected.price+
                "DH, bleu="+found.blue+", vert absent");
            screenBitmap.recycle(); takingScreenshot=false;
            return;
        }
        // OCR des deux étiquettes, avec la lecture d'accessibilité comme contrôle du bleu.
        ocr.read(screenBitmap,found.blue,blue -> ocr.read(screenBitmap,found.green,green -> {
            try {
                log(String.format(Locale.FRANCE,
                    "Offre %.1f DH | approche accessibilité %.1f km | OCR bleu %.1f | OCR vert %.1f | boîtes %s / %s",
                    selected.price,selected.approach,blue,green,found.blue,found.green));
                if(green<0) {
                    noVerdict("Distance verte illisible; ouvrez bien la carte.");
                    return;
                }
                if(blue>=0 && Math.abs(blue-selected.approach)>Math.max(0.9,selected.approach*0.25)) {
                    noVerdict("Distance bleue contradictoire; calcul suspendu.");
                    return;
                }
                // L'offre a-t-elle changé pendant l'OCR ?
                AccessibilityNodeInfo active=getRootInActiveWindow();
                InDriveReader.Screen current=InDriveReader.read(active);
                if(!current.detail || Math.abs(current.price-selected.price)>0.01 ||
                    Math.abs(current.approach-selected.approach)>0.7) {
                    noVerdict("L'offre a changé; nouvelle analyse nécessaire.");
                    main.postDelayed(scheduled,300);
                    return;
                }
                RideCalculator.Evaluation e=RideCalculator.calculate(selected.price,selected.approach,green,prefs.config());
                String first=e.viable ? "ACCEPTER" : "NÉGOCIER ≥ "+e.minimumPriceDh+" DH";
                int color=e.viable?GREEN:(selected.price<e.minimumPriceDh*0.72?RED:ORANGE);
                String label=String.format(Locale.FRANCE,
                    "%s | Net %.1f DH · %.2f DH/km\nApproche %.1f km + course %.1f km = %.1f km",
                    first,e.netDh,e.netDhPerKm,selected.approach,green,e.totalKm);
                banner.show(label,color);
                log(String.format(Locale.FRANCE,"Net=%.2f DH, net/km=%.2f, minimum=%d DH, verdict=%s",
                    e.netDh,e.netDhPerKm,e.minimumPriceDh,first));
            } catch(Exception ex) { noVerdict("Erreur de calcul: "+ex.getClass().getSimpleName()); }
            finally {screenBitmap.recycle();takingScreenshot=false;}
        }));
    }
    private void noVerdict(String message) {
        if(banner!=null) banner.show("ANALYSE INCOMPLÈTE\n"+message,NEUTRAL);
        log(message);
    }
    private void log(String message) {
        if(prefs==null) return;
        if(!prefs.diagnostic() && !message.startsWith("Service")) return;
        android.content.SharedPreferences p=getSharedPreferences("app_debug",MODE_PRIVATE);
        String old=p.getString("last_diag","");
        String line=String.format(Locale.ROOT,"%tT  %s\n",new java.util.Date(),message);
        String next=(old+line);
        if(next.length()>10000) next=next.substring(next.length()-10000);
        p.edit().putString("last_diag",next).apply();
    }
    @Override public void onInterrupt() {if(banner!=null)banner.hide();}
    @Override public void onDestroy() {
        main.removeCallbacksAndMessages(null);
        if(ocr!=null)ocr.close();
        if(banner!=null)banner.hide();
        super.onDestroy();
    }
}
