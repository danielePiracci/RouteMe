package com.teamrouteme.routeme.fragment;

import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.teamrouteme.routeme.R;

import java.util.List;

/**
 * Created by nicolopergola on 24/06/15.
 */
public class OrariBusDialog extends DialogFragment {

    private String linea;
    private String fermata;
    private TextView fermatatw;
    private ListView linee;
    private ArrayAdapter<String> adapter;
    private boolean connectionError;

    public OrariBusDialog() {

    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        final View view = inflater.inflate(R.layout.fragment_info_linea_dialog, container);

        connectionError = false;

        Bundle b = getArguments();
        linea = b.getString("linea");
        fermata = b.getString("fermata");

        ParseQuery query = ParseQuery.getQuery("info_linea_nuovo");
        query = query.whereContains("fermata", fermata);
        query.whereContains("linea_bus", linea);

        final ProgressDialog dialog = ProgressDialog.show(getActivity(), "",
                "Caricamento in corso...", true);


        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                if (e == null) {
                    for (ParseObject parseObject : list) {
                        String linea = parseObject.getString("linea_bus");
                        TextView lineaTw = (TextView) view.findViewById(R.id.linea);
                        lineaTw.setText(linea);
                        lineaTw.setVisibility(View.VISIBLE);

                        String tipo_linea = parseObject.getString("tipo_linea");
                        if (tipo_linea.equals("feriale")) {
                            TextView titolo = (TextView) view.findViewById(R.id.ferialeTitle);
                            titolo.setVisibility(View.VISIBLE);

                            String a_r = parseObject.getString("a_r");
                            String s = parseObject.getString("orari");
                            String [] tmp = s.split(" ");

                            for(int i = 0;i<tmp.length;i++){
                                if(tmp[i].equals("|"))
                                    tmp[i] = "";
                            }

                            String s2 = "";
                            for(String string : tmp){
                                s2 = s2 + " " + string;
                            }

                            s = s2;

                            s = s.trim();

                            String orari = s.replace(" "," - ");

                            if (a_r.equals("andata")) {
                                TextView feriale = (TextView) view.findViewById(R.id.andataFerialeTitle);
                                feriale.setVisibility(View.VISIBLE);
                                TextView orariTw = (TextView) view.findViewById(R.id.andataOrariFeriale);
                                orariTw.setText(orari);
                                orariTw.setVisibility(View.VISIBLE);
                            }
                            if(a_r.equals("ritorno")) {
                                TextView orariTw = (TextView) view.findViewById(R.id.ritornoFerialeOrari);
                                orariTw.setText(orari);
                                TextView feriale = (TextView) view.findViewById(R.id.ritornoFerialeTitle);
                                feriale.setVisibility(View.VISIBLE);
                                orariTw.setVisibility(View.VISIBLE);
                            }
                        }
                        if(tipo_linea.equals("festivo")) {
                            TextView titolo = (TextView) view.findViewById(R.id.festivoTitle);
                            titolo.setVisibility(View.VISIBLE);
                            String a_r = parseObject.getString("a_r");
                            String s = parseObject.getString("orari");
                            s = s.trim();
                            String orari = s.replace(" ", "-");

                            if (a_r.equals("andata")) {
                                TextView festivo = (TextView) view.findViewById(R.id.andataFestiviTitle);
                                festivo.setVisibility(View.VISIBLE);
                                TextView orariTw = (TextView) view.findViewById(R.id.andataOrariFestivi);
                                orariTw.setText(orari);
                                orariTw.setVisibility(View.VISIBLE);
                            }
                            if(a_r.equals("ritorno")) {
                                TextView orariTw = (TextView) view.findViewById(R.id.ritornoOrariFestivo);
                                orariTw.setText(orari);
                                TextView festivo = (TextView) view.findViewById(R.id.ritornoFestivoTitle);
                                festivo.setVisibility(View.VISIBLE);
                                orariTw.setVisibility(View.VISIBLE);
                            }
                        }
                    }


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



