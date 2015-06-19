package com.teamrouteme.routeme.fragment;

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.teamrouteme.routeme.R;

/**
 * Created by massimo299 on 14/05/15.
 */
public class InserimentoTappaDialog extends DialogFragment {

    private EditText nomeTappaEditText, descrizioneTappaEditText;
    private String nomeTappa, descrizioneTappa;
    private int markerPosition;
    private boolean isModifica = false;

    public InserimentoTappaDialog() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_inserimento_tappa_dialog, container);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        Bundle b = getArguments();
        if(b != null) {
            isModifica = true;
            nomeTappa = b.getString("nomeTappa");
            descrizioneTappa = b.getString("descrizioneTappa");
            markerPosition = b.getInt("markerPosition");
        }

        nomeTappaEditText = (EditText) view.findViewById(R.id.editText_nome_tappa);
        descrizioneTappaEditText = (EditText) view.findViewById(R.id.editText_descrizione_tappa);

        if(nomeTappa != null){
            nomeTappaEditText.setText(nomeTappa);
            descrizioneTappaEditText.setText(descrizioneTappa);
        }

        Button btn_confermaTappa = (Button)view.findViewById(R.id.btn_conferma_tappa);
        btn_confermaTappa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String result = checkFields();
                if (result == null) {
                    Intent i = new Intent();
                    i.putExtra("nome_tappa", nomeTappaEditText.getText().toString());
                    i.putExtra("descrizione_tappa", descrizioneTappaEditText.getText().toString());

                    if(isModifica) {
                        i.putExtra("markerPosition", markerPosition);
                        getTargetFragment().onActivityResult(5, 5, i);
                    }
                    else
                        getTargetFragment().onActivityResult(getTargetRequestCode(), getTargetRequestCode(), i);
                    closeKeyboard(getActivity(), nomeTappaEditText.getWindowToken());
                    closeKeyboard(getActivity(), descrizioneTappaEditText.getWindowToken());
                    getDialog().dismiss();
                }
                else
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
        if(nomeTappaEditText.getText().toString().equals("")){
            return "Inserisci un nome per la tappa";
        }
        else if(descrizioneTappaEditText.getText().toString().equals(""))
            return "Inserisci una descrizione per la tappa";

        return null;
    }
}