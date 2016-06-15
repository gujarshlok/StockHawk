package thedorkknightrises.stockhawk.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.view.LineChartView;
import thedorkknightrises.stockhawk.R;
import thedorkknightrises.stockhawk.service.StockHistory;

/**
 * Created by samri_000 on 5/23/2016.
 */
public class Detail extends AppCompatActivity {
    public static LineChartView lineChartView;
    private static View buttonOne;
    private static View buttonSix;
    private static View buttonYear;
    private String name;
    private String symbol;
    private String price;
    private String change;
    private String perc;

    public static void updateChart(ArrayList arrayList, Context context) {

        //In most cased you can call data model methods in builder-pattern-like manner.
        Line line = new Line(arrayList).setColor(context.getResources().getColor(R.color.accent)).setHasLabelsOnlyForSelected(true);
        List<Line> lines = new ArrayList<Line>();
        lines.add(line);

        LineChartData data = new LineChartData();
        data.setLines(lines);

        lineChartView.setLineChartData(data);
        lineChartView.setVisibility(View.VISIBLE);

        SharedPreferences pref = context.getSharedPreferences("Prefs", MODE_PRIVATE);
        String range = pref.getString("range", "1m");
        buttonOne.setBackgroundColor(Color.parseColor("#424242"));
        buttonSix.setBackgroundColor(Color.parseColor("#424242"));
        buttonYear.setBackgroundColor(Color.parseColor("#424242"));
        switch (range) {
            case "1m":
                buttonOne.setBackgroundColor(Color.parseColor("#116622"));
                break;
            case "6m":
                buttonSix.setBackgroundColor(Color.parseColor("#116622"));
                break;
            case "1y":
                buttonYear.setBackgroundColor(Color.parseColor("#116622"));
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);

        lineChartView = (LineChartView) findViewById(R.id.linechart);

        Bundle bundle = getIntent().getExtras();
        name = bundle.getString("name", getResources().getString(R.string.name));
        symbol = bundle.getString("symbol", "-");

        buttonOne = findViewById(R.id.one);
        buttonSix = findViewById(R.id.six);
        buttonYear = findViewById(R.id.year);

        new StockHistory(getApplicationContext(), symbol).execute();

        price = getResources().getString(R.string.price) + ":\t" + bundle.getString("price", "-");
        change = getResources().getString(R.string.change) + ":\t" + bundle.getString("change", "-");
        perc = getResources().getString(R.string.change_perc) + ":\t" + bundle.getString("perc", "-");

        ((TextView) findViewById(R.id.stock_name)).setText(name);
        ((TextView) findViewById(R.id.stock_symbol)).setText(getResources().getString(R.string.symbol) + ":\t" + symbol);
        ((TextView) findViewById(R.id.stock_price)).setText(price);
        ((TextView) findViewById(R.id.stock_change)).setText(change);
        ((TextView) findViewById(R.id.stock_change_perc)).setText(perc);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(name);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    public void onButtonClick(View v) {
        SharedPreferences pref = getSharedPreferences("Prefs", MODE_PRIVATE);
        String range = (String) ((Button) v).getText();
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("range", range);
        editor.commit();
        new StockHistory(getApplicationContext(), symbol).execute();
    }

}
