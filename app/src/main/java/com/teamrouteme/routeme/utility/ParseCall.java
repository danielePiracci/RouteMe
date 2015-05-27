package com.teamrouteme.routeme.utility;

import android.app.ProgressDialog;
import android.util.Log;

import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.teamrouteme.routeme.bean.Itinerario;
import com.teamrouteme.routeme.bean.Tappa;

import org.json.JSONArray;

import java.util.List;

/**
 * Created by ginofarisano on 15/05/15.
 */
public class ParseCall {

    ParseUser user;

    public ParseCall(){

        user= ParseUser.getCurrentUser();
    }


    public void saveDataToParse(String citta, String [] tags, String nome, String descrizione, int min, int max, Itinerario itinerario) {

        ParseObject toUploadItinerario = new ParseObject("itinerario");

        toUploadItinerario.put("user", user);

        toUploadItinerario.put("citta", citta);

        JSONArray jsonTags = new JSONArray();

        for(int i=0;i<tags.length;i++){
            jsonTags.put(tags[i]);
        }

        toUploadItinerario.put("tags",jsonTags);

        toUploadItinerario.put("nome",nome);
        toUploadItinerario.put("descrizione",descrizione);
        toUploadItinerario.put("durata_min",min);
        toUploadItinerario.put("durata_max",max);



        Tappa tappa;
        ParseObject tappaItinerario;
        ParseGeoPoint point;
        JSONArray jsonTappe = new JSONArray();

        for(int i=0;i<itinerario.getTappeSize();i++){

            tappa = itinerario.getTappa(i);

            tappaItinerario = new ParseObject("tappa");
            tappaItinerario.put("nome",tappa.getNome());
            tappaItinerario.put("descrizione",tappa.getDescrizione());
            point = new ParseGeoPoint(tappa.getMarker().getPosition().latitude, tappa.getMarker().getPosition().longitude);
            tappaItinerario.put("location", point);
            jsonTappe.put(tappaItinerario);
        }

        toUploadItinerario.put("tappe",jsonTappe);

        toUploadItinerario.saveInBackground();


    }


    public void addWishList(String idItinerario) {

        ParseObject toAddWishList = new ParseObject("lista_desideri");

        toAddWishList.put("user", user);

        toAddWishList.put("idItinerario", idItinerario);

        toAddWishList.saveInBackground();



    }

    public void buyRoute(final String idItinerario, final ProgressDialog dialog) {


        ParseObject toAddWishList = new ParseObject("itinerari_acquistati");

        toAddWishList.put("user", user);

        toAddWishList.put("idItinerario", idItinerario);

        toAddWishList.saveInBackground(new SaveCallback() {
            public void done(ParseException e) {
                if (e == null) {
                    ParseQuery query = ParseQuery.getQuery("lista_desideri");

                    query = query.whereEqualTo("idItinerario", idItinerario);

                    query.findInBackground(new FindCallback<ParseObject>() {

                        @Override
                        public void done(List<ParseObject> list, com.parse.ParseException e) {

                            if (e == null) {
                                if (list.size() != 0) {
                                    list.get(0).deleteInBackground(new DeleteCallback() {
                                        public void done(ParseException e) {
                                            if (e == null) {
                                                dialog.hide();
                                            } else {
                                                Log.d("ParseCall", "Error: " + e.getMessage());
                                            }
                                        }
                                    });
                                }
                            } else {
                                Log.d("AnteprimaItinerario", "Error: " + e.getMessage());
                            }
                        }

                    });
                } else {
                    Log.d("ParseCall", "Error: " + e.getMessage());
                }
            }
        });

    }

    public void buyRoute(final String idItinerario, final ProgressDialog dialog, final ParseObject listaDesideriObject) {

        ParseObject toAddWishList = new ParseObject("itinerari_acquistati");

        toAddWishList.put("user", user);

        toAddWishList.put("idItinerario", idItinerario);

        toAddWishList.saveInBackground(new SaveCallback() {
            public void done(ParseException e) {
                if (e == null) {
                    if(listaDesideriObject != null)
                        listaDesideriObject.deleteInBackground(new DeleteCallback() {
                            public void done(ParseException e) {
                                if (e == null) {
                                    dialog.hide();
                                } else {
                                    Log.d("ParseCall", "Error: " + e.getMessage());
                                }
                            }
                        });
                    else
                        dialog.hide();
                } else {
                    Log.d("ParseCall", "Error: " + e.getMessage());
                }
            }
        });
    }
}