package thedorkknightrises.stockhawk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.widget.RemoteViews;

import thedorkknightrises.stockhawk.R;
import thedorkknightrises.stockhawk.data.QuoteColumns;
import thedorkknightrises.stockhawk.data.QuoteProvider;
import thedorkknightrises.stockhawk.service.StockIntentService;
import thedorkknightrises.stockhawk.ui.Detail;
import thedorkknightrises.stockhawk.ui.MyStocksActivity;

/**
 * Implementation of App Widget functionality.
 */
public class AppWidget extends AppWidgetProvider {
    private static final String TAP_ACTION = "com.example.android.stackwidget.TAP_ACTION";
    public static final String EXTRA = "com.example.android.stackwidget.EXTRA";


    // Called when the BroadcastReceiver receives an Intent broadcast.
    // Checks to see whether the intent's action is TOAST_ACTION. If it is, the app widget
    // displays a Toast message for the current item.
    @Override
    public void onReceive(Context context, Intent intent) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        if (intent.getAction().equals(TAP_ACTION)) {
            String symbol = intent.getStringExtra(EXTRA);
            Cursor cursor = context.getContentResolver().query(
                    QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{QuoteColumns.SYMBOL, //0
                            QuoteColumns.BIDPRICE, //1
                            QuoteColumns.PERCENT_CHANGE, //2
                            QuoteColumns.CHANGE,  //3
                            QuoteColumns.NAME}, //4
                    QuoteColumns.ISCURRENT + " = ?",
                    new String[]{"1"},
                    null
            );
            if (cursor.moveToFirst())
            for (int j= 0; j< cursor.getCount(); j++) {
                if (cursor.getString(0).equals(symbol)) {
                    Intent i = new Intent(context, Detail.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    i.putExtra("name", cursor.getString(4));
                    i.putExtra("symbol", symbol);
                    i.putExtra("price", cursor.getString(1));
                    i.putExtra("change", cursor.getString(3));
                    i.putExtra("perc", cursor.getString(2));
                    context.startActivity(i);
                }
                else cursor.moveToNext();
            }
            cursor.close();
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Intent mServiceIntent = new Intent(context, StockIntentService.class);
        mServiceIntent.putExtra("tag", "init");
        context.startService(mServiceIntent);

        // update each of the app widgets with the remote adapter
        for (int appWidgetId : appWidgetIds) {

            // Set up the intent that starts the WidgetService, which will
            // provide the views for this collection.
            Intent intent = new Intent(context, WidgetService.class);
            // Add the app widget ID to the intent extras.
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            // Instantiate the RemoteViews object for the app widget layout.
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.layout_widget);
            // Set up the RemoteViews object to use a RemoteViews adapter.
            // This adapter connects
            // to a RemoteViewsService  through the specified intent.
            // This is how you populate the data.
            rv.setRemoteAdapter(R.id.list_view, intent);

            // The empty view is displayed when the collection has no items.
            // It should be in the same layout used to instantiate the RemoteViews
            // object above.
            rv.setEmptyView(R.id.list_view, R.id.textView);

            // Create an Intent to launch MyStocksActivity
            Intent launchIntent = new Intent(context, MyStocksActivity.class);
            PendingIntent launchIntentPending = PendingIntent.getActivity(context, 0, launchIntent, 0);

            rv.setOnClickPendingIntent(R.id.openAppButton, launchIntentPending);

            Intent refreshIntent = new Intent(context, AppWidget.class);
            refreshIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                    0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.refresh, pendingIntent);

            // This section makes it possible for items to have individualized behavior.
            // It does this by setting up a pending intent template. Individuals items of a collection
            // cannot set up their own pending intents. Instead, the collection as a whole sets
            // up a pending intent template, and the individual items set a fillInIntent
            // to create unique behavior on an item-by-item basis.
            Intent tapIntent = new Intent(context, AppWidget.class);
            // Set the action for the intent.
            // When the user touches a particular view, it will have the effect of
            // broadcasting TOAST_ACTION.
            tapIntent.setAction(AppWidget.TAP_ACTION);
            tapIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            PendingIntent tapPendingIntent = PendingIntent.getBroadcast(context, 0, tapIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setPendingIntentTemplate(R.id.list_view, tapPendingIntent);

            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_view);
            appWidgetManager.updateAppWidget(appWidgetId, rv);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

