package com.marianhello.bgloc.data;

import android.content.ContentResolver;
import android.content.UriMatcher;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by finch on 21/07/16.
 */
public class LocationContract {
    public static final String AUTHORITY = "com.marianhello.bgloc.data.locations";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    /**
     * Constants for the Location table
     * of the location provider.
     */
    public static final class Locations implements BaseColumns {
        public static final String TIME = "time";
        public static final String ACCURACY = "accuracy";
        public static final String SPEED = "speed";
        public static final String BEARING = "bearing";
        public static final String ALTITUDE = "altitude";
        public static final String LATITUDE = "latitude";
        public static final String LONGITUDE = "longitude";
        public static final String PROVIDER = "provider";
        public static final String LOCATION_PROVIDER = "service_provider";
        public static final String VALID = "valid";
        /**
         * The content URI for this table.
         */
        public static final Uri CONTENT_URI =
                Uri.withAppendedPath(LocationContract.CONTENT_URI, "locations");

        /**
         * The mime type of a directory of items.
         */
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.marianhello.bgloc.data.locations";

        /**
         * The mime type of a single item.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.marianhello.bgloc.data.locations";

        /**
         * A projection of all columns
         * in the items table.
         */
        public static final String[] PROJECTION_ALL = {_ID, TIME, ACCURACY, SPEED, BEARING, ALTITUDE, LATITUDE, LONGITUDE, PROVIDER, LOCATION_PROVIDER, VALID};
        /**
         * The default sort order for
         * queries containing NAME fields.
         */
        public static final String SORT_ORDER_DEFAULT = TIME + " ASC";

    }


}
