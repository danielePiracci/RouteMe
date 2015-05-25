package com.teamrouteme.routeme.activity;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.parse.ParseUser;
import com.teamrouteme.routeme.R;
import com.teamrouteme.routeme.fragment.CercaItinerarioFragment;
import com.teamrouteme.routeme.fragment.ItinerariScaricatiFragment;
import com.teamrouteme.routeme.fragment.ListaDesideriFragment;
import com.teamrouteme.routeme.fragment.CreaItinerarioFragment;
import com.teamrouteme.routeme.fragment.MieiItinerariFragment;
import com.teamrouteme.routeme.fragment.ProfiloFragment;
import com.teamrouteme.routeme.utility.JsonReader;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialAccount;
import it.neokree.materialnavigationdrawer.elements.MaterialSection;
import it.neokree.materialnavigationdrawer.elements.listeners.MaterialSectionListener;


public class HomeActivity extends MaterialNavigationDrawer {

    private static final int LOGIN_REQUEST = 0;

    MaterialAccount account;
    private ParseUser currentUser;
    private Bitmap profilo;
    private Bitmap copertina;
    private View mDecorView;

    @Override
    public void init(Bundle bundle) {

        mDecorView = getWindow().getDecorView();

        currentUser = ParseUser.getCurrentUser();

        //Caricare immagini copertina e profilo se loggati con facebook

        //is a facebook login
        if(currentUser.get("authData")!=null){
            HashMap<Object,Object> fb1= ( HashMap<Object,Object>)currentUser.get("authData");
            HashMap<Object,Object> fb2= ( HashMap<Object,Object>)fb1.get("facebook");

            String fbId= (String)fb2.get("id");
            String access_token = (String) fb2.get("access_token");


            getProfileImage(fbId,access_token);


        }


        profilo = BitmapFactory.decodeResource(getResources(), R.drawable.com_parse_ui_app_logo);
        copertina = BitmapFactory.decodeResource(getResources(), R.drawable.copertina);

        account = new MaterialAccount(this.getResources(),ParseUser.getCurrentUser().getEmail(),currentUser.getString("name"),profilo,copertina);
        this.addAccount(account);



        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        this.addSection(newSection("Cerca Itinerario", R.drawable.search, new CercaItinerarioFragment()));
        this.addSection(newSection("Crea Itinerario", R.drawable.marker, new CreaItinerarioFragment()));
        this.addDivisor();
        this.addSection(newSection("Miei Itinerari", R.drawable.list, new MieiItinerariFragment()));
        this.addSection(newSection("Itinerari Scaricati", R.drawable.download_icon, new ItinerariScaricatiFragment()));
        this.addSection(newSection("Lista Desideri", R.drawable.whishlist, new ListaDesideriFragment()));
        this.addDivisor();
        this.addSection(newSection("Profilo", R.drawable.profilo, new ProfiloFragment()));
        this.addDivisor();
        this.addSection(newSection("Logout", R.drawable.logout, new MaterialSectionListener() {
            @Override
            public void onClick(MaterialSection materialSection) {

                ParseUser.logOut();

                Intent intent = new Intent(HomeActivity.this, SplashActivity.class);

                startActivity(intent);

                HomeActivity.this.finish();
            }
        }));


        enableToolbarElevation();

    }

    /*public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
            mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }*/

    //Controlla se c'è connessione ad Internet
    public boolean isConnected(){
        ConnectivityManager conMgr = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo i = conMgr.getActiveNetworkInfo();
        if (i == null)
            return false;
        if (!i.isConnected())
            return false;
        if (!i.isAvailable())
            return false;
        return true;
    }

    public void getProfileImage(final String userFacebookId, final String access_token){

        new AsyncTask<Void, Void, Bitmap[]>()
        {
            @Override
            protected Bitmap[] doInBackground(Void... params)
            {

                Bitmap [] bitmaps = new Bitmap[2];
                // safety check
                if (userFacebookId == null)
                    return null;

                String urlProfile = String.format(
                        "https://graph.facebook.com/%s/picture?type=large",
                        userFacebookId);


                String urlCover = String.format(
                        "https://graph.facebook.com/%s?fields=cover&access_token=%s",
                        userFacebookId,access_token);


                // you'll need to wrap the two method calls
                // which follow in try-catch-finally blocks
                // and remember to close your input stream

                InputStream inputStream = null;
                try {
                    inputStream = new URL(urlProfile).openStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }


                bitmaps[0] = BitmapFactory.decodeStream(inputStream);


                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    String urlFinalCover =  (String) ((JSONObject)((JSONObject)JsonReader.readJsonFromUrl(urlCover)).get("cover")).get("source");

                    try {
                        inputStream = new URL(urlFinalCover).openStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                    bitmaps[1] = BitmapFactory.decodeStream(inputStream);

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }


                return bitmaps;
            }

            @Override
            protected void onPostExecute(Bitmap[] bitmaps)
            {
                // safety check
                if (bitmaps != null
                        && !isChangingConfigurations()
                        && !isFinishing()){

                    account.setPhoto(bitmaps[0]);
                    account.setBackground(bitmaps[1]);

                }
                // do what you need to do with the bitmap :)
            }
        }.execute();


    }

}

