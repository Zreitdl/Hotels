package com.example.dan.hotels;

/*      TODO
        (!) Утечки памяти - fised
        При повороте экрана пропадает диалог выбора сортировки
        По кол-ву свободных мест отели следовало сортировать в убывающем порядке
        (!)При клике на меню каждый раз создаётся диалог сортировки (у Activity есть методы showDialog/onCreateDialog) - fixed
        При сортировке пересоздаётся SimpleAdapter
        CoordinatorLayout используется без необходимости
        Подсчёт кол-ва свободных мест - ненадёжно и неинтуитивно
        Использование сторонних библиотек (?)*/




import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class HotelsListActivity extends AppCompatActivity {
    //идентификатор диалога сортировки
    private final int SORT_DIALOG_ID = 0;

    //url для скачивания списка отелей в формате json
    private static final String HOTELS_LIST_JSON_URL =
            "https://dl.dropboxusercontent.com/u/109052005/1/0777.json";
    // Индикатор прогресса
    private ProgressBar progressBarView;
    // Выполняющийся таск загрузки файла
    private JSONDownloadTask downloadTask;

    //список отелей
    private ArrayList<Hotel> hotels = null;

    //сообщение об ошибке
    private TextView textView;

    //список отелей
    private ListView hotelsListView;

    //
    enum SortBy {
        distance, countOfAviableRooms, nothing
    }
    SortBy sortBy;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hotels_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sortBy = SortBy.nothing;

        hotelsListView = (ListView)findViewById(R.id.listView);

        textView = (TextView)findViewById(R.id.textView);
        textView.setVisibility(View.INVISIBLE);

        progressBarView = (ProgressBar)findViewById(R.id.progressBar);
        progressBarView.setVisibility(View.VISIBLE);

        // Пытаемся получить сохраненные значения
        if (savedInstanceState != null) {
            downloadTask = (JSONDownloadTask) getLastCustomNonConfigurationInstance();
            hotels = savedInstanceState.getParcelableArrayList("hotels");
            sortBy = (SortBy)savedInstanceState.getSerializable("sort");
        }

        if (hotels == null) {
            // Создаем новый таск
            downloadTask = new JSONDownloadTask(this);
            downloadTask.execute();
        } else {
            //Log.d(TAG, "Get task");
            // Передаем в ранее запущенный таск текущий объект Activity
            downloadTask.attachActivity(this);
            if (hotels != null) {
                inflateListView(hotels);
            } else {
                Log.e(TAG, "hotels = null in onCreate");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       /* Toast.makeText(this, "Дестрой ((",
                Toast.LENGTH_LONG).show();*/
        if (hotels == null) {
            downloadTask.cancel(true);
            downloadTask = null;
        }
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        // Смена конфигурации экрана
        return downloadTask;
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("hotels", hotels);
        outState.putSerializable("sort", sortBy);
        //TODO переделать
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        hotels = savedInstanceState.getParcelableArrayList("hotels");
    }

    public static class JSONDownloadTask extends AsyncTask<Void, Void, Boolean> {
        // Текущий объект Activity, храним для обновления отображения
        private HotelsListActivity activity;
        private Boolean successful;
        JSONDownloadTask(HotelsListActivity activity) {
            this.activity = activity;
        }

        /**
         * Подключаем  после смены конфигурации экрана
         *@param activity новый объект активити
          */
        void attachActivity(HotelsListActivity activity) {
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
                activity.hotels = parseUtils.readJSONStream(HOTELS_LIST_JSON_URL, false);
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
                activity.inflateListView(activity.hotels);
                Log.d(TAG, "Successful obtaining array of hotels");

            } else {
                //TODO добавить кнопку "Try again", при нажатии на неё запускать новый таск, закрывать старый
                activity.progressBarView.setVisibility(View.INVISIBLE);
                activity.textView.setVisibility(View.VISIBLE);
                activity.textView.setText(R.string.internet_conn_err);
            }
        }
    }

    public void inflateListView(ArrayList<Hotel> hotels) {
        hotelsListView.setVisibility(View.INVISIBLE);
        progressBarView.setVisibility(View.VISIBLE);
        Log.d(TAG, "Inflating listView..");

        if (hotels == null) {
            Log.e(TAG, "hotels = null");
            return;
        }

        if (sortBy == SortBy.distance) {
            Collections.sort(hotels, new SortedByDistance());
        } else if (sortBy == SortBy.countOfAviableRooms) {
            Collections.sort(hotels, new SortedByCountOfAvailableRooms());
        }

        ArrayList<HashMap<String, Object>> hotelsList = new ArrayList<>();
        HashMap<String, Object> hm;

        //ключи для hashMap из strings.xml
        String[] keys = new String[]{getString(R.string.key_name), getString(R.string.key_adress),
                getString(R.string.key_stars), getString(R.string.key_distance), getString(R.string.key_suites) };

        for (int i = 0; i < hotels.size(); i++) {
            hm = new HashMap<>();
            Hotel hotel = hotels.get(i);
            hm.put(keys[0], hotel.name);
            hm.put(keys[1], hotel.address);
            hm.put(keys[2], getString(R.string.hotel_stars) + " " + hotel.stars);
            hm.put(keys[3], getString(R.string.hotel_distance) + " " + hotel.distance);
            hm.put(keys[4], getString(R.string.hotel_suites) + " " + hotel.countOfAvailableRooms);
            hm.put("id", hotel.id);
            hotelsList.add(hm);
        }

        SimpleAdapter adapter = new SimpleAdapter(this, hotelsList, R.layout.list_item,
                keys, new int[]{R.id.textView_name, R.id.textView_address, R.id.textView_stars,
                 R.id.textView_distance, R.id.textView_suites});
        hotelsListView.setAdapter(adapter);
        Log.d(TAG, "Inflate ended");

        hotelsListView.setVisibility(View.VISIBLE);
        progressBarView.setVisibility(View.INVISIBLE);

        hotelsListView.setOnItemClickListener(itemClickListener);
    }

    //слушатель itemClick для ListView
    AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            HashMap<String, Object> itemHashMap = (HashMap <String, Object>) parent.getItemAtPosition(position);
            long selectedHotelId = (Long)itemHashMap.get("id");
            String selectedHotelName = (String)itemHashMap.get(getString(R.string.key_name));
            Intent intent = new Intent(HotelsListActivity.this, SelectHotelActivity.class);
            intent.putExtra("id", selectedHotelId);
            intent.putExtra("name", selectedHotelName);
            startActivity(intent);
           // Log.d(TAG, "Hotel selected: " + selectedHotelId);
        }
    };

    //Компаратор для сортировки Hotel по параметру distance
    private class SortedByDistance implements Comparator<Hotel> {

        public int compare(Hotel hotel1, Hotel hotel2) {

            double distance1 = hotel1.distance;
            double distance2 = hotel2.distance;

            if (distance1 == distance2) {
                return 0;
            } else if (distance1 < distance2) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    //Компаратор для сортировки Hotel по параметру countOfAvailableRooms
    private class SortedByCountOfAvailableRooms implements Comparator<Hotel> {

        public int compare(Hotel hotel1, Hotel hotel2) {

            long count1 = hotel1.countOfAvailableRooms;
            long count2 = hotel2.countOfAvailableRooms;

            if (count1 == count2) {
                return 0;
            } else if (count1 < count2) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_hotels_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            //sortDialog().show();
            this.showDialog(SORT_DIALOG_ID);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case SORT_DIALOG_ID:
                final String[] sortNamesArray = {getString(R.string.sort_by_distance),
                        getString(R.string.sort_by_countOfAvailableRooms)};

                AlertDialog.Builder builder = new AlertDialog.Builder(HotelsListActivity.this);
                builder.setTitle(getString(R.string.action_sort))
                        .setItems(sortNamesArray, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //если сортировка поменялась, то перезагрузить ListView и обновить сортировку
                                if (sortNamesArray[which].equals(getString(R.string.sort_by_distance))) {
                                    if (sortBy != SortBy.distance) {
                                        sortBy = SortBy.distance;
                                        if (hotels != null) {
                                            inflateListView(hotels);
                                        }
                                    }
                                } else if (sortNamesArray[which].equals(getString(R.string.sort_by_countOfAvailableRooms))) {
                                    if (sortBy != SortBy.countOfAviableRooms) {
                                        sortBy = SortBy.countOfAviableRooms;
                                        if (hotels != null) {
                                            inflateListView(hotels);
                                        }
                                    }
                                }
                            }
                        });
                return builder.create();
            default:
                return null;
        }

    }

    private static final String TAG = "HotelsList";
}
