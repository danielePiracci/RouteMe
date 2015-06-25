package com.teamrouteme.routeme.fragment;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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
 * Created by nicolopergola on 24/06/15.
 */
public class InformazioniBusDialog extends DialogFragment {

    private int markerPosition;
    private String fermata;
    private TextView fermatatw;
    private ListView linee;
    private ArrayAdapter<String> adapter;
    private boolean connectionError;

    public InformazioniBusDialog() {

    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        final View view = inflater.inflate(R.layout.fragment_informazionibus_dialog, container);

        Bundle b = getArguments();
        markerPosition = b.getInt("markerPosition");
        fermata = b.getString("fermata");

        connectionError = false;
        
        fermatatw = (TextView)view.findViewById(R.id.fermataInfo);
        fermatatw.setText(fermata);

        linee = (ListView)view.findViewById(R.id.listView_linee);

        ParseQuery<ParseObject> query = ParseQuery.getQuery("info_linea");
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

        return view;

    }
}

