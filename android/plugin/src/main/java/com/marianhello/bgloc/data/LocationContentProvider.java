package com.marianhello.bgloc.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.marianhello.bgloc.data.sqlite.SQLiteLocationContract;
import com.marianhello.bgloc.data.sqlite.SQLiteOpenHelper;

public class LocationContentProvider extends ContentProvider {

    private static final int LOCATION_LIST = 1;
    private static final int LOCATION_ID = 2;
    private static final UriMatcher URI_MATCHER;

    private SQLiteOpenHelper dbHelper = null;

    // prepare the UriMatcher
    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(LocationContract.AUTHORITY, "locations", LOCATION_LIST);
        URI_MATCHER.addURI(LocationContract.AUTHORITY, "locations/#", LOCATION_ID);
    }

    /*
     * Always return true, indicating that the
     * provider loaded correctly.
     */
    @Override
    public boolean onCreate() {
        dbHelper = SQLiteOpenHelper.getHelper(getContext());
        return true;
    }

    /*
     * Return no type for MIME type
     */
    @Override
    public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case LOCATION_LIST:
                return LocationContract.Locations.CONTENT_TYPE;
            case LOCATION_ID:
                return LocationContract.Locations.CONTENT_ITEM_TYPE;
        }

        return null;
    }

    /*
     * query() always returns no results
     *
     */
    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        boolean useAuthorityUri = false;

        switch (URI_MATCHER.match(uri)) {
            case LOCATION_LIST:
                builder.setTables(SQLiteLocationContract.LocationEntry.TABLE_NAME);
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = LocationContract.Locations.SORT_ORDER_DEFAULT;
                }
                break;
            case LOCATION_ID:
                builder.setTables(SQLiteLocationContract.LocationEntry.TABLE_NAME);
                // limit query to one row at most:
                builder.appendWhere(LocationContract.Locations._ID + " = " + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        Cursor cursor = builder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        if (useAuthorityUri) {
            //noop
        } else {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }

        return cursor;
    }

    /*
     * insert() always returns null (no URI)
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (URI_MATCHER.match(uri) == URI_MATCHER.NO_MATCH) {
            throw new IllegalArgumentException("Unsupported URI for insertion: " + uri);
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (URI_MATCHER.match(uri) == LOCATION_LIST) {
            long id = db.insert(SQLiteLocationContract.LocationEntry.TABLE_NAME, null, values);
            return getUriForId(id, uri);
        }

        throw new IllegalArgumentException("Unsupported URI for insertion: " + uri);
    }

    /*
     * delete() always returns "no rows affected" (0)
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int delCount = 0;

        switch (URI_MATCHER.match(uri)) {
            case LOCATION_LIST:
                delCount = db.delete(
                        SQLiteLocationContract.LocationEntry.TABLE_NAME,
                        selection,
                        selectionArgs);
                break;
            case LOCATION_ID:
                String idStr = uri.getLastPathSegment();
                String where = LocationContract.Locations._ID + " = " + idStr;
                if (!TextUtils.isEmpty(selection)) {
                    where += " AND " + selection;
                }
                delCount = db.delete(
                        SQLiteLocationContract.LocationEntry.TABLE_NAME,
                        where,
                        selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        // notify all listeners of changes:
        if (delCount > 0 && !isInBatchMode()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return delCount;
    }

    /*
     * update() always returns "no rows affected" (0)
     */
    public int update(
            Uri uri,
            ContentValues values,
            String selection,
            String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int updateCount = 0;
        switch (URI_MATCHER.match(uri)) {
            case LOCATION_LIST:
                updateCount = db.update(
                        SQLiteLocationContract.LocationEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            case LOCATION_ID:
                String idStr = uri.getLastPathSegment();
                String where = LocationContract.Locations._ID + " = " + idStr;
                if (!TextUtils.isEmpty(selection)) {
                    where += " AND " + selection;
                }
                updateCount = db.update(
                        SQLiteLocationContract.LocationEntry.TABLE_NAME,
                        values,
                        where,
                        selectionArgs);
                break;
            default:
                // no support for updating photos or entities!
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        // notify all listeners of changes:
        if (updateCount > 0 && !isInBatchMode()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return updateCount;
    }

    private boolean isInBatchMode() {
        //TODO: implement
        return false;
    }

    private Uri getUriForId(long id, Uri uri) {
        if (id > 0) {
            Uri itemUri = ContentUris.withAppendedId(uri, id);
            if (!isInBatchMode()) {
                // notify all listeners of changes and return itemUri:
                getContext().getContentResolver().notifyChange(itemUri, null);
            }
            return itemUri;
        }
        // s.th. went wrong:
        throw new SQLException("Problem while inserting into uri: " + uri);
    }
}

