package com.teamrouteme.routeme.bean;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * Created by daniele on 03/07/15.
 */
public class FermataBus implements Parcelable{
    private String linea;
    private String fermata;


    public FermataBus(String linea, String fermata) {

        this.linea = linea;
        this.fermata = fermata;

    }



    public String getLinea() {
        return linea;
    }

    public void setLinea(String linea) {
        this.linea = linea;
    }

    public String getFermata() {
        return fermata;
    }

    public void setFermata(String fermata) {
        this.fermata = fermata;
    }

    @Override
    public boolean equals(Object o) {
        FermataBus myFermata = (FermataBus) o;
        if ((this.getLinea().equals(myFermata.getLinea())) && (this.getFermata().equals(myFermata.getFermata())))
            return true;
        return false;

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
}
