package com.deepwits.Patron.DataBase;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.deepwits.Patron.DataBase.DBOpenHelper;
import com.deepwits.Patron.DataBase.MediaFileDAOImpl;
import com.deepwits.Patron.StorageManager.StorageManager;

public class RecorderProvider extends ContentProvider {

    private static final String TAG="RecorderProvider";
    private static UriMatcher uriMatcher = null;
    private static final int COLLECTION_INDICATOR = 1;
    private static final int SINGLE_INDICATOR = 2;
    private static final String MF_TABLE_NAME = RecorderProviderMetaData.MediaFileMetaData.TABLE_NAME;
    private DBOpenHelper dbHelper;
    private SQLiteDatabase db;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(RecorderProviderMetaData.AUTHORITY,
                MF_TABLE_NAME, COLLECTION_INDICATOR);
        uriMatcher.addURI(RecorderProviderMetaData.AUTHORITY,
                MF_TABLE_NAME+"/#", SINGLE_INDICATOR);
    }
    MediaFileDAOImpl mediaFileDAO = null;
    StorageManager storageManager = null;
    public RecorderProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = -1;
        try{
            db = dbHelper.getWritableDatabase();
            switch (uriMatcher.match(uri)){
                case COLLECTION_INDICATOR:
                    return db.delete(MF_TABLE_NAME,selection,selectionArgs);
                case SINGLE_INDICATOR:
                    long id = ContentUris.parseId(uri);
                    String where_value = "id="+id;
                    if(selection != null && !selection.equals("")){
                        where_value += " and " + selection;
                    }
                    count = db.delete(MF_TABLE_NAME,where_value,selectionArgs);
                default:
                    throw new IllegalArgumentException("This is a unKnow Uri"
                            + uri.toString());

            }
        }catch (Exception e){

        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        // TODO: Implement this to handle requests for the MIME type of the data
        // at the given URI.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long rowId = 0;
        Uri insertUri = null;

        switch (uriMatcher.match(uri)) {
            case COLLECTION_INDICATOR:
                db = dbHelper.getWritableDatabase();//取得数据库操作实例

                //执行添加，返回行号，如果主健字段是自增长的，那么行号会等于主键id
                rowId = db.insert(MF_TABLE_NAME, null, values);
                insertUri = Uri.withAppendedPath(uri, "/" + rowId);
                Log.i(TAG, "insertUri:" + insertUri.toString());
                //发出数据变化通知(MediaFile表的数据发生变化)
                getContext().getContentResolver().notifyChange(uri, null);
                break;

            default:
                //不能识别uri
                throw new IllegalArgumentException("This is a unKnow Uri"
                        + uri.toString());
        }
        return insertUri;
    }

    @Override
    public boolean onCreate() {
        dbHelper = new DBOpenHelper(getContext(),RecorderProviderMetaData.DB_NAME, null, 1);
        return (dbHelper == null) ? false : true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        db = dbHelper.getReadableDatabase();
        switch (uriMatcher.match(uri)) {
            case COLLECTION_INDICATOR:
                return db.query(MF_TABLE_NAME,
                        projection, selection, selectionArgs, null, null,
                        sortOrder);
            case SINGLE_INDICATOR:
                long id = ContentUris.parseId(uri);
                String where_value = "id=" + id;
                if (selection != null && !"".equals(selection)) {
                    where_value = selection + " and " + where_value;
                }
                return db.query(MF_TABLE_NAME, projection, where_value, selectionArgs, null,
                        null, sortOrder);

            default:
                throw new IllegalArgumentException("This is a unKnow Uri"
                        + uri.toString());
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        db = dbHelper.getWritableDatabase();
        switch (uriMatcher.match(uri)) {
            case COLLECTION_INDICATOR:
                return db.update(MF_TABLE_NAME, values, null, null);
            case SINGLE_INDICATOR:
                long id = ContentUris.parseId(uri);
                String where_value = "id=" + id;
                if (selection != null && !"".equals(selection)) {
                    where_value = selection + " and " + where_value;
                }
                return db.update(MF_TABLE_NAME, values, selection, selectionArgs);

            default:
                throw new IllegalArgumentException("This is a unKnow Uri"
                        + uri.toString());
        }
    }
}
