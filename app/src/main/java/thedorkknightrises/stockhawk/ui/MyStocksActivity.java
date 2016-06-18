package thedorkknightrises.stockhawk.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;

import thedorkknightrises.stockhawk.R;
import thedorkknightrises.stockhawk.data.QuoteColumns;
import thedorkknightrises.stockhawk.data.QuoteProvider;
import thedorkknightrises.stockhawk.rest.QuoteCursorAdapter;
import thedorkknightrises.stockhawk.rest.RecyclerViewItemClickListener;
import thedorkknightrises.stockhawk.rest.Utils;
import thedorkknightrises.stockhawk.service.StockIntentService;
import thedorkknightrises.stockhawk.service.StockTaskService;
import thedorkknightrises.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    private static final int CURSOR_LOADER_ID = 0;
    private static android.support.design.widget.CoordinatorLayout coordinatorLayout;
    private static SwipeRefreshLayout swipeRefreshLayout;
    private BroadcastReceiver receiver;
    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private Intent mServiceIntent;
    private QuoteCursorAdapter mCursorAdapter;
    private Context mContext;
    private Cursor mCursor;

    public static android.support.design.widget.CoordinatorLayout getCoordinatorLayout() {
        return coordinatorLayout;
    }

    public static Boolean isConnected(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    private void update() {
        // Run the initialize task service so that some stocks appear upon an empty database
        mServiceIntent.putExtra("tag", "init");
        startService(mServiceIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        setContentView(R.layout.activity_my_stocks);

        coordinatorLayout = (android.support.design.widget.CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        mServiceIntent = new Intent(getApplicationContext(), StockIntentService.class);
        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately
        if (savedInstanceState == null) {
            if (isConnected(this))
                update();
            else
                Snackbar.make(coordinatorLayout, getString(R.string.network_toast), Snackbar.LENGTH_LONG).show();

        }
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        mCursorAdapter = new QuoteCursorAdapter(this, null);
        recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        mCursor.moveToPosition(position);
                        Intent i = new Intent(MyStocksActivity.this, Detail.class);
                        i.putExtra("name", mCursor.getString(2));
                        i.putExtra("symbol", mCursor.getString(1));
                        i.putExtra("price", mCursor.getString(3));
                        i.putExtra("change", mCursor.getString(5));
                        i.putExtra("perc", mCursor.getString(4));
                        startActivity(i);
                    }
                }));
        recyclerView.setAdapter(mCursorAdapter);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeResources(R.color.accent);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    if (isConnected(getApplicationContext()))
                        update();
                    else Snackbar.make(coordinatorLayout, R.string.error, Snackbar.LENGTH_LONG)
                            .show();
                    swipeRefreshLayout.setRefreshing(false);
                }
            });
        }

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        mTitle = getTitle();
        if (isConnected(getApplicationContext())) {
            long period = 3600L;
            long flex = 10L;
            String periodicTag = "periodic";

            // create a periodic task to pull stocks once every hour after the app has been opened. This
            // is so Widget data stays up to date.
            PeriodicTask periodicTask = new PeriodicTask.Builder()
                    .setService(StockTaskService.class)
                    .setPeriod(period)
                    .setFlex(flex)
                    .setTag(periodicTag)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setRequiresCharging(false)
                    .build();
            // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
            // are updated.
            GcmNetworkManager.getInstance(this).schedule(periodicTask);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);

        IntentFilter i = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (isInitialStickyBroadcast()) {
                    // Ignore this call to onReceive, as this is the sticky broadcast
                } else {
                    // Connectivity state has changed
                    if (isConnected(getBaseContext())) {
                        Snackbar snackbar = Snackbar.make(coordinatorLayout, R.string.conn, Snackbar.LENGTH_LONG);
                        snackbar.setAction(R.string.refresh, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                update();
                            }
                        });
                        snackbar.show();
                    } else {
                        Snackbar.make(coordinatorLayout, R.string.no_conn, Snackbar.LENGTH_LONG).show();
                    }
                }
            }
        };
        registerReceiver(receiver, i);
    }

    @Override
    protected void onPause() {
        unregisterReceiver(receiver);
        super.onPause();
    }

    private void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);
        restoreActionBar();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_change_units) {
            // this is for changing stock changes from percent value to dollar value
            Utils.showPercent = !Utils.showPercent;
            this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.NAME, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);
        mCursor = data;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    public void onFabClick(View v) {
        if (isConnected(getApplicationContext())) {
            new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
                    .content(R.string.content_test)
                    .inputType(InputType.TYPE_CLASS_TEXT)
                    .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                        @Override
                        public void onInput(MaterialDialog dialog, CharSequence input) {
                            // On FAB click, receive user input. Make sure the stock doesn't already exist
                            // in the DB and proceed accordingly
                            Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                    new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                                    new String[]{input.toString()}, null);
                            if (c.getCount() != 0) {
                                Toast toast =
                                        Toast.makeText(MyStocksActivity.this, "This stock is already saved!",
                                                Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                                toast.show();
                            } else {
                                // Add the stock to DB
                                mServiceIntent.putExtra("tag", "add");
                                mServiceIntent.putExtra("symbol", input.toString());
                                startService(mServiceIntent);
                            }
                            c.close();
                        }
                    })
                    .show();
        } else {
            Snackbar.make(coordinatorLayout, getString(R.string.fab_error), Snackbar.LENGTH_LONG).show();
        }
    }

}
