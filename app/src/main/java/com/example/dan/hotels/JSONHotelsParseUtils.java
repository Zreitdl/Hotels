package com.example.dan.hotels;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dan) on 09.11.2015.
 */
final class JSONHotelsParseUtils {

    private boolean oneObject = false;

    JSONHotelsParseUtils() {}

    public ArrayList<Hotel> readJSONStream(String downloadUrl, boolean isOneObject) throws IOException {

        Log.d(TAG, "Start downloading url: " + downloadUrl);

        oneObject = isOneObject;

        HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();

        InputStream in = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        JsonReader jsonReader = null;

        try{
            int responseCode = conn.getResponseCode();
            Log.d(TAG, "Received HTTP response code: " + responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new FileNotFoundException(conn.getResponseMessage());
            }

            in = conn.getInputStream();
            inputStreamReader = new InputStreamReader(in);
            bufferedReader = new BufferedReader(inputStreamReader);
            jsonReader = new JsonReader(inputStreamReader);

            try {
                return readHotelsArray(jsonReader);
            }
            finally {
                jsonReader.close();
            }

        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                   //failed to close input stream
                    Log.e(TAG, "Failed to close HTTP input stream: " + e, e);
                }
            }
            conn.disconnect();
        }
    }

    private ArrayList<Hotel> readHotelsArray(JsonReader reader) throws IOException {
        ArrayList<Hotel> hotels = new ArrayList<>();

        if (!oneObject) {
            reader.beginArray();
        }
        while (reader.hasNext()) {
            hotels.add(readHotel(reader));
            if (oneObject) {
                return hotels;
            }
        }
        reader.endArray();
        Log.d(TAG, "Hotels added");
        return hotels;
    }

    private Hotel readHotel(JsonReader reader) throws IOException {
        long id = -1;
        String name = null;
        String address = null;
        double stars = -1;
        double distance = -1;
        String imgFile = null;
        long countOfAvailableRooms = -1;
        double lat = 0;
        double lon = 0;


        reader.beginObject();
        while (reader.hasNext()) {
            String field = reader.nextName();
            if (field.equals("id")) {
                id = reader.nextLong();
                Log.d(TAG, "Reading hotel.. " + id);
            } else if (field.equals("name") && (reader.peek() != JsonToken.NULL)) {
                name = reader.nextString();
            } else if (field.equals("address") && (reader.peek() != JsonToken.NULL) ) {  //&& reader.peek() != JsonToken.NULL
                address = reader.nextString();
            } else if (field.equals("stars")) {
                stars = reader.nextDouble();
            } else if (field.equals("distance") ) {
                distance = reader.nextDouble();
            } else if (field.equals("image") && (reader.peek() != JsonToken.NULL) ) {  //&& reader.peek() != JsonToken.NULL
                imgFile = reader.nextString();
            } else if (field.equals("suites_availability") && (reader.peek() != JsonToken.NULL)) {
                String suites = reader.nextString();
                int[] count = new int[20];
                for (int i = 0; i < suites.length(); i++) {
                    count[suites.charAt(i) - '0']++;
                }
                countOfAvailableRooms = count[':' - '0'] + 1;
            } else if (field.equals("lat")) {
                lat = reader.nextDouble();
            } else if (field.equals("lon")) {
                lon = reader.nextDouble();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return new Hotel(id, name, address, stars, distance, countOfAvailableRooms, imgFile, lat, lon);
    }

    private static final String TAG = "DownloadAndParce";
}
