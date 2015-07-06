package com.teamrouteme.routeme.fragment;

import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.Color;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.teamrouteme.routeme.R;
import com.teamrouteme.routeme.bean.FermataBus;
import com.teamrouteme.routeme.bean.Itinerario;
import com.teamrouteme.routeme.bean.Tappa;
import com.teamrouteme.routeme.utility.DirectionsJSONParser;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;

/**
 * Created by massimo299 on 22/05/15.
 */
public class VisualizzaItinerarioFragment extends Fragment implements LocationListener{

    private final String TAG = "VisualizzaItinerarioLog";
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private static final LatLng ITALY = new LatLng(42.5, 12.5);
    private LocationManager mLocationManager;
    private String provider;
    private boolean locationUpdatesRequested;
    private Itinerario itinerario;
    private static View view;
    private Button btnIndietro;
    private boolean itinerariScaricati = false;
    private ArrayList<Marker> markersBus = new ArrayList<Marker>();
    private ArrayList<Marker> markersTappe = new ArrayList<Marker>();
    private HashSet<FermataBus> hsFBcurrent;
    private HashSet<FermataBus> hsFBnext;
    private ArrayList<String> ALlineainComune;
    private boolean flagAndata = false;
    private String tmpOrarioCurrent="";
    private String tmpOrarioNext="";
    private int durata, cntFor, cntLista;
    private String currentFermata ="";
    private String nextFermata ="";
    // private InformazioniNextTappaDialog informazioniNextTappaDialog;
    private Bundle b;
    private boolean flagStop = false;


    public VisualizzaItinerarioFragment(){
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle bundle = getArguments();

        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        }
        try {
            view = inflater.inflate(R.layout.fragment_visualizza_itinerario, container, false);
        } catch (InflateException e) {
            /* map is already there, just return view as it is */
        }

        setUpMapOnLastKnownLocation(false);

        mMap.clear();

        if(bundle != null) {
            itinerario = (Itinerario) bundle.get("itinerario");
            itinerariScaricati = bundle.getBoolean("itinerariScaricati");
            Log.d("","Nome itinerario ricevuto: "+ itinerario.getNome());
            Log.d("","Descrizione itinerario ricevuto: "+ itinerario.getDescrizione());
            Log.d("","Citta itinerario ricevuto: "+ itinerario.getCitta());
            Log.d("","Id itinerario ricevuto: "+ itinerario.getId());
            Log.d("","Durata Min itinerario ricevuto: "+ itinerario.getDurataMin());
            Log.d("","Durata Max itinerario ricevuto: "+ itinerario.getDurataMax());
            Log.d("","Tags itinerario ricevuto: "+ itinerario.getTags());
            Log.d("","Numero Tappe itinerario ricevuto: "+ itinerario.getTappe().size());
        }

        // Disegna i marker per ogni tappa sulla mappa
        ArrayList<Tappa> tappe = itinerario.getTappe();
        for(int i=0;i<tappe.size();i++) {
            MarkerOptions mO = createMarkerFromTappa(tappe.get(i));
            Marker m = mMap.addMarker(mO);
            markersTappe.add(m);
        }

        getFermateBus(tappe,mMap);

        // Disegna il percorso tra i marker, se ce ne sono almeno 2
        for(int i=0;i<tappe.size()-1;i++){
            Tappa a = tappe.get(i);
            Tappa b = tappe.get(i+1);
            LatLng aLL = new LatLng(a.getCoordinate().getLatitude(), a.getCoordinate().getLongitude());
            LatLng bLL = new LatLng(b.getCoordinate().getLatitude(), b.getCoordinate().getLongitude());
            drawFromAtoB(aLL, bLL);
        }

        btnIndietro = (Button) view.findViewById(R.id.btn_indietro);
        btnIndietro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(itinerariScaricati){
                    Fragment anteprimaItinerariScaricatiFragment = new AnteprimaItinerariAcquistatiFragment();
                    b = new Bundle();
                    b.putParcelable("itinerario", itinerario);
                    anteprimaItinerariScaricatiFragment.setArguments(b);
                    // Set new fragment on screen
                    MaterialNavigationDrawer home = (MaterialNavigationDrawer) getActivity();
                    home.setFragment(anteprimaItinerariScaricatiFragment, "Anteprima Itinerario");
                }
                else {
                    Fragment anteprimaMieiItinerariFragment = new AnteprimaMieiItinerariFragment();
                    b = new Bundle();
                    b.putParcelable("itinerario", itinerario);
                    anteprimaMieiItinerariFragment.setArguments(b);
                    // Set new fragment on screen
                    MaterialNavigationDrawer home = (MaterialNavigationDrawer) getActivity();
                    home.setFragment(anteprimaMieiItinerariFragment, "Anteprima Itinerario");
                }
            }
        });

        return view;
    }


    public void getFermateBus(ArrayList<Tappa> tappe, final GoogleMap mMap){

        recursiveGetFermate(tappe,0);
    }

    private void recursiveGetFermate(final ArrayList<Tappa> tappe, final int index ){

        if(tappe.size()==index){

        } else {
            ParseQuery<ParseObject> info_linea = ParseQuery.getQuery("info_linea_nuovo");

            ParseGeoPoint tappaLocation = tappe.get(index).getCoordinate();
            info_linea.whereWithinKilometers("geo_point", tappaLocation, 1);

            info_linea.findInBackground(new FindCallback<ParseObject>() {
                public void done(List<ParseObject> objects, ParseException e) {

                    MarkerOptions markerOptions;

                    if (e == null) {
                        for(ParseObject parseObject : objects){
                            markerOptions = createMarkerBus(parseObject);
                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_bus));
                            Marker marker = mMap.addMarker(markerOptions);
                            markersBus.add(marker);


                        }
                        recursiveGetFermate(tappe,index+1);
                        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {

                            @Override
                            public void onMapLongClick(LatLng latLng) {
                                for (int i = 0; i < markersBus.size(); i++) {
                                    Marker marker = markersBus.get(i);
                                    if (Math.abs(marker.getPosition().latitude - latLng.latitude) < 0.0009 && Math.abs(marker.getPosition().longitude - latLng.longitude) < 0.0009) {
                                        showInformazioniBusDialog(i, marker.getTitle());
                                        Log.d(TAG, "Trovato marker tappa " + marker.getTitle());
                                        break;
                                    }
                                }


                                //long press per le fermate vicine
                                for (int i = 0; i < markersTappe.size(); i++) {
                                    Marker marker = markersTappe.get(i);
                                    if (Math.abs(marker.getPosition().latitude - latLng.latitude) < 0.0009 && Math.abs(marker.getPosition().longitude - latLng.longitude) < 0.0009) {

                                        if(i+1<markersTappe.size()) {
                                            showInformazioniTappeBusDialog(markersTappe.get(i), markersTappe.get(i + 1));
                                            Log.d(TAG, "Trovato marker tappa " + marker.getTitle());
                                            break;
                                        }
                                        Toast.makeText(getActivity().getBaseContext(), "Questa è l'ultima tappa", Toast.LENGTH_SHORT).show();
                                    }
                                }

                            }
                        });

                    } else {
                        Log.e("Bus location",e.getMessage());
                    }
                }
            });
        }
    }

    // Metodo per posizionare la mappa sulla posizione attuale, se possibile, altrimenti la posiziona sull'Italia
    private void setUpMapOnLastKnownLocation(boolean locationUpdatesRequested){
        this.locationUpdatesRequested = locationUpdatesRequested;

        // Getting Google Play availability status
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity().getBaseContext());

        // Showing status
        if(status!= ConnectionResult.SUCCESS){ // Google Play Services are not available

            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, getActivity(), requestCode);
            dialog.show();

        }
        else {
            if (mMap == null)
                // Try to obtain the map from the SupportMapFragment.
                mMap = ((SupportMapFragment) ((FragmentActivity)getActivity()).getSupportFragmentManager().findFragmentById(R.id.map2))
                        .getMap();
            if (mMap != null) {
                // Enabling MyLocation Layer of Google Map (the blu dot for the location of the smartphone)
                mMap.setMyLocationEnabled(true);

                // Getting Current Location and setting the location updates if requested
                Location location = getLastKnownLocation();

                if (location != null) {
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 12);
                    mMap.animateCamera(cameraUpdate);
                }
                else {
                    Log.d(TAG, "Last known location is null, map setted on Italy");
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(ITALY, 5);
                    mMap.animateCamera(cameraUpdate);
                }
            }

        }
    }

    //Metodo che recupera la posiziona attuale, se disponibile
    private Location getLastKnownLocation() {
        mLocationManager = (LocationManager)getActivity().getApplicationContext().getSystemService(getActivity().LOCATION_SERVICE);
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location l = mLocationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            // Aggiorna la bestLocation solo se la nuova è sia più accurata che più recente
            if (bestLocation == null || (l.getAccuracy() < bestLocation.getAccuracy() && l.getTime() > bestLocation.getTime())) {
                // Found best last known location: %s", l);
                this.provider = provider;
                bestLocation = l;
            }
        }

        Log.d(TAG, "Best location " + bestLocation);

        if(locationUpdatesRequested && provider != null)
            mLocationManager.requestLocationUpdates(provider,5000,0,this);
        return bestLocation;
    }

    // Callback chiamata ogni tot ms (5000 come impsotato nel metodo sopra), che serve per aggiornare la posizione attuale in automatico
    @Override
    public void onLocationChanged(Location location) {

        Log.d(TAG, "OnLocationChanged called");

        location = getLastKnownLocation();

        // Codice per spostare la camera in funzione della posizione attuale
        /*CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15);
        mMap.animateCamera(cameraUpdate);*/
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    // Da un oggetto Tappa crea e restituisce il marker da disegnare sulla mappa
    private MarkerOptions createMarkerFromTappa(Tappa t) {
        MarkerOptions markerOptions = new MarkerOptions();

        // Getting latitude of the place
        double lat = t.getCoordinate().getLatitude();

        // Getting longitude of the place
        double lng = t.getCoordinate().getLongitude();

        // Getting name
        String name = t.getNome();

        // Getting description
        String description = t.getDescrizione();

        LatLng latLng = new LatLng(lat, lng);

        // Setting the position for the marker
        markerOptions.position(latLng);

        // Setting the title for the marker
        markerOptions.title(name);



        // Setting the snippet for the marker
        markerOptions.snippet(description);

        return markerOptions;
    }

    // Da un oggetto geopoint (coordinate bus) crea e restituisce il marker da disegnare sulla mappa
    private MarkerOptions createMarkerBus(ParseObject t) {
        MarkerOptions markerOptions = new MarkerOptions();

        // Getting latitude of the place
        double lat = t.getParseGeoPoint("geo_point").getLatitude();

        // Getting longitude of the place
        double lng = t.getParseGeoPoint("geo_point").getLongitude();

        // Getting name
        String name = t.getString("fermata");

        LatLng latLng = new LatLng(lat, lng);

        // Setting the position for the marker
        markerOptions.position(latLng);

        // Setting the title for the marker
        markerOptions.title(name);

        // Setting the snippet for the marker
        markerOptions.snippet("Tieni premuto per maggiori informazioni");

        return markerOptions;
    }

    //Metodo per tracciare il percorso sulla mappa da un punto A ad un punto B passati come parametro
    private void drawFromAtoB(LatLng a, LatLng b){
        // Getting URL to the Google Directions API
        String url = getDirectionsUrl(a, b);

        DownloadTaskDirections downloadTask = new DownloadTaskDirections();

        // Start downloading json data from Google Directions API
        downloadTask.execute(url);
    }



    //Date due posizioni come parametro, origin e dest, crea l'url per tracciare il percorso tra i 2, a piedi
    private String getDirectionsUrl(LatLng origin,LatLng dest){

        // Origin of route
        String str_origin = "origin="+origin.latitude+","+origin.longitude;

        // Destination of route
        String str_dest = "destination="+dest.latitude+","+dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";

        // Travel mode type
        String mode = "mode=walking";

        // Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest+"&"+mode+"&"+sensor;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;

        return url;
    }


    /** A class to download data from Google Directions URL */
    private class DownloadTaskDirections extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTaskDirections parserTask = new ParserTaskDirections();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

    /** A class to parse the Google Directions in JSON format */
    private class ParserTaskDirections extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> >{

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            for(int i=0;i<result.size();i++){
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions

                int [] colors = new int[8];

                //Initialize the values of the array
                colors[0] = Color.RED;
                colors[1] = Color.BLUE;
                colors[2] = Color.YELLOW;
                colors[3] = Color.GRAY;
                colors[4] = Color.MAGENTA;
                colors[5] = Color.GREEN;
                colors[6] = Color.CYAN;
                colors[7] = Color.BLACK;

                int cnt = 0;
                for(LatLng l : points){
                    lineOptions.add(l);
                    lineOptions.width(8);
                    lineOptions.color(colors[cnt%7]);
                    cnt++;
                }
/*
                lineOptions.addAll(points);
                lineOptions.width(8);
                lineOptions.color(Color.BLUE);*/

            }

            // Drawing polyline in the Google Map for the i-th route
            mMap.addPolyline(lineOptions);
        }
    }

    /** A method to download json data from url */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception download url", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private void showInformazioniBusDialog(int markerPosition, String titleBus){
        FragmentManager fm = getFragmentManager();
        final InformazioniBusDialog informazioniBusDialog = new InformazioniBusDialog();
        b = new Bundle();
        b.putInt("markerPosition", markerPosition);
        b.putString("fermata", titleBus);
        informazioniBusDialog.setArguments(b);
        informazioniBusDialog.show(fm, "fragment_informazionibus_dialog");
        informazioniBusDialog.setTargetFragment(this, 3);
    }



    //*********************************************************************************************************************


    private void showInformazioniTappeBusDialog(final Marker currentMarker, final Marker nextMarker){
        b = new Bundle();
        ParseQuery<ParseObject> info_linea = ParseQuery.getQuery("info_linea_nuovo");

        //fermate intorno a currentMarker
        ParseGeoPoint currentGeoPoint = new ParseGeoPoint();
        currentGeoPoint.setLatitude(currentMarker.getPosition().latitude);
        currentGeoPoint.setLongitude(currentMarker.getPosition().longitude);
        info_linea.whereWithinKilometers("geo_point", currentGeoPoint, 1);

        try {
            List<ParseObject> objects = info_linea.find();
            hsFBcurrent = new HashSet<FermataBus>();
            for (ParseObject parseObject : objects) {
                String linea = parseObject.getString("linea_bus");
                String fermata = parseObject.getString("fermata");
                FermataBus fb = new FermataBus(linea, fermata);
                hsFBcurrent.add(fb);
            }
            hsFBcurrent = controllaDuplicati(hsFBcurrent);

            //fermate intorno a nextMarker
            ParseQuery<ParseObject> info_linea2 = ParseQuery.getQuery("info_linea_nuovo");
            ParseGeoPoint nextGeoPoint = new ParseGeoPoint();
            nextGeoPoint.setLatitude(nextMarker.getPosition().latitude);
            nextGeoPoint.setLongitude(nextMarker.getPosition().longitude);
            info_linea2.whereWithinKilometers("geo_point", nextGeoPoint, 1);
            info_linea2.whereEqualTo("a_r", "andata");

            List<ParseObject> objects2 = info_linea2.find();
            hsFBnext = new HashSet<FermataBus>();
            for (ParseObject parseObject : objects2) {
                String linea = parseObject.getString("linea_bus");
                String fermata = parseObject.getString("fermata");
                FermataBus fb = new FermataBus(linea, fermata);
                hsFBnext.add(fb);
            }
            hsFBnext = controllaDuplicati(hsFBnext);
            ALlineainComune = new ArrayList<String>();

            for (FermataBus f1 : hsFBcurrent) {
                for (FermataBus f2 : hsFBnext) {
                    if (f2.getLinea().equals(f1.getLinea()) && !(f1.getFermata().equals(f2.getFermata()))) {

                        currentFermata = f1.getFermata();
                        nextFermata = f2.getFermata();
                        String lineaComune = f1.getLinea();

                        //calcola durata e riempie la dialog successiva


                        //query per capire se e' la corsa di andata o ritorno
                        ParseQuery query = ParseQuery.getQuery("trasporti");
                        query.whereContains("linea", lineaComune);
                        List<ParseObject> objects3 = query.find();
                        for (ParseObject object : objects3) {
                            String sequenzaFermateAndata = (String) object.get("fermateAndata");
                            String fermateAndata[] = sequenzaFermateAndata.split(" ,");
                            for (String fermata : fermateAndata) {
                                if ((fermata.equals(currentFermata) && flagAndata == false))
                                    flagAndata = true;
                            }
                        }


                        //query per trovare gli orari della fermata corrente
                        ParseQuery queryOrari = ParseQuery.getQuery("info_linea_nuovo");
                        if (flagAndata)
                            queryOrari.whereContains("a_r", "andata");
                        else
                            queryOrari.whereContains("a_r", "ritorno");

                        queryOrari = queryOrari.whereContains("fermata", currentFermata);
                        List<ParseObject> objects4 = queryOrari.find();
                        for (ParseObject object : objects4) {
                            tmpOrarioCurrent = (String) object.get("orari");
                        }

                        //query per trovare gli orari della fermata next
                        ParseQuery queryOrari2 = ParseQuery.getQuery("info_linea_nuovo");
                        if (flagAndata)
                            queryOrari2.whereContains("a_r", "andata");
                        else
                            queryOrari2.whereContains("a_r", "ritorno");

                        queryOrari2 = queryOrari2.whereContains("fermata", nextFermata);
                        List<ParseObject> objects5 = queryOrari2.find();

                        for (ParseObject object : objects5) {
                            tmpOrarioNext = (String) object.get("orari");
                        }

                        //calcolo effettivo della durata
                        String tmp[] = tmpOrarioCurrent.split(" ");
                        String tmpPrimoOrariCurrent = tmp[0];

                        tmp = tmpOrarioNext.split(" ");
                        String tmpPrimoOrarioNext = tmp[0];

                        tmp = tmpPrimoOrariCurrent.split("\\.");
                        int oraCurrent = Integer.parseInt(tmp[0]);
                        int minutiCurrent = Integer.parseInt(tmp[1]);


                        tmp = tmpPrimoOrarioNext.split("\\.");
                        int oraNext = Integer.parseInt(tmp[0]);
                        int minutiNext = Integer.parseInt(tmp[1]);

                        int diffOra = Math.abs(oraNext - oraCurrent);

                        if (diffOra != 0) {
                            diffOra = diffOra * 60;
                        }
                        int diffMinuti = Math.abs(minutiNext - minutiCurrent);
                        durata = diffOra + diffMinuti;
                        String tmpString = lineaComune + ": Da " + currentFermata + " A " + nextFermata + " Durata " + durata + " minuti";
                        ALlineainComune.add(tmpString);



                        Log.e("FermataA", currentFermata + "");
                        Log.e("FermataR", nextFermata + "");

                    }
                }
            }

            FragmentManager fm = getFragmentManager();
            InformazioniNextTappaDialog informazioniNextTappaDialog = new InformazioniNextTappaDialog();
            b.putString("nextTappa", nextMarker.getTitle());
            b.putStringArrayList("ALF",ALlineainComune);
            informazioniNextTappaDialog.setArguments(b);
            informazioniNextTappaDialog.show(fm, "fragment_informazioni_next_tappa_dialog");



        } catch (ParseException e) {
            e.printStackTrace();
        }


/*
                                FragmentManager fm = getFragmentManager();
                                InformazioniNextTappaDialog informazioniNextTappaDialog = new InformazioniNextTappaDialog();

                                Bundle b = new Bundle();
                                b.putStringArrayList("ALF", ALlineainComune);
                                b.putString("nextTappa", nextMarker.getTitle());
                                informazioniNextTappaDialog.setArguments(b);
                                informazioniNextTappaDialog.show(fm, "fragment_informazioni_next_tappa_dialog");

*/


    }





    private void showInformazioniTappeBusDialog2(final Marker currentMarker, final Marker nextMarker){
        b = new Bundle();
        ParseQuery<ParseObject> info_linea = ParseQuery.getQuery("info_linea_nuovo");

        //fermate intorno a currentMarker
        ParseGeoPoint currentGeoPoint = new ParseGeoPoint();
        currentGeoPoint.setLatitude(currentMarker.getPosition().latitude);
        currentGeoPoint.setLongitude(currentMarker.getPosition().longitude);
        info_linea.whereWithinKilometers("geo_point", currentGeoPoint, 1);
        info_linea.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    hsFBcurrent = new HashSet<FermataBus>();
                    for (ParseObject parseObject : objects) {
                        String linea = parseObject.getString("linea_bus");
                        String fermata = parseObject.getString("fermata");
                        FermataBus fb = new FermataBus(linea, fermata);
                        hsFBcurrent.add(fb);
                    }

                    hsFBcurrent = controllaDuplicati(hsFBcurrent);



                    //fermate intorno a nextMarker
                    ParseQuery<ParseObject> info_linea = ParseQuery.getQuery("info_linea_nuovo");
                    ParseGeoPoint nextGeoPoint = new ParseGeoPoint();
                    nextGeoPoint.setLatitude(nextMarker.getPosition().latitude);
                    nextGeoPoint.setLongitude(nextMarker.getPosition().longitude);
                    info_linea.whereWithinKilometers("geo_point", nextGeoPoint, 1);
                    info_linea.whereEqualTo("a_r", "andata");
                    info_linea.findInBackground(new FindCallback<ParseObject>() {
                        public void done(List<ParseObject> objects, ParseException e) {

                            hsFBnext = new HashSet<FermataBus>();
                            if (e == null) {

                                for (ParseObject parseObject : objects) {
                                    String linea = parseObject.getString("linea_bus");
                                    String fermata = parseObject.getString("fermata");
                                    FermataBus fb = new FermataBus(linea, fermata);
                                    hsFBnext.add(fb);
                                }

                                hsFBnext = controllaDuplicati(hsFBnext);

                                ALlineainComune = new ArrayList<String>();
                                cntFor =0;
                                cntLista =0;
                                for (FermataBus f1 : hsFBcurrent) {
                                    for (FermataBus f2 : hsFBnext) {
                                        if (f2.getLinea().equals(f1.getLinea()) && !(f1.getFermata().equals(f2.getFermata()))) {
                                            cntLista++;

                                            currentFermata = f1.getFermata();
                                            nextFermata = f2.getFermata();
                                            String lineaComune = f1.getLinea();

                                            //calcola durata e riempie la dialog successiva

                                            //query per capire se e' la corsa di andata o ritorno
                                            ParseQuery query = ParseQuery.getQuery("trasporti");
                                            query.whereContains("linea", lineaComune);
                                            query.findInBackground(new FindCallback<ParseObject>() {
                                                public void done(List<ParseObject> objects, ParseException e) {
                                                    if (e == null) {
                                                        for (ParseObject object : objects) {
                                                            String sequenzaFermateAndata = (String) object.get("fermateAndata");
                                                            String fermateAndata[] = sequenzaFermateAndata.split(" ,");
                                                            for (String fermata : fermateAndata) {
                                                                if ((fermata.equals(currentFermata) && flagAndata == false))
                                                                    flagAndata = true;
                                                            }

                                                        }


                                                        //query per trovare gli orari della fermata corrente
                                                        ParseQuery queryOrari = ParseQuery.getQuery("info_linea_nuovo");
                                                        if (flagAndata)
                                                            queryOrari.whereContains("a_r", "andata");
                                                        else
                                                            queryOrari.whereContains("a_r", "ritorno");

                                                        queryOrari = queryOrari.whereContains("fermata", currentFermata);
                                                        queryOrari.findInBackground(new FindCallback<ParseObject>() {
                                                            public void done(List<ParseObject> objects, ParseException e) {
                                                                if (e == null) {
                                                                    for (ParseObject object : objects) {
                                                                        tmpOrarioCurrent = (String) object.get("orari");
                                                                    }

                                                                    //query per trovare gli orari della fermata next
                                                                    ParseQuery queryOrari = ParseQuery.getQuery("info_linea_nuovo");
                                                                    if (flagAndata)
                                                                        queryOrari.whereContains("a_r", "andata");
                                                                    else
                                                                        queryOrari.whereContains("a_r", "ritorno");

                                                                    queryOrari = queryOrari.whereContains("fermata", nextFermata);
                                                                    queryOrari.findInBackground(new FindCallback<ParseObject>() {
                                                                        public void done(List<ParseObject> objects, ParseException e) {
                                                                            if (e == null) {
                                                                                for (ParseObject object : objects) {
                                                                                    tmpOrarioNext = (String) object.get("orari");
                                                                                }


                                                                                //calcolo effettivo della durata
                                                                                String tmp[] = tmpOrarioCurrent.split(" ");
                                                                                String tmpPrimoOrariCurrent = tmp[0];

                                                                                tmp = tmpOrarioNext.split(" ");
                                                                                String tmpPrimoOrarioNext = tmp[0];

                                                                                tmp = tmpPrimoOrariCurrent.split("\\.");
                                                                                int oraCurrent = Integer.parseInt(tmp[0]);
                                                                                int minutiCurrent = Integer.parseInt(tmp[1]);


                                                                                tmp = tmpPrimoOrarioNext.split("\\.");
                                                                                int oraNext = Integer.parseInt(tmp[0]);
                                                                                int minutiNext = Integer.parseInt(tmp[1]);

                                                                                int diffOra = Math.abs(oraNext - oraCurrent);

                                                                                if (diffOra != 0) {
                                                                                    diffOra = diffOra * 60;
                                                                                }
                                                                                int diffMinuti = Math.abs(minutiNext - minutiCurrent);
                                                                                durata = diffOra + diffMinuti;
                                                                                //String tmpString = lineaComune + ": Da " + currentFermata + " A " + nextFermata + " Durata " + durata + " minuti";
                                                                                String tmpString = "Linea TOT" + ": Da " + "Fermata A" + " A " + "Fermata B" + " Durata " + durata + " minuti";
                                                                                //ALlineainComune.add(tmpString);

                                                                                b.putString("Item"+cntLista, tmpString);


                                                                                Log.e("FermataA", currentFermata + "");
                                                                                Log.e("FermataR", nextFermata + "");

                                                                                if(cntFor == hsFBcurrent.size())
                                                                                    flagStop=true;
/*
                                                                                View vv = informazioniNextTappaDialog.getView();
                                                                                TextView tv = (TextView) vv.findViewById(R.id.titoloInfo);
                                                                                tv.setText("AAAAAAAAAAAAAAAAAAAAAAAA");
                                                                                */




                                                                                b.putString("nextTappa", nextMarker.getTitle());


                                                                            } else {
                                                                                Log.e("Calcolo durata", e.getMessage());
                                                                            }
                                                                        }
                                                                    });

                                                                } else {
                                                                    Log.e("Calcolo durata", e.getMessage());
                                                                }
                                                            }
                                                        });


                                                    } else {
                                                        Log.e("Calcolo durata", e.getMessage());
                                                    }
                                                }
                                            });


                                        }
                                    }


                                    cntFor++;
                                }

                                FragmentManager fm = getFragmentManager();
                                InformazioniNextTappaDialog informazioniNextTappaDialog = new InformazioniNextTappaDialog();
                                b.putString("nextTappa", nextMarker.getTitle());
                                informazioniNextTappaDialog.setArguments(b);
                                informazioniNextTappaDialog.show(fm, "fragment_informazioni_next_tappa_dialog");

/*
                                FragmentManager fm = getFragmentManager();
                                InformazioniNextTappaDialog informazioniNextTappaDialog = new InformazioniNextTappaDialog();

                                Bundle b = new Bundle();
                                b.putStringArrayList("ALF", ALlineainComune);
                                b.putString("nextTappa", nextMarker.getTitle());
                                informazioniNextTappaDialog.setArguments(b);
                                informazioniNextTappaDialog.show(fm, "fragment_informazioni_next_tappa_dialog");

*/



                            } else {
                                Log.e("Bus location", e.getMessage());
                            }
                        }
                    });


                } else {
                    Log.e("Bus location", e.getMessage());
                }
            }
        });

    }

    private HashSet<FermataBus> controllaDuplicati(HashSet<FermataBus> hsFB) {
        HashSet<FermataBus> hsTmp = new HashSet<FermataBus>();
        for(FermataBus fb1 : hsFB){
            if(hsTmp.size()==0)
                hsTmp.add(fb1);
            else {
                for (FermataBus fb2 : hsTmp) {
                    if (!(fb1.getLinea().equals(fb2.getLinea())))
                        if (!(fb1.getFermata().equals(fb2.getFermata())))
                            hsTmp.add(fb1);
                }
            }
        }
        return hsTmp;
    }
/*
    private int calcolaTempo(String tmpOrariCurrent, String tmpOrariNext) {
        String tmp [] = tmpOrariCurrent.split("\\.");
        int oraCurrent = Integer.parseInt(tmp[0]);
        int minutiCurrent = Integer.parseInt(tmp[1]);

        tmp = tmpOrariNext.split("\\.");
        int oraNext = Integer.parseInt(tmp[0]);
        int minutiNext = Integer.parseInt(tmp[1]);

        int diffOra = Math.abs(oraNext - oraCurrent);

        if(diffOra != 0){
            diffOra = diffOra*60;
        }
        int diffMinuti = Math.abs(minutiNext - minutiCurrent);
        int durata = diffOra+diffMinuti;

        return durata;
    }*/
/*
    private int calcolaTempo(final String currentFermata, final String nextFermata, String linea) {

        //query per capire se e' la corsa di andata o ritorno
        ParseQuery query = ParseQuery.getQuery("trasporti");
        query.whereContains("linea", linea);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    for (ParseObject object : objects) {
                        String sequenzaFermateAndata = (String) object.get("fermateAndata");
                        String fermateAndata[] = sequenzaFermateAndata.split(" ,");
                        for (String fermata : fermateAndata) {
                            if ((fermata.equals(currentFermata) && flagAndata == false))
                                flagAndata = true;
                        }

                    }


                    //query per trovare gli orari della fermata corrente
                    ParseQuery queryOrari = ParseQuery.getQuery("info_linea_nuovo");
                    if (flagAndata)
                        queryOrari.whereContains("a_r", "andata");
                    else
                        queryOrari.whereContains("a_r", "ritorno");

                    queryOrari = queryOrari.whereContains("fermata",currentFermata);
                    queryOrari.findInBackground(new FindCallback<ParseObject>() {
                        public void done(List<ParseObject> objects, ParseException e) {
                            if (e == null) {
                                for (ParseObject object : objects) {
                                    tmpOrarioCurrent = (String)object.get("orari");
                                }

                                //query per trovare gli orari della fermata next
                                ParseQuery queryOrari = ParseQuery.getQuery("info_linea_nuovo");
                                if (flagAndata)
                                    queryOrari.whereContains("a_r", "andata");
                                else
                                    queryOrari.whereContains("a_r", "ritorno");

                                queryOrari = queryOrari.whereContains("fermata",nextFermata);
                                queryOrari.findInBackground(new FindCallback<ParseObject>() {
                                    public void done(List<ParseObject> objects, ParseException e) {
                                        if (e == null) {
                                            for (ParseObject object : objects) {
                                                tmpOrarioNext = (String)object.get("orari");
                                            }


                                            //calcolo effettivo della durata
                                            String tmp[] = tmpOrarioCurrent.split(" ");
                                            String tmpPrimoOrariCurrent = tmp[0];

                                            tmp = tmpOrarioNext.split(" ");
                                            String tmpPrimoOrarioNext = tmp[0];

                                            tmp= tmpPrimoOrariCurrent.split("\\.");
                                            int oraCurrent = Integer.parseInt(tmp[0]);
                                            int minutiCurrent = Integer.parseInt(tmp[1]);


                                            tmp = tmpPrimoOrarioNext.split("\\.");
                                            int oraNext = Integer.parseInt(tmp[0]);
                                            int minutiNext = Integer.parseInt(tmp[1]);

                                            int diffOra = Math.abs(oraNext - oraCurrent);

                                            if(diffOra != 0){
                                                diffOra = diffOra*60;
                                            }
                                            int diffMinuti = Math.abs(minutiNext - minutiCurrent);
                                            durata = diffOra+diffMinuti;
                                        }
                                        else {
                                            Log.e("Calcolo durata", e.getMessage());
                                        }
                                    }
                                });
                            }
                            else {
                                Log.e("Calcolo durata", e.getMessage());
                            }
                        }
                    });
                } else {
                    Log.e("Calcolo durata", e.getMessage());
                }
            }
        });
        return durata;
    }
*/

}
