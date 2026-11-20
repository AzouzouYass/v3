package ma.netcourse;

import android.graphics.Bitmap;
import java.util.ArrayList;
import java.util.List;

/** Repère les rectangles bleus et verts de distance sans déduire de valeurs de l'itinéraire. */
final class ColorBoxDetector {
    static final class Box {
        int x, y, right, bottom;
        Box(int x, int y, int right, int bottom) {
            this.x=x; this.y=y; this.right=right; this.bottom=bottom;
        }
        int width() { return right-x; }
        int height() { return bottom-y; }
        @Override public String toString() { return "["+x+","+y+","+right+","+bottom+"]"; }
    }
    static final class Found {
        final Box blue, green;
        Found(Box blue, Box green) { this.blue=blue; this.green=green; }
    }
    private static final class Group {
        final int color;
        int minX, maxX, top, bottom, lastY, runs;
        double center;
        Group(int c,int start,int end,int y) {
            color=c; minX=start;maxX=end;top=y;bottom=y;lastY=y;runs=1;center=(start+end)/2.0;
        }
        void add(int a,int b,int y) {
            minX=Math.min(minX,a);maxX=Math.max(maxX,b);bottom=y;lastY=y;
            center=(center*runs+(a+b)/2.0)/(runs+1);runs++;
        }
        Box box() { return new Box(minX,top,maxX+1,bottom+4); }
    }
    /** mapBottom: haut du volet d'offre, pour éviter le bouton « Accepter ». */
    static Found detect(Bitmap bitmap, int mapBottom) {
        int w=bitmap.getWidth(), h=bitmap.getHeight();
        int y0=Math.max(0, (int)(h*0.12));
        int yEnd=Math.min(Math.min(h-1,mapBottom), (int)(h*0.68));
        int stride=Math.max(2,w/330);
        int minRun=Math.max(27, (int)(w*0.073));
        int maxRun=Math.max(minRun+1,(int)(w*0.42));
        int maxGap=Math.max(35,(int)(h*0.042));
        List<Group> groups=new ArrayList<>();
        for(int y=y0;y<yEnd;y+=3) {
            for(int color=1;color<=2;color++) {
                int runStart=-1;
                for(int x=0;x<w+stride;x+=stride) {
                    boolean hit=x<w && type(bitmap.getPixel(x,y))==color;
                    if(hit && runStart<0) runStart=x;
                    else if(!hit && runStart>=0) {
                        int end=Math.min(x,w), len=end-runStart;
                        if(len>=minRun && len<=maxRun) {
                            Group choice=null;
                            double bestDelta=Double.MAX_VALUE;
                            double mid=(runStart+end)/2.0;
                            for(Group g:groups) {
                                int overlap=Math.min(end,g.maxX)-Math.max(runStart,g.minX);
                                double delta=Math.abs(g.center-mid);
                                if(g.color==color && y-g.lastY<=maxGap && overlap>=len*0.60
                                    && delta<Math.max(22,w*0.095) && delta<bestDelta) {
                                    choice=g;bestDelta=delta;
                                }
                            }
                            if(choice==null) groups.add(new Group(color,runStart,end,y));
                            else choice.add(runStart,end,y);
                        }
                        runStart=-1;
                    }
                }
            }
        }
        Box blue=null, green=null;
        double scoreBlue=0, scoreGreen=0;
        for(Group g:groups) {
            Box b=g.box();
            double width=b.width(), height=b.height();
            if(g.runs<5 || height<Math.max(28,h*0.028) || height>h*0.24) continue;
            if(width<minRun || width>maxRun*1.25) continue;
            double ratio=width/height;
            if(ratio<0.35 || ratio>3.3) continue;
            double score=g.runs*width / (1.0+Math.abs(ratio-1.15));
            if(g.color==1 && score>scoreBlue) { scoreBlue=score;blue=b; }
            if(g.color==2 && score>scoreGreen) { scoreGreen=score;green=b; }
        }
        return new Found(blue,green);
    }
    /** Couleurs de fond des étiquettes, distinctes des lignes fines et de l'overlay sombre. */
    private static int type(int p) {
        int r=(p>>>16)&255,g=(p>>>8)&255,b=p&255;
        if(b>=130 && b>r+35 && b>g+25 && g>=55) return 1; // bleu
        if(g>=145 && r>=110 && b<=145 && g>r+9 && g>b+55) return 2; // vert citron
        return 0;
    }
}
