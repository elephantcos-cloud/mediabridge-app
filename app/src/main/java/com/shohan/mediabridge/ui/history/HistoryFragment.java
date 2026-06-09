package com.shohan.mediabridge.ui.history;
import android.content.Intent;import android.net.Uri;import android.os.Bundle;
import android.os.Handler;import android.os.Looper;import android.view.*;
import androidx.annotation.*;import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.shohan.mediabridge.databinding.FragmentHistoryBinding;
import com.shohan.mediabridge.db.*;import java.io.File;
import java.util.*;import java.util.concurrent.*;
public class HistoryFragment extends Fragment {
    private FragmentHistoryBinding b;
    private HistoryAdapter adp;
    private final ExecutorService pool=Executors.newSingleThreadExecutor();
    private final Handler ui=new Handler(Looper.getMainLooper());
    @Override public View onCreateView(@NonNull LayoutInflater i,ViewGroup p,Bundle s){
        b=FragmentHistoryBinding.inflate(i,p,false);return b.getRoot();}
    @Override public void onViewCreated(@NonNull View v,@Nullable Bundle s){
        super.onViewCreated(v,s);
        adp=new HistoryAdapter(requireContext(),new ArrayList<>(),this::action);
        b.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recycler.setAdapter(adp);
        b.btnClearAll.setOnClickListener(x->confirmClear());
        load();
    }
    private void load(){
        pool.execute(()->{
            List<ConversionRecord> list=AppDatabase.get(requireContext()).conversionDao().getAll();
            ui.post(()->{if(b==null)return;
                adp.setItems(list);
                b.emptyView.setVisibility(list.isEmpty()?View.VISIBLE:View.GONE);
                b.btnClearAll.setEnabled(!list.isEmpty());});
        });
    }
    private void action(ConversionRecord rec,String act){
        if("open".equals(act)){
            if(rec.outputPath!=null&&new File(rec.outputPath).exists()){
                Intent i=new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(Uri.fromFile(new File(rec.outputPath)),"*/*");
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try{startActivity(i);}catch(Exception ignored){}}}
        else if("delete".equals(act)){
            pool.execute(()->{AppDatabase.get(requireContext()).conversionDao().deleteById(rec.id);
                ui.post(this::load);});}
    }
    private void confirmClear(){
        new AlertDialog.Builder(requireContext()).setTitle("Clear All History")
            .setMessage("Delete all records? Output files will NOT be deleted.")
            .setPositiveButton("Clear",(d,w)->pool.execute(()->{
                AppDatabase.get(requireContext()).conversionDao().deleteAll();
                ui.post(this::load);}))
            .setNegativeButton("Cancel",null).show();}
    @Override public void onDestroyView(){super.onDestroyView();b=null;}
}
