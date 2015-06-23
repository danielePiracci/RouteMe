package com.teamrouteme.routeme.utility;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.teamrouteme.routeme.R;

/**
 * Created by nicolopergola on 23/06/15.
 */
public class CustomInfoView implements GoogleMap.InfoWindowAdapter {

    LayoutInflater inflater = null;
    private TextView textViewTitle;

    public CustomInfoView(LayoutInflater inflater) {
        this.inflater = inflater;
    }
    public View getInfoWindow(Marker marker) {
        View v = inflater.inflate(R.layout.info_custom, null);
        if (marker != null) {
            textViewTitle = (TextView) v.findViewById(R.id.title);
            textViewTitle.setText("LACAPOCCHIA");
        }
        return v;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }
}
