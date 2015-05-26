package com.teamrouteme.routeme.fragment;

import android.app.Dialog;
import android.app.Fragment;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.teamrouteme.routeme.R;
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
        }

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
                Fragment anteprimaItinerarioFragment = new AnteprimaMieiItinerariFragment();

                Bundle b = new Bundle();
                b.putParcelable("itinerario",itinerario);
                anteprimaItinerarioFragment.setArguments(b);
                // Set new fragment on screen
                MaterialNavigationDrawer home = (MaterialNavigationDrawer) getActivity();
                home.setFragment(anteprimaItinerarioFragment, "Anteprima Itinerario");
            }
        });

        return view;
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

        LatLng latLng = new LatLng(lat, lng);

        // Setting the position for the marker
        markerOptions.position(latLng);

        // Setting the title for the marker
        markerOptions.title(name);

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
                lineOptions.addAll(points);
                lineOptions.width(4);
                lineOptions.color(Color.BLUE);
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
        try{
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
            while( ( line = br.readLine()) != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d("Exception download url", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }
}
