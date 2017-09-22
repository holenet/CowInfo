package com.holenet.cowinfo;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;

public class DatabaseHelper extends SQLiteOpenHelper {
    final static String propTable = "prop_lists";
    final static String detailTable = "detail_lists";
    final static int databaseVersion = 3;

    public DatabaseHelper(Context context) {
        super(context, context.getExternalFilesDir(null).toString() + File.separator + "cows.db", null, databaseVersion);
//            Toast.makeText(getApplicationContext(), "Constructor", Toast.LENGTH_SHORT).show();
    }
    public void onCreate(SQLiteDatabase db) {
//            Toast.makeText(getApplicationContext(), "onCreate", Toast.LENGTH_SHORT).show();
/*
            try {
                String DROP_SQL = "drop table if exists " + tableName;
                db.execSQL(DROP_SQL);
            } catch(Exception ex) {
            }
*/
        String CREATE_SQL = "create table " + propTable + "("
                + " _id integer PRIMARY KEY autoincrement, "
                + " number text, "
                + " female text, "
                + " year int, "
                + " month int, "
                + " day int, "
                + " mnumber text)";
        try {
            db.execSQL(CREATE_SQL);
        } catch(Exception ex) {
//            Toast.makeText(getApplicationContext(), "create prop_table failed", Toast.LENGTH_SHORT).show();
        }

        CREATE_SQL = "create table " + detailTable + "("
                + " _id integer PRIMARY KEY autoincrement, "
                + " prop_id int, "
                + " content text, "
                + " etc text, "
                + " year int, "
                + " month int, "
                + " day int)";
        try {
            db.execSQL(CREATE_SQL);
        } catch(Exception ex) {
//            Toast.makeText(getApplicationContext(), "create detail_table failed", Toast.LENGTH_SHORT).show();
        }
    }
    public void onOpen(SQLiteDatabase db) {
//            Toast.makeText(getApplicationContext(), "onOpen", Toast.LENGTH_SHORT).show();
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//            Toast.makeText(getApplicationContext(), "onUpgrade", Toast.LENGTH_SHORT).show();
        db.execSQL("drop table if exists " + propTable);
        db.execSQL("drop table if exists " + detailTable);
        onCreate(db);
    }
}
