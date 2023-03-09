package com.example.ecm2425;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements RecyclerViewInterface {

    private final String API_KEY = "5m1cJmo4lCYar60eRMhm1A==yQMvjeHswVaXi55a";

    TextView mTitle;
    TextView mBody;
    Button mCreateLogButton;
    URL quoteURL;
    TextView quote;
    Thread networkThread;
    static boolean resumed;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(Log.allLogs.size() == 0 ){
            createPersistentLogs();
        }

        resumed = true; // boolean to monitor activity state for api data pull scheduling

        /* quote setup */
        quote = findViewById(R.id.the_quote);
        quoteURL = buildUrl();

        /* networking - anonymous offloaded thread to pull api data every 8 seconds */
        networkThread = new Thread(() -> {
            try {
                while (resumed){
                    JSONObject jsonObj = getJSONResponseFromAPI(quoteURL, API_KEY);
                    if(jsonObj!=null){
                        quote.setText(jsonObj.getString("quote"));
                        Thread.sleep(8000);
                    } else {
                        throw new NullPointerException();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        networkThread.start();




        /* wire widgets */
        mTitle = findViewById(R.id.main_logTitle);
        mBody = findViewById(R.id.main_logBody);
        mCreateLogButton = findViewById(R.id.main_createLog_btn);

        /* when createLogButton is pressed, create Log object with input data */
        mCreateLogButton.setOnClickListener( v -> {
            /** add -> if null functionality **/
            Log newLog = new Log();
            String title = mTitle.getText().toString();
            String body = mBody.getText().toString();
            newLog.setTitle(title);
            newLog.setBody(body);
            Log.allLogs.add(newLog);
            clearFormData();
            addToSharedPref(newLog, getSharedPref(MainActivity.this));  // add log to persistent storage

            Intent intent = new Intent(MainActivity.this, RecordedLogs.class);
            intent.putExtra("sent_log", newLog); // use serializable version of putExtra
            startActivity(intent);
        });
    }

    @Override
    public void onPause(){
        super.onPause();
        resumed = false;
    }


    @Override
    public void onItemClick(int position) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        SharedPreferences sharedPreferences = getSharedPref(MainActivity.this);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if(MenuFunc.menuFunctionality(editor,item,MainActivity.this)){
            return true;
        }
        return false;
    }

    /* clear text views */
    void clearFormData(){
        mBody.setText("");
        mTitle.setText("");
    }

    /********** NETWORKING METHODS **********/

    /* build url */
    public static URL buildUrl() {
        URL url = null;
        try {
            url = new URL("https://api.api-ninjas.com/v1/quotes?category=happiness");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url;
    }

    /* returns a json object */
    public static JSONObject getJSONResponseFromAPI(URL url, String api_key) throws IOException {
        try{
            //make connection
            HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
            urlc.setRequestMethod("GET");
            // set the content type
            urlc.setRequestProperty("Content-Type", "application/json");
            urlc.setRequestProperty("X-Api-Key", api_key);
            android.util.Log.d("http", "connected: " + url);
            urlc.setAllowUserInteraction(false);
            urlc.connect();

            StringBuffer response = new StringBuffer();
            //get result
            BufferedReader br = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
            String l;
            while ((l=br.readLine())!=null) {
                response.append(l);
            }
            /* extracting quote from response */
            JSONObject jsonObj = new JSONObject(response.toString().substring(response.indexOf("{"), response.lastIndexOf("}")+1));
            br.close();
            urlc.disconnect();
            return jsonObj;
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }


    /********** SHARED PREF METHODS **********/

    /* add to shared preference */
    void addToSharedPref(Log log, SharedPreferences sharedPreferences){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        try {
            editor.putString(log.getID().toString(), formattedLog(log));
            editor.apply();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /* print to logcat. Filter with 'pref_data' tag name
    * only available on create log */
    public void createPersistentLogs(){
        SharedPreferences pref = getSharedPref(MainActivity.this);

        Map<String, ?> allData = pref.getAll();
        for( Map.Entry<String, ?> entry: allData.entrySet() ){
            String formattedString = (String)entry.getValue();
            Log newLog = new Log();
            newLog.setTitle(formattedString.substring(formattedString.indexOf('{')+1,formattedString.indexOf('}')));
            newLog.setBody(formattedString.substring(formattedString.indexOf('[')+1,formattedString.indexOf(']')));
            Log.allLogs.add(newLog);
        }
        Log.sortedLogs();
        for(Log l: Log.reverseSortedLogs ){
            android.util.Log.d("logs", l.getTitle() + " : " + l.getBody() );
        }
    }

    /* return shared preference */
    public static SharedPreferences getSharedPref(Context context){
        return context.getSharedPreferences(Integer.toString(R.string.shared_pref_key),Context.MODE_PRIVATE);
    }

    /* formats log contents for shared preference insertion and retrieval via substring extraction */
    String formattedLog(Log log){
        return "{"+log.getTitle()+"}["+log.getBody()+"]";
    }


}
