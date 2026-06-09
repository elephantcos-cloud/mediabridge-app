package com.shohan.mediabridge;
import android.Manifest;import android.content.pm.PackageManager;
import android.os.Build;import android.os.Bundle;import android.widget.Toast;
import androidx.annotation.NonNull;import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.shohan.mediabridge.databinding.ActivityMainBinding;
import com.shohan.mediabridge.ui.browse.BrowseFragment;
import com.shohan.mediabridge.ui.convert.ConvertFragment;
import com.shohan.mediabridge.ui.history.HistoryFragment;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private static final int PERM_REQ=100;
    private ActivityMainBinding b;
    private BrowseFragment browseFragment;

    @Override protected void onCreate(Bundle s){
        super.onCreate(s);
        b=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setSupportActionBar(b.toolbar);
        browseFragment=new BrowseFragment();
        b.bottomNav.setOnItemSelectedListener(item->{
            int id=item.getItemId();
            if(id==R.id.nav_browse){show(browseFragment,"browse");b.toolbar.setTitle("MediaBridge");}
            else if(id==R.id.nav_convert){show(new ConvertFragment(),"convert");b.toolbar.setTitle("Convert for Button Phone");}
            else if(id==R.id.nav_history){show(new HistoryFragment(),"history");b.toolbar.setTitle("Conversion History");}
            return true;
        });
        checkPermissions();
        if(s==null){show(browseFragment,"browse");b.toolbar.setTitle("MediaBridge");}
    }
    private void show(Fragment f,String tag){
        getSupportFragmentManager().beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in,android.R.anim.fade_out)
            .replace(R.id.fragment_container,f,tag).commit();
    }
    private void checkPermissions(){
        List<String> n=new ArrayList<>();
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){
            add(n,Manifest.permission.READ_MEDIA_IMAGES);
            add(n,Manifest.permission.READ_MEDIA_VIDEO);
            add(n,Manifest.permission.READ_MEDIA_AUDIO);
        } else {
            add(n,Manifest.permission.READ_EXTERNAL_STORAGE);
            if(Build.VERSION.SDK_INT<=Build.VERSION_CODES.P)
                add(n,Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if(!n.isEmpty())ActivityCompat.requestPermissions(this,n.toArray(new String[0]),PERM_REQ);
    }
    private void add(List<String> l,String p){
        if(ContextCompat.checkSelfPermission(this,p)!=PackageManager.PERMISSION_GRANTED)l.add(p);}
    @Override public void onRequestPermissionsResult(int code,@NonNull String[] p,@NonNull int[] g){
        super.onRequestPermissionsResult(code,p,g);
        if(code==PERM_REQ){
            boolean ok=true; for(int gr:g)if(gr!=PackageManager.PERMISSION_GRANTED){ok=false;break;}
            if(ok)browseFragment.loadMedia();
            else Toast.makeText(this,"Storage permission required to browse media",Toast.LENGTH_LONG).show();
        }
    }
}
