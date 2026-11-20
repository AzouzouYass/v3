package ma.netcourse;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** Réglages simples, comme dans la version 1. */
public final class MainActivity extends Activity {
    private Prefs prefs;
    private LinearLayout root;
    private int dp(float px) {return (int)(px*getResources().getDisplayMetrics().density+0.5f);}

    @Override public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        prefs=new Prefs(this);
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22),dp(30),dp(22),dp(22));
        root.setBackgroundColor(0xfff6f8f8);
        scroll.addView(root);
        setContentView(scroll);

        TextView title=text("Net course · Version 2",27,true);
        title.setTextColor(0xff0f7a4a);
        root.addView(title);
        root.addView(text("La bulle analyse l'offre ouverte : bleu = approche, vert = trajet client. La capture est traitée localement, sans enregistrer d'image.",15,false));
        button("Ouvrir les réglages d'accessibilité",v -> {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        });
        root.addView(text("Réglages de rentabilité",21,true));
        field("Prix du gazole (DH/L)",Prefs.DIESEL,10.5);
        field("Consommation (L/100 km)",Prefs.CONSO,5.2);
        field("Commission inDrive (%)",Prefs.COMM,10.0);
        field("Usure et entretien (DH/km)",Prefs.WEAR,0.4);
        field("Retour à vide supplémentaire (km)",Prefs.BACK,0.0);
        field("Net minimum voulu (DH/km total)",Prefs.TKM,2.0);
        root.addView(text("Formule : net = prix × (1 − commission) − distance totale × (carburant/km + usure/km). Distance totale = approche + course + retour supplémentaire.",14,false));
        Switch mode=new Switch(this);
        mode.setText("Mode diagnostic (sans noms ni adresses)");
        mode.setTextColor(Color.DKGRAY);
        mode.setTextSize(15);
        mode.setChecked(prefs.diagnostic());
        mode.setOnCheckedChangeListener((view, checked) -> prefs.setDiagnostic(checked));
        root.addView(mode);
        button("Copier le diagnostic",v -> {
            String diagnostic=getSharedPreferences("app_debug",MODE_PRIVATE).getString("last_diag","");
            if(diagnostic.isEmpty()) {
                Toast.makeText(this,"Aucun diagnostic. Ouvre une offre inDrive.",Toast.LENGTH_LONG).show();
                return;
            }
            ClipboardManager cb=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            cb.setPrimaryClip(ClipData.newPlainText("NetCourse V2",diagnostic));
            Toast.makeText(this,"Diagnostic copié",Toast.LENGTH_SHORT).show();
        });
        root.addView(text("Important : n'accepte pas une course sur la seule base de la bulle avant d'avoir vérifié les valeurs en conditions réelles. L'application ne clique jamais sur inDrive.",14,false));
    }
    private TextView text(String text,int sp,boolean bold) {
        TextView view=new TextView(this);
        view.setText(text);view.setTextSize(sp);view.setTextColor(0xff303c40);
        view.setPadding(0,dp(9),0,dp(11));
        if(bold)view.setTypeface(null,1);
        return view;
    }
    private void field(String label,String key,double def) {
        root.addView(text(label,15,false));
        EditText edit=new EditText(this);
        edit.setSingleLine(true);
        edit.setTextSize(18);
        edit.setTextColor(Color.BLACK);
        edit.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);
        edit.setText(String.valueOf(prefs.get(key,def)));
        edit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            @Override public void onTextChanged(CharSequence s,int start,int before,int count) {
                prefs.put(key,s.toString());
            }
            @Override public void afterTextChanged(Editable e) { }
        });
        root.addView(edit);
    }
    private void button(String title,View.OnClickListener callback) {
        Button b=new Button(this);b.setAllCaps(false);b.setText(title);
        b.setOnClickListener(callback);root.addView(b);
    }
}
