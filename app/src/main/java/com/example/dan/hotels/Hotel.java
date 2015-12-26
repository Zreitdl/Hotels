package com.example.dan.hotels;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by Dan) on 08.11.2015.
 */
public class Hotel implements Parcelable {
    public long id;
    public String name;
    public String address;
    public double stars;
    public double distance;
    public String imgFile;
    public long countOfAvailableRooms;
    public double lat;
    public double lon;

    public Hotel(long id, String name, String address, double stars, double distance,
                 long countOfAvailableRooms, String imgFile, double lat, double lon ) {

        this.id = id;
        this.name = name;
        this.address = address;
        this.stars = stars;
        this.distance = distance;
        this.countOfAvailableRooms = countOfAvailableRooms;
        this.imgFile = imgFile;
        this.lat = lat;
        this.lon = lon;

    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(id);
        parcel.writeString(name);
        parcel.writeString(address);
        parcel.writeDouble(stars);
        parcel.writeDouble(distance);
        parcel.writeString(imgFile);
        parcel.writeLong(countOfAvailableRooms);
        parcel.writeDouble(lat);
        parcel.writeDouble(lon);

    }

    public static final Parcelable.Creator<Hotel> CREATOR = new Parcelable.Creator<Hotel>() {
        // распаковываем объект из Parcel
        public Hotel createFromParcel(Parcel in) {
            return new Hotel(in);
        }

        public Hotel[] newArray(int size) {
            return new Hotel[size];
        }
    };

    // конструктор, считывающий данные из Parcel
    private Hotel(Parcel parcel) {
        id = parcel.readLong();
        name = parcel.readString();
        address = parcel.readString();
        stars = parcel.readDouble();
        distance = parcel.readDouble();
        imgFile = parcel.readString();
        countOfAvailableRooms = parcel.readLong();
        lat = parcel.readDouble();
        lon = parcel.readDouble();
    }
}
