package com.holenet.cowinfo;

import android.app.ActionBar;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

import static com.holenet.cowinfo.RecodeActivity.MENU_DELETE;
import static com.holenet.cowinfo.RecodeActivity.MENU_MODIFY;
import static com.holenet.cowinfo.RecodeActivity.REQUEST_COW_INFO;
import static com.holenet.cowinfo.RecodeActivity.REQUEST_COW_MODIFY;
import static com.holenet.cowinfo.RecodeActivity.REQUEST_RECODE_ADD;
import static com.holenet.cowinfo.RecodeActivity.REQUEST_RECODE_MODIFY;

public class CowActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;

    int id;
    String number;
    boolean female;
    int[] birthday;
    String mnumber;

    TextView tVnumber, tVmnumber, tVbirthday;
    ListView lVrecs;
    RecodeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cow);
        dbHelper = new DatabaseHelper(this);

        Intent intent = getIntent();
        id = intent.getIntExtra("id", -1);

        tVnumber = (TextView) findViewById(R.id.tVnumber);
        tVmnumber = (TextView) findViewById(R.id.tVmnumber);
        tVbirthday = (TextView) findViewById(R.id.tVbirthday);

        lVrecs = (ListView) findViewById(R.id.lVrecs);
        lVrecs.setEmptyView(findViewById(R.id.tVempty));
        adapter = new RecodeAdapter(this, R.layout.item_rec, new ArrayList<RecodeAdapter.Item>());
        registerForContextMenu(lVrecs);
        lVrecs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RecodeAdapter.Item recItem = adapter.getItem(position);
                Intent intent = new Intent(getApplicationContext(), RecodeEditActivity.class);
                intent.putExtra("id", recItem.getId());
                intent.putExtra("prop_id", id);
                intent.putExtra("requestCode", REQUEST_RECODE_MODIFY);
                startActivityForResult(intent, REQUEST_RECODE_MODIFY);
            }
        });

        refresh();
    }

    public void pad(View v) {
        Intent intent = new Intent(getApplicationContext(), RecodeEditActivity.class);
        intent.putExtra("prop_id", id);
        intent.putExtra("requestCode", REQUEST_RECODE_ADD);
        startActivityForResult(intent, REQUEST_RECODE_ADD);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, MENU_MODIFY, Menu.NONE, "수정");
        menu.add(0, MENU_DELETE, Menu.NONE, "삭제");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int index;
        final RecodeAdapter.Item recItem;
        switch (item.getItemId()) {
            case MENU_MODIFY:
                index = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;
                recItem = adapter.getItem(index);
                Intent intent = new Intent(getApplicationContext(), RecodeEditActivity.class);
                intent.putExtra("id", recItem.getId());
                intent.putExtra("prop_id", id);
                intent.putExtra("requestCode", REQUEST_RECODE_MODIFY);
                startActivityForResult(intent, REQUEST_RECODE_MODIFY);
                return true;
            case MENU_DELETE:
                index = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;
                recItem = adapter.getItem(index);

                new AlertDialog.Builder(this)
                        .setTitle(recItem.getContent()+" "+recItem.getDate()[0]+"년 "+recItem.getDate()[1]+"월 "+recItem.getDate()[2]+"일")
                        .setMessage("삭제하시겠습니까?")
                        .setPositiveButton("네", new DialogInterface.OnClickListener() {
                            int id = recItem.getId();

                            public void onClick(DialogInterface dialog, int whichButton) {
                                SQLiteDatabase db = dbHelper.getWritableDatabase();
                                db.delete(DatabaseHelper.detailTable, "_id="+id, null);
                                db.close();

                                refresh();
                            }})
                        .setNegativeButton("아니오", null).show();

                return true;
        }
        return false;
    }

    protected void refresh() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("select number, female, year, month, day, mnumber from "+DatabaseHelper.propTable+" where _id="+id, null);
        c.moveToNext();
        number = c.getString(0);
        female = Boolean.valueOf(c.getString(1));
        int year = c.getInt(2);
        int month = c.getInt(3);
        int day = c.getInt(4);
        birthday = new int[] {year, month, day};
        mnumber = c.getString(5);
        c.close();

        TextView tv = new TextView(getApplicationContext());
        tv.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT));
        tv.setText(number.split("-")[2]+" "+(female ? "♀" : "♂"));
        tv.setTextColor(Color.BLACK);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP,30);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(tv);

        tVnumber.setText(number);
        if(mnumber==null) {
            tVmnumber.setText("-");
        } else {
            tVmnumber.setText(mnumber);
        }
        tVbirthday.setText(birthday[0]+"년 "+birthday[1]+"월 "+birthday[2]+"일");


        db = dbHelper.getReadableDatabase();
        c = db.rawQuery("select _id, content, etc, year, month, day from " + DatabaseHelper.detailTable + " where prop_id="+id, null);
        int recodeCount = c.getCount();

        ArrayList<RecodeAdapter.Item> items = new ArrayList<>();
        for(int i=0; i<recodeCount; i++) {
            c.moveToNext();
            int id = c.getInt(0);
            String content = c.getString(1);
            String etc = c.getString(2);
            year = c.getInt(3);
            month = c.getInt(4);
            day = c.getInt(5);

            items.add(new RecodeAdapter.Item(id, content, etc, year, month, day));
        }
        c.close();
        db.close();

        Collections.sort(items, new Comparator<RecodeAdapter.Item>() {
            @Override
            public int compare(RecodeAdapter.Item lhs, RecodeAdapter.Item rhs) {
                int[] lBirth = lhs.getDate();
                int[] rBirth = rhs.getDate();
                return ((lBirth[0]*10000+lBirth[1]*100+lBirth[2]) - (rBirth[0]*10000+rBirth[1]*100+rBirth[2]));
            }
        });

        Calendar now = Calendar.getInstance();
        Calendar calendar = Calendar.getInstance();

        int indexTomorrow = -1;

        for (int i = 0; i < items.size(); i++) {
            int[] date = items.get(i).getDate();
            calendar.set(date[0], date[1] - 1, date[2]);
            if (calendar.after(now)) {
                indexTomorrow = i;
                break;
            }
        }

        adapter.setArrayItem(items);
        lVrecs.setAdapter(adapter);
        if (indexTomorrow != -1) {
            lVrecs.setSelection(indexTomorrow > 0 ? indexTomorrow - 1 : indexTomorrow);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==REQUEST_RECODE_ADD || requestCode==REQUEST_RECODE_MODIFY || requestCode==REQUEST_COW_MODIFY) {
            if(resultCode==RESULT_OK) {
                refresh();
                setResult(RESULT_OK);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_cow, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        final int MENU_ID = item.getItemId();

        //noinspection SimplifiableIfStatement

        if(MENU_ID==R.id.action_cow_modify) {
            Intent intent = new Intent(this, CowEditActivity.class);
            intent.putExtra("id", id);
            intent.putExtra("requestCode", REQUEST_COW_MODIFY);
            startActivityForResult(intent, REQUEST_COW_MODIFY);
        } else if(MENU_ID==R.id.action_cow_delete) {
            new AlertDialog.Builder(this)
                    .setTitle(number)
                    .setMessage("삭제하시겠습니까?")
                    .setPositiveButton("네", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            SQLiteDatabase db = dbHelper.getWritableDatabase();
                            db.delete(DatabaseHelper.propTable, "_id="+id, null);
                            db.delete(DatabaseHelper.detailTable, "prop_id="+id, null);
                            db.close();
                            setResult(RESULT_OK);
                            finish();
                        }})
                    .setNegativeButton("아니오", null).show();
        }

        return super.onOptionsItemSelected(item);
    }
}
