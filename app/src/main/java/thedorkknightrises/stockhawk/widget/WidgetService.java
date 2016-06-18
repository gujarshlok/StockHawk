package thedorkknightrises.stockhawk.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import thedorkknightrises.stockhawk.R;
import thedorkknightrises.stockhawk.data.QuoteColumns;
import thedorkknightrises.stockhawk.data.QuoteProvider;

/**
 * Created by Samriddha Basu on 6/18/2016.
 */
public class WidgetService extends RemoteViewsService {
    private Cursor cursor;
    @Override
    public RemoteViewsService.RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ListRemoteViewsFactory(this.getApplicationContext(), intent);
    }
    class ListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
        private final Context context;

        public ListRemoteViewsFactory(Context applicationContext, Intent intent) {
            context = applicationContext;
        }

        @Override
        public void onCreate() {
            cursor = context.getContentResolver().query(
                    QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{QuoteColumns._ID, //0
                            QuoteColumns.SYMBOL, //1
                            QuoteColumns.BIDPRICE, //2
                            QuoteColumns.CHANGE, //3
                            QuoteColumns.ISUP,  //4
                            QuoteColumns.NAME}, //5
                    QuoteColumns.ISCURRENT + " = ?",
                    new String[]{"1"},
                    null
            );
        }

        @Override
        public void onDataSetChanged() {
            cursor = context.getContentResolver().query(
                    QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{QuoteColumns._ID, //0
                            QuoteColumns.SYMBOL, //1
                            QuoteColumns.BIDPRICE, //2
                            QuoteColumns.PERCENT_CHANGE, //3
                            QuoteColumns.ISUP,  //4
                            QuoteColumns.NAME}, //5
                    QuoteColumns.ISCURRENT + " = ?",
                    new String[]{"1"},
                    null
            );
        }

        @Override
        public void onDestroy() {
            if (cursor != null)
                cursor.close();
            Log.d("DEBUG", "Cursor destroyed");
        }

        @Override
        public int getCount() {
            if (cursor != null)
                return cursor.getCount();
            return 0;
        }

        @Override
        public RemoteViews getViewAt(int position) {
            RemoteViews remoteViews = new RemoteViews(this.context.getPackageName(), R.layout.list_item_widget);

            if (cursor.moveToPosition(position)) {
                String symbol = cursor.getString(1);
                remoteViews.setTextViewText(R.id.stock_symbol, symbol);
                remoteViews.setTextViewText(R.id.bid_price, cursor.getString(2));
                remoteViews.setTextViewText(R.id.change, cursor.getString(3));
                remoteViews.setTextViewText(R.id.stock_name, cursor.getString(5));

                if (cursor.getInt(4) == 1) {
                    remoteViews.setInt(R.id.change, "setBackgroundResource",
                            R.drawable.percent_change_pill_green);
                } else {
                    remoteViews.setInt(R.id.change, "setBackgroundResource",
                            R.drawable.percent_change_pill_red);
                }
                Bundle extras = new Bundle();
                extras.putString(AppWidget.EXTRA, symbol);
                Intent fillInIntent = new Intent();
                fillInIntent.putExtras(extras);
                // Make it possible to distinguish the individual on-click
                // action of a given item
                remoteViews.setOnClickFillInIntent(R.id.list_item, fillInIntent);
            }

            return remoteViews;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        public Cursor getCursor(){
            return cursor;
        }

        @Override public long getItemId(int position) {
            if (cursor != null){
                return cursor.getLong(0);
            }
            return 0;
        }

    }

}