package com.shohan.mediabridge.viewer;
import android.content.Intent;import android.graphics.*;import android.net.Uri;
import android.os.Bundle;import android.view.MotionEvent;import android.view.ScaleGestureDetector;
import android.view.View;import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.shohan.mediabridge.databinding.ActivityImageViewerBinding;
import java.io.File;
public class ImageViewerActivity extends AppCompatActivity {
    private ActivityImageViewerBinding b;
    private final Matrix matrix=new Matrix(),saved=new Matrix();
    private final PointF start=new PointF(),mid=new PointF();
    private float oldDist=1f;private int mode=0;
    private ScaleGestureDetector sgd;
    @Override protected void onCreate(Bundle s){
        super.onCreate(s);
        b=ActivityImageViewerBinding.inflate(getLayoutInflater());setContentView(b.getRoot());
        String path=getIntent().getStringExtra("path");
        String title=getIntent().getStringExtra("title");
        b.tvTitle.setText(title!=null?title:"Image");
        b.btnBack.setOnClickListener(v->finish());
        b.btnShare.setOnClickListener(v->{if(path!=null)share(path);});
        if(path==null||!new File(path).exists()){Toast.makeText(this,"File not found",Toast.LENGTH_SHORT).show();return;}
        loadImage(path);
        sgd=new ScaleGestureDetector(this,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override public boolean onScale(ScaleGestureDetector d){
                float f=d.getScaleFactor();matrix.postScale(f,f,d.getFocusX(),d.getFocusY());
                b.imageView.setImageMatrix(matrix);return true;}});
        b.imageView.setOnTouchListener((v,e)->{
            sgd.onTouchEvent(e);
            switch(e.getAction()&MotionEvent.ACTION_MASK){
                case MotionEvent.ACTION_DOWN:saved.set(matrix);start.set(e.getX(),e.getY());mode=1;break;
                case MotionEvent.ACTION_POINTER_DOWN:oldDist=sp(e);saved.set(matrix);mp2(mid,e);mode=2;break;
                case MotionEvent.ACTION_MOVE:
                    if(mode==1){matrix.set(saved);matrix.postTranslate(e.getX()-start.x,e.getY()-start.y);}
                    else if(mode==2){float nd=sp(e);if(nd>10f){matrix.set(saved);float sc=nd/oldDist;matrix.postScale(sc,sc,mid.x,mid.y);}}
                    break;
                case MotionEvent.ACTION_UP:case MotionEvent.ACTION_POINTER_UP:mode=0;break;}
            b.imageView.setImageMatrix(matrix);return true;});
    }
    private void loadImage(String path){
        BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;
        BitmapFactory.decodeFile(path,o);
        o.inSampleSize=Math.max(1,Math.max(o.outWidth/2048,o.outHeight/2048));
        o.inJustDecodeBounds=false;
        Bitmap bmp=BitmapFactory.decodeFile(path,o);
        if(bmp!=null){b.imageView.setScaleType(android.widget.ImageView.ScaleType.MATRIX);
            b.imageView.setImageBitmap(bmp);b.imageView.setImageMatrix(matrix);}
        else Toast.makeText(this,"Cannot load image",Toast.LENGTH_SHORT).show();}
    private float sp(MotionEvent e){float x=e.getX(0)-e.getX(1),y=e.getY(0)-e.getY(1);return(float)Math.sqrt(x*x+y*y);}
    private void mp2(PointF p,MotionEvent e){p.set((e.getX(0)+e.getX(1))/2,(e.getY(0)+e.getY(1))/2);}
    private void share(String path){Intent i=new Intent(Intent.ACTION_SEND);i.setType("image/*");
        i.putExtra(Intent.EXTRA_STREAM,Uri.fromFile(new File(path)));
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(i,"Share Image"));}
}
