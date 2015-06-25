package com.teamrouteme.routeme.fragment;

import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.LevelListDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.parse.ParseUser;
import com.teamrouteme.routeme.R;
import com.teamrouteme.routeme.bean.Itinerario;
import com.teamrouteme.routeme.bean.Tappa;
import com.teamrouteme.routeme.utility.CustomInfoView;
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
    private boolean itinerariScaricati = false;
    private ArrayList<Marker> markersBus = new ArrayList<Marker>();

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
            mMap.addMarker(mO);
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
                    Bundle b = new Bundle();
                    b.putParcelable("itinerario", itinerario);
                    anteprimaItinerariScaricatiFragment.setArguments(b);
                    // Set new fragment on screen
                    MaterialNavigationDrawer home = (MaterialNavigationDrawer) getActivity();
                    home.setFragment(anteprimaItinerariScaricatiFragment, "Anteprima Itinerario");
                }
                else {
                    Fragment anteprimaMieiItinerariFragment = new AnteprimaMieiItinerariFragment();
                    Bundle b = new Bundle();
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
            ParseQuery<ParseObject> info_linea = ParseQuery.getQuery("info_linea");

            ParseGeoPoint tappaLocation = tappe.get(index).getCoordinate();
            info_linea.whereWithinKilometers("geo_point", tappaLocation, 0.5);

            info_linea.findInBackground(new FindCallback<ParseObject>() {
                public void done(List<ParseObject> objects, ParseException e) {

                    MarkerOptions markerOptions;

                    if (e == null) {
                        for(ParseObject parseObject : objects){
                            markerOptions = createMarkerBus(parseObject);
                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_bus));
                            Marker marker = mMap.addMarker(markerOptions);
                            markersBus.add(marker);

                            mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {

                                @Override
                                public void onMapLongClick(LatLng latLng) {
                                    for (int i = 0; i <  markersBus.size(); i++) {
                                        Marker marker = markersBus.get(i);
                                        if (Math.abs(marker.getPosition().latitude - latLng.latitude) < 0.0009 && Math.abs(marker.getPosition().longitude - latLng.longitude) < 0.0009) {
                                            showInformazioniBusDialog(i, marker.getTitle());
                                            Log.d(TAG, "Trovato marker tappa " + marker.getTitle());
                                            break;
                                        }

                                    }

                                }
                            });
                        }
                        recursiveGetFermate(tappe,index+1);

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
                lineOptions.addAll(points);
                lineOptions.width(8);
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
        Bundle b = new Bundle();
        b.putInt("markerPosition", markerPosition);
        b.putString("fermata", titleBus);
        informazioniBusDialog.setArguments(b);
        informazioniBusDialog.show(fm, "fragment_informazionibus_dialog");
        informazioniBusDialog.setTargetFragment(this, 3);
    }
}
