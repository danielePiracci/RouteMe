package com.teamrouteme.routeme.fragment;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.teamrouteme.routeme.R;
import com.teamrouteme.routeme.bean.FermataBus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by daniele on 03/07/15.
 */
public class InformazioniNextTappaDialog extends DialogFragment {

    private ArrayList<String> ALF;
    private ArrayAdapter adapter;
    private String nomeTappa;
    private int cntLista;
    public InformazioniNextTappaDialog() {

    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        final View view = inflater.inflate(R.layout.fragment_informazioni_next_tappa_dialog, container);
        Bundle b = getArguments();
        nomeTappa = b.getString("nextTappa");
        ALF = b.getStringArrayList("ALF");

        //cntLista = b.getInt("cntLista");

       // ArrayList<String> AL = new ArrayList<>();
/*
        for(int i=0;i<cntLista;i++){
            String tmp = b.getString("Item"+cntLista);
            AL.add(tmp);
        }

*/

        //ALF = b.getStringArrayList("ALF");



        ListView lv = (ListView) view.findViewById(R.id.listView_infoLinea);

        TextView tv = (TextView) view.findViewById(R.id.titoloInfo);
        tv.setText("Prossima Tappa: "+ nomeTappa);

        //   ArrayList<String> AL = new ArrayList<>();
/*
        if(ALF.size()==0)
            AL.add("Nessuna fermata disponibile per raggiungere la prossima tappa");
        else {
            for (String f : ALF) {
                AL.add(f);
            }
            Set<String> hs = new HashSet<>();
            hs.addAll(AL);
            AL.clear();
            AL.addAll(hs);
        }*/


        if(ALF.size()==0)
            ALF.add("Nessuna fermata disponibile per raggiungere la prossima tappa");


        ALF.add("Nessuna fermata disponibile per raggiungere la prossima tappa");

        adapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_expandable_list_item_1,ALF);
        lv.setAdapter(adapter);


        return view;

    }
}
