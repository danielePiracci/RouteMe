package com.teamrouteme.routeme.fragment;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.teamrouteme.routeme.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by daniele on 03/07/15.
 */
public class TabInformazioniDialog extends DialogFragment {

    TabHost tabs;

    private View view;
    private int markerPosition;
    private String fermata;
    private TextView fermatatw;
    private ListView linee;
    private ArrayAdapter<String> adapter;
    private boolean connectionError;

    private ArrayList<String> ALF;
    private ArrayAdapter adapter2;
    private String nomeTappa;
    private int cntLista;

    public TabInformazioniDialog() {

    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);


        view=inflater.inflate(R.layout.tab_informazioni_dialog, container);


        Bundle b = getArguments();
        markerPosition = b.getInt("markerPosition");
        fermata = b.getString("fermata");

        nomeTappa = b.getString("nextTappa");
        ALF = b.getStringArrayList("ALF");

        mostraInfoBus(markerPosition,fermata);
        mostraInfoProssimaTappa(markerPosition, fermata);
        // Add tabs
        tabs=(TabHost)view.findViewById(R.id.tabHost);
        tabs.setup();

        TabHost.TabSpec tabpage1 = tabs.newTabSpec("one");
        tabpage1.setContent(R.id.shareIndividual);
        LinearLayout ll = (LinearLayout) view.findViewById(R.id.shareIndividual);
        tabpage1.setIndicator("Orari bus", getResources().getDrawable(R.drawable.ic_launcher));
        tabs.addTab(tabpage1);


        TabHost.TabSpec tabpage2 = tabs.newTabSpec("due");
        tabpage2.setContent(R.id.shareGroup);
        LinearLayout ll2 = (LinearLayout) view.findViewById(R.id.shareGroup);
        tabpage2.setIndicator("Per la prossima tappa", getResources().getDrawable(R.drawable.ic_launcher));
        tabs.addTab(tabpage2);

        for(int i=0;i<tabs.getTabWidget().getChildCount();i++)
        {
            TextView tv = (TextView) tabs.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
            tv.setTextColor(getResources().getColor(R.color.testo));
        }

        return view;

    }

    private void mostraInfoProssimaTappa(int markerPosition, String fermata) {
        ListView lv = (ListView) view.findViewById(R.id.listView_info);

        TextView tv = (TextView) view.findViewById(R.id.titoloInfo);
        tv.setText("Prossima Tappa: "+ nomeTappa);



        if(ALF.size()==0)
            ALF.add("Nessuna fermata disponibile per raggiungere la prossima tappa");
        else{
            Set<String> ALFnoDuplicate = new HashSet<String>(ALF);
            ALF = new ArrayList<String>(ALFnoDuplicate);
        }




        adapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_expandable_list_item_1,ALF);
        lv.setAdapter(adapter);
    }

    private void mostraInfoBus(int markerPosition, final String fermata) {

        connectionError = false;

        fermatatw = (TextView)view.findViewById(R.id.fermataInfo);
        fermatatw.setText(fermata);

        linee = (ListView)view.findViewById(R.id.listView_linee);

        ParseQuery<ParseObject> query = ParseQuery.getQuery("info_linea_nuovo");
        query.whereContains("fermata", fermata);

        final ProgressDialog dialog = ProgressDialog.show(getActivity(), "",
                "Caricamento in corso...", true);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {

                if (e == null) {
                    ArrayList<String> allLinee = new ArrayList<String>();
                    fermatatw.setVisibility(View.VISIBLE);
                    for (ParseObject linea : list) {
                        allLinee.add(linea.getString("linea_bus"));
                    }

                    Set<String> hs = new HashSet<>();
                    hs.addAll(allLinee);
                    allLinee.clear();
                    allLinee.addAll(hs);

                    adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, allLinee);
                    linee.setAdapter(adapter);
                    adapter.notifyDataSetChanged();

                    linee.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            FragmentManager fm = getFragmentManager();
                            final OrariBusDialog orariBusDialog = new OrariBusDialog();
                            Bundle b = new Bundle();
                            b.putString("fermata", fermata);
                            b.putString("linea", linee.getItemAtPosition(position).toString());
                            orariBusDialog.setArguments(b);
                            orariBusDialog.show(fm, "fragment_info_linea_dialog");
                        }
                    });
                } else {
                    dialog.hide();
                    if(!connectionError) {
                        connectionError = true;
                        Toast.makeText(getActivity().getBaseContext(), "Errore di connessione. Riprova", Toast.LENGTH_SHORT).show();
                    }
                }
                dialog.hide();
            }
        });
    }
}
