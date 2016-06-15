package thedorkknightrises.stockhawk.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import lecho.lib.hellocharts.model.PointValue;
import thedorkknightrises.stockhawk.ui.Detail;

/**
 * Created by samri_000 on 6/14/2016.
 */
public class StockHistory extends AsyncTask<Void, Void, ArrayList> {
    private String LOG_TAG = StockHistory.class.getSimpleName();
    private OkHttpClient client = new OkHttpClient();
    private Context mContext;
    private String range;
    private ArrayList array;
    private String urlString;
    private String URL_START = "http://chartapi.finance.yahoo.com/instrument/1.0/";
    private String URL_SECTION = "/chartdata;type=quote;range=";
    private String URL_END = "/json";

    public StockHistory(Context context, String symbol) {
        mContext = context;
        this.array = new ArrayList();
        SharedPreferences pref = mContext.getSharedPreferences("Prefs", Context.MODE_PRIVATE);
        range = pref.getString("range", "1m");
        urlString = URL_START + symbol + URL_SECTION + range + URL_END;

    }

    String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    @Override
    protected ArrayList doInBackground(Void... params) {
        try {
            Request request = new Request.Builder()
                    .url(urlString)
                    .build();

            Response response = client.newCall(request).execute();
            String getResponse = response.body().string();
            try {
                String json = getResponse.substring(getResponse.indexOf("(") + 1, getResponse.lastIndexOf(")"));
                JSONObject mainObject = new JSONObject(json);
                JSONArray series_data = mainObject.getJSONArray("series");
                float last = 0;
                int i;
                for (i = 0; i < series_data.length(); ) {
                    JSONObject singleObject = series_data.getJSONObject(i);
                    String data = singleObject.getString("close");
                    float close = Float.parseFloat(data);
                    array.add(new PointValue(i, close));
                    switch (range) {
                        case "1y":
                            i += 12;
                            break;
                        case "6m":
                            i += 6;
                            break;
                        default:
                            i++;
                            break;
                    }
                    last = close;
                }
                JSONObject singleObject = series_data.getJSONObject(series_data.length() - 1);
                String data = singleObject.getString("close");
                float close = Float.parseFloat(data);
                if (last != close)
                    array.add(new PointValue(i, close));

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return array;
    }

    @Override
    protected void onPostExecute(ArrayList array) {
        Detail.updateChart(array, mContext);
    }
}
