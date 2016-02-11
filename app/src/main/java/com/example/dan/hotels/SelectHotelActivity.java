package com.example.dan.hotels;

import android.app.TaskStackBuilder;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class SelectHotelActivity extends AppCompatActivity {
    //url для информации об отдельном отеле, для картинки (+id)
    private static final String HOTELS_FOLDER_URL =
            "https://dl.dropboxusercontent.com/u/109052005/1/";

    private Bitmap hotelImg = null;
    private long selectedHotelID;
    private Hotel selectedHotel = null;
    private ProgressBar progressBarView;
    // Выполняющийся таск загрузки файла
    private JSONDownloadTask downloadTask;
    private DownloadImageTask downloadImageTask;
    private TextView textViewErr;
    private LinearLayout linearLayoutData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_hotel);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        selectedHotelID = getIntent().getExtras().getLong("id");
        setTitle(getIntent().getExtras().getString(getString(R.string.key_name)));

        progressBarView = (ProgressBar)findViewById(R.id.progressBarSelected);
        progressBarView.setVisibility(View.VISIBLE);

        textViewErr = (TextView)findViewById(R.id.textViewErr);
        textViewErr.setVisibility(View.INVISIBLE);

        linearLayoutData = (LinearLayout)findViewById(R.id.linearLayoutSelected);
        linearLayoutData.setVisibility(View.INVISIBLE);

        if (savedInstanceState != null) {
            downloadTask = (SelectHotelActivity.JSONDownloadTask) getLastCustomNonConfigurationInstance();
            downloadImageTask = (SelectHotelActivity.DownloadImageTask) getLastCustomNonConfigurationInstance();
            selectedHotel = savedInstanceState.getParcelable("hotel");
            hotelImg = savedInstanceState.getParcelable("img");
        }

        if (selectedHotel == null) {
            // Создаем новый таск
            downloadTask = new JSONDownloadTask(this);
            downloadImageTask = new DownloadImageTask(this);
            downloadTask.execute();
        } else {
            //Log.d(TAG, "Get task");
            // Передаем в ранее запущенный таск текущий объект Activity
            downloadTask.attachActivity(this);
            downloadImageTask.attachActivity(this);
            if (selectedHotel != null) {
                inflateActivity(selectedHotel);
            } else {
                Log.e(TAG, "hotel = null in onCreate");
            }

        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        downloadTask.cancel(true);
        downloadImageTask.cancel(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("hotel", selectedHotel);
        outState.putParcelable("img", hotelImg);
        //TODO переделать в sharedpreferences, картинку в отдельный файл
    }

    public static class JSONDownloadTask extends AsyncTask<Void, Void, Boolean> {
        // Текущий объект Activity, храним для обновления отображения
        private SelectHotelActivity activity;
        private Boolean successful;
        JSONDownloadTask(SelectHotelActivity activity) {
            this.activity = activity;
        }

        /**
         * Подключаем  после смены конфигурации экрана
         *@param activity новый объект активити
         */
        void attachActivity(SelectHotelActivity activity) {
            this.activity = activity;
            //updateView();
        }

        @Override
        protected void onPreExecute() {
            //  Log.d(TAG, "PreExecute ..");
        }

        @Override
        protected Boolean doInBackground(Void... ignore) {
            successful = true;
            try {
                JSONHotelsParseUtils parseUtils = new JSONHotelsParseUtils();
                activity.selectedHotel = parseUtils.readJSONStream(HOTELS_FOLDER_URL + activity.selectedHotelID + ".json", true).get(0);
                Log.d(TAG, "Parce successful");

            } catch (Exception e) {
                successful = false;
                Log.e(TAG, "Error downloading file: " + e, e);
            }
            return successful;
        }

        @Override
        protected void onPostExecute(Boolean successful) {
            // Log.d(TAG, "successful: " + successful);
            if (successful) {
                Log.d(TAG, "Successful obtaining hotel");
                DownloadImageTask downloadImageTask = new DownloadImageTask(activity);
                downloadImageTask.execute(HOTELS_FOLDER_URL + activity.selectedHotel.imgFile);
            } else {
                //TODO добавить кнопку "Try again", при нажатии на неё запускать новый таск, закрывать старый
                activity.progressBarView.setVisibility(View.INVISIBLE);
                activity.textViewErr.setVisibility(View.VISIBLE);
                activity.textViewErr.setText(R.string.internet_conn_err);
            }
        }
    }
    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        // Смена конфигурации экрана
        return downloadTask;
    }

    public void inflateActivity(Hotel hotel) {
        progressBarView.setVisibility(View.VISIBLE);
        linearLayoutData.setVisibility(View.INVISIBLE);

        ImageView imgViewHotel = (ImageView) findViewById(R.id.imageViewHotel);
        if (hotelImg != null) {
            Display display = getWindowManager().getDefaultDisplay();
            DisplayMetrics metricsB = new DisplayMetrics();
            display.getMetrics(metricsB);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(metricsB.widthPixels, metricsB.widthPixels * 9 / 16);
            imgViewHotel.setLayoutParams(layoutParams);
            imgViewHotel.setImageBitmap(hotelImg);
        }

        //устанавливаем значения TextView
        TextView textView = (TextView)findViewById(R.id.textView_nameSelected);
        textView.setText(hotel.name);

        textView = (TextView)findViewById(R.id.textView_addressSelected);
        textView.setText(hotel.address);

        textView = (TextView)findViewById(R.id.textView_starsSelected);
        textView.setText(getString(R.string.hotel_stars) + " " + hotel.stars);

        textView = (TextView)findViewById(R.id.textView_distanceSelected);
        textView.setText(getString(R.string.hotel_distance) + " " + hotel.distance);

        textView = (TextView)findViewById(R.id.textView_suitesSelected);
        textView.setText(getString(R.string.hotel_suites) + " " + hotel.countOfAvailableRooms);

        textView = (TextView)findViewById(R.id.textView_latSelected);
        textView.setText(getString(R.string.hotel_lat) + " " + hotel.lat);

        textView = (TextView)findViewById(R.id.textView_lonSelected);
        textView.setText(getString(R.string.hotel_lon) + " " + hotel.lon);

        progressBarView.setVisibility(View.INVISIBLE);
        linearLayoutData.setVisibility(View.VISIBLE);
    }

    private static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        private SelectHotelActivity activity;

        public DownloadImageTask(SelectHotelActivity activity) {
            this.activity = activity;
        }

        void attachActivity(SelectHotelActivity activity) {
            this.activity = activity;
            //updateView();
        }

        protected Bitmap doInBackground(String... urls) {
            Log.d(TAG, "Start downloading picture...");
            String urldisplay = urls[0];
            Bitmap image = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                image = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return image;
        }

        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                result = Bitmap.createBitmap(result, 2, 2, result.getWidth() - 2, result.getHeight() - 5);
                activity.hotelImg = result;
            }
            activity.inflateActivity(activity.selectedHotel);
        }
    }

    private static final String TAG = "SelectHotel";
}
