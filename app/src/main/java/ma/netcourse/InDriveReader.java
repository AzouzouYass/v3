package ma.netcourse;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lit uniquement les champs utiles de l'offre actuellement ouverte. */
final class InDriveReader {
    static final class Screen {
        boolean detail;
        double price=-1, approach=-1;
        int mapBottom=-1;
        String error="";
    }
    private static final Pattern DH = Pattern.compile("(?i)(\\d+(?:[.,]\\d+)?)\\s*(?:MAD|DHS?|DH)");
    private static final Pattern KM = Pattern.compile("(?i)(\\d+(?:[.,]\\d+)?)\\s*(?:km|كم)");

    static Screen read(AccessibilityNodeInfo root) {
        Screen result=new Screen();
        if(root==null) {result.error="Arbre d'accessibilité indisponible";return result;}
        // La liste présente plusieurs prix: NE JAMAIS exploiter les deux premiers km de la liste.
        AccessibilityNodeInfo detail=find(root,"driver_order_map_title");
        AccessibilityNodeInfo accept=find(root,"order_button_accept_price_stage_0");
        result.detail=detail!=null || accept!=null;
        if(!result.detail) {result.error="Ouvrez une offre pour afficher les deux distances";return result;}
        AccessibilityNodeInfo priceNode=accept!=null?accept:find(root,"info_textview_stage_price_view");
        result.price=number(priceNode,DH);
        AccessibilityNodeInfo distanceNode=find(root,"order_info_stage_textview_distance");
        result.approach=number(distanceNode,KM);
        if(detail!=null) {
            Rect r=new Rect(); detail.getBoundsInScreen(r);
            if(r.top>0) result.mapBottom=r.top;
        }
        if(distanceNode!=null) {
            Rect r=new Rect(); distanceNode.getBoundsInScreen(r);
            if(r.top>0) result.mapBottom=r.top;
        }
        if(result.price<0) result.error="Prix non lu";
        else if(result.approach<0) result.error="Distance d'approche non lue";
        return result;
    }
    private static double number(AccessibilityNodeInfo n,Pattern pat) {
        if(n==null || n.getText()==null) return -1;
        Matcher m=pat.matcher(n.getText().toString());
        if(!m.find()) return -1;
        try {return Double.parseDouble(m.group(1).replace(',','.'));}
        catch(NumberFormatException e) {return -1;}
    }
    private static AccessibilityNodeInfo find(AccessibilityNodeInfo root,String idSuffix) {
        if(root==null) return null;
        String id=root.getViewIdResourceName();
        if(id!=null && id.endsWith(":id/"+idSuffix)) return root;
        int children=root.getChildCount();
        for(int i=0;i<children;i++) {
            AccessibilityNodeInfo n=root.getChild(i);
            AccessibilityNodeInfo found=find(n,idSuffix);
            if(found!=null) return found;
        }
        return null;
    }
}
