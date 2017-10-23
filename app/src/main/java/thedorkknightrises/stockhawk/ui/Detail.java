package thedorkknightrises.stockhawk.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
public class Detail extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private static LineChartView lineChartView;
    private EditText share_bought,share_sold;
    private String symbol;
    private String share_price_bought,share_price_sold;
    private float price_product,number_of_shares_bought=0,number_of_shares_sold=0;
    public static float customer_wallet=10000;
    private Button purchase_button,sell_button;
    private TextView money;

    public static void updateChart(ArrayList arrayList, Context context) {

        //In most cased you can call data model methods in builder-pattern-like manner.
        Line line = new Line(arrayList).setColor(context.getResources().getColor(R.color.accent)).setHasLabelsOnlyForSelected(true);
        List<Line> lines = new ArrayList<>();
        lines.add(line);

        LineChartData data = new LineChartData();
        data.setLines(lines);

        lineChartView.setLineChartData(data);
        lineChartView.setVisibility(View.VISIBLE);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);

        lineChartView = (LineChartView) findViewById(R.id.linechart);
        share_bought=(EditText)findViewById(R.id.share_bought);
        share_sold=(EditText)findViewById(R.id.share_sold);
        money=(TextView)findViewById(R.id.money);
        purchase_button=(Button)findViewById(R.id.purchase_button);
        sell_button=(Button)findViewById(R.id.sell_button);
        Bundle bundle = getIntent().getExtras();
        String name = bundle.getString("name", getResources().getString(R.string.name));
        symbol = bundle.getString("symbol", "-");

        new StockHistory(getApplicationContext(), symbol).execute();

        String price = getResources().getString(R.string.price) + ":\t" + bundle.getString("price", "-");
        String price1=bundle.getString("price","-");
        Log.e("Tag",price1);
        price_product=Float.parseFloat(price1);
        String change = getResources().getString(R.string.change) + ":\t" + bundle.getString("change", "-");
        String perc = getResources().getString(R.string.change_perc) + ":\t" + bundle.getString("perc", "-");

        ((TextView) findViewById(R.id.stock_name)).setText(name);
        ((TextView) findViewById(R.id.stock_symbol)).setText(getResources().getString(R.string.symbol) + ":\t" + symbol);
        ((TextView) findViewById(R.id.stock_price)).setText(price);
        ((TextView) findViewById(R.id.stock_change)).setText(change);
        ((TextView) findViewById(R.id.stock_change_perc)).setText(perc);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        Spinner sp= (Spinner) findViewById(R.id.spinner_range);
        sp.setOnItemSelectedListener(this);
        purchase_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                share_price_bought=share_bought.getText().toString();
                number_of_shares_bought=Float.parseFloat(share_price_bought);
                if(customer_wallet<price_product*number_of_shares_bought)
                {
                    Toast.makeText(getApplicationContext(),"Insufficient Balance",Toast.LENGTH_SHORT).show();
                }
                else
                {
                    customer_wallet=customer_wallet-price_product*number_of_shares_bought;
                    String customer_wallet2=Float.toString(customer_wallet);
                    Toast.makeText(getApplicationContext(),"Purchase Successful",Toast.LENGTH_SHORT).show();
                    money.setText(customer_wallet2);
                }
            }
        });
        sell_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                share_price_sold=share_sold.getText().toString();
                number_of_shares_sold=Float.parseFloat(share_price_sold);
                if(number_of_shares_sold>number_of_shares_bought)
                {
                    Toast.makeText(getApplicationContext(), "Haven't bought that many shares", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    customer_wallet=customer_wallet+price_product*number_of_shares_sold;
                    String customer_wallet3=Float.toString(customer_wallet);
                    Toast.makeText(getApplicationContext(),"Sold Successfully",Toast.LENGTH_SHORT).show();
                    money.setText(customer_wallet3);
                }
            }
        });
        ArrayList<String> elements= new ArrayList<>();
        elements.add(getString(R.string.one_ex));
        elements.add(getString(R.string.six_ex));
        elements.add(getString(R.string.year_ex));

        ArrayAdapter<String> spAdapter= new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, elements);
        sp.setAdapter(spAdapter);

        if (!MyStocksActivity.isConnected(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), R.string.no_history, Toast.LENGTH_SHORT).show();
        }

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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        SharedPreferences pref= getSharedPreferences("Prefs", MODE_PRIVATE);
        String item = parent.getItemAtPosition(position).toString();
        SharedPreferences.Editor e = pref.edit();

        if(parent.equals(findViewById(R.id.spinner_range))) {
            if (item.equals(getString(R.string.one_ex)))
                e.putString("range", "1m");
            else if (item.equals(getString(R.string.six_ex)))
                e.putString("range", "6m");
            else if (item.equals(getString(R.string.year_ex)))
                e.putString("range", "1y");
            e.putInt("pos", position);
        }
        e.commit();

        new StockHistory(getApplicationContext(), symbol).execute();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    protected void onResume() {
        super.onResume();

        Spinner sp= (Spinner) findViewById(R.id.spinner_range);

        SharedPreferences pref= getSharedPreferences("Prefs", MODE_PRIVATE);
        int pos= pref.getInt("pos", 1);
        sp.setSelection(pos);
    }

}
