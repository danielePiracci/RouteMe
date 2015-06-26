package com.teamrouteme.routeme.fragment;

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ogaclejapan.arclayout.ArcLayout;
import com.parse.FindCallback;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.teamrouteme.routeme.R;
import com.teamrouteme.routeme.utility.ArcLayoutButton;
import com.teamrouteme.routeme.utility.MyOnOpenTagsListener;
import com.teamrouteme.routeme.utility.ClipRevealFrame;
import com.yahoo.mobile.client.android.util.RangeSeekBar;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by massimo299 on 14/05/15.
 */
public class UploadItinerarioDialog extends DialogFragment{

    private EditText nomeItinerarioEditText, descrizioneItinerarioEditText, cittaItinerarioEditText;
    private Button btn_confermaItinerario;

    private RangeSeekBar<Integer> rangeSeekBar;
    private LinearLayout seekbar_placeholder_layout;

    private View rootLayout;
    private ClipRevealFrame menuLayout;
    private ArcLayout arcLayout;
    private Button centerItem;
    private  ArrayList<String> listTags;

    public UploadItinerarioDialog() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_upload_itinerario_dialog, container);

        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        cittaItinerarioEditText = (EditText) view.findViewById(R.id.editText_citta_itinerario);
        nomeItinerarioEditText = (EditText) view.findViewById(R.id.editText_nome_itinerario);
        descrizioneItinerarioEditText = (EditText) view.findViewById(R.id.editText_descrizione_itinerario);

        rootLayout = view.findViewById(R.id.layout_dialog_creazione_itinerario);
        menuLayout = (ClipRevealFrame) view.findViewById(R.id.menu_layout);
        arcLayout = (ArcLayout) view.findViewById(R.id.arc_layout);
        centerItem = (Button) view.findViewById(R.id.center_item);
        listTags = new ArrayList<>();

        seekbar_placeholder_layout = (LinearLayout) view.findViewById(R.id.seekbar_placeholder);

        btn_confermaItinerario = (Button)view.findViewById(R.id.btn_conferma_itinerario);


        // Setup the new range seek bar
        rangeSeekBar = new RangeSeekBar<Integer>(getActivity());
        // Set the range
        rangeSeekBar.setRangeValues(1, 10);
        rangeSeekBar.setSelectedMinValue(1);
        rangeSeekBar.setSelectedMaxValue(10);


        // Add to layout
        seekbar_placeholder_layout.addView(rangeSeekBar);


        final ArrayList<Integer> colours = getTagsColours();

        ParseQuery<ParseObject> query = ParseQuery.getQuery("tags");


        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, com.parse.ParseException e) {
                if (e == null) {
                    //ok

                    ArrayList<String> tags = new ArrayList<String>();

                    String tagName;

                    for (ParseObject parseObjetc : list) {

                        tagName = (String) parseObjetc.get("tagname");
                        tags.add(tagName);

                    }
                    //Istanzia dinamicamente i bottoni per i tag e gli assegna il listener
                    for (int i = 0; i < tags.size(); i++) {
                        ArcLayoutButton b = new ArcLayoutButton(getActivity().getApplicationContext());
                        b.setButtonAttributes(tags.get(i), colours.get(i % colours.size()));
                        b.setOnTouchListener(new tagButtonOnClick());
                        if (listTags.contains(b.getText()))
                            b.setPressed(true);
                        arcLayout.addView(b);
                    }


                } else {
                    //error
                    Toast.makeText(getActivity().getBaseContext(), "Nessun tag trovato. Errore di connessione", Toast.LENGTH_SHORT).show();
                    Log.d("Tags", "Error: " + e.getMessage());
                }


            }
        });


        //Setta il listener per il bottone di apertura dei tag
        ArrayList<View> vL = new ArrayList<View>();
        vL.add(cittaItinerarioEditText);
        vL.add(nomeItinerarioEditText);
        vL.add(descrizioneItinerarioEditText);
        vL.add(btn_confermaItinerario);
        vL.add(rangeSeekBar);
        view.findViewById(R.id.open_tags).setOnClickListener(new MyOnOpenTagsListener(rootLayout, menuLayout, arcLayout, centerItem, vL));

        //Listener per il bottone di conferma
        btn_confermaItinerario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String result = checkFields();
                if (result == null) {

                    File root = Environment.getExternalStorageDirectory();
                    File outDir = new File(root.getAbsolutePath() + File.separator + "routeme");


                    File outputFile = new File(outDir,"tappe_tmp.txt");
                    if(outputFile!=null){
                        if(outputFile.exists())
                            outputFile.delete();
                    }

                    Intent i = new Intent();
                    i.putExtra("citta_itinerario", cittaItinerarioEditText.getText().toString());;
                    i.putExtra("tags_itinerario", listTags.toArray(new String [listTags.size()]));
                    i.putExtra("nome_itinerario", nomeItinerarioEditText.getText().toString());
                    i.putExtra("descrizione_itinerario", descrizioneItinerarioEditText.getText().toString());
                    i.putExtra("range_min_itinerario", rangeSeekBar.getSelectedMinValue());
                    i.putExtra("range_max_itinerario", rangeSeekBar.getSelectedMaxValue());


                    getTargetFragment().onActivityResult(getTargetRequestCode(), getTargetRequestCode(), i);
                    closeKeyboard(getActivity(), nomeItinerarioEditText.getWindowToken());
                    closeKeyboard(getActivity(), descrizioneItinerarioEditText.getWindowToken());
                    getDialog().dismiss();
                } else
                    Toast.makeText(getActivity().getBaseContext(), result, Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    public static void closeKeyboard(Context c, IBinder windowToken) {
        InputMethodManager mgr = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(windowToken, 0);
    }

    private String checkFields(){
        if(nomeItinerarioEditText.getText().toString().equals(""))
            return "Inserisci un nome per l'itinerario";
        else if(cittaItinerarioEditText.getText().toString().equals(""))
            return "Inserisci una città per l'itinerario";
        else if(descrizioneItinerarioEditText.getText().toString().equals(""))
            return "Inserisci una descrizione per l'itinerario";
        else if(listTags.size()==0)
            return "Inserisci dei tag per l'itinerario";
        return null;
    }

    //Metodo che recupera tutti i drawable per i bottoni con i tag
    private ArrayList<Integer> getTagsColours(){
        Field[] fields = R.drawable.class.getFields();
        ArrayList<Integer> drawables = new ArrayList<Integer>();
        for (Field field : fields) {
            // Take only those with name starting with "fr"
            if (field.getName().startsWith("tumblr_")) {
                try {
                    drawables.add(field.getInt(null));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        return drawables;
    }

    //Listener per i bottoni con i tag
    private class tagButtonOnClick implements View.OnTouchListener{
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN: v.setPressed(!v.isPressed());
                    if (v instanceof Button) {
                        String tagName = ((Button) v).getText().toString();
                        if(!listTags.contains(tagName)){
                            listTags.add(tagName);
                            Toast.makeText(getActivity().getBaseContext(), "Aggiunto "+((Button) v).getText(), Toast.LENGTH_SHORT).show();
                        } else {
                            listTags.remove(tagName);
                            Toast.makeText(getActivity().getBaseContext(), "Rimosso "+((Button) v).getText(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
            }
            return  true;
        }
    }
}
