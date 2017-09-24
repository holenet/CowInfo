package com.holenet.cowinfo;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.Calendar;

import static com.holenet.cowinfo.RecodeActivity.REQUEST_RECODE_ADD;
import static com.holenet.cowinfo.RecodeActivity.REQUEST_RECODE_MODIFY;

public class RecodeEditActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    int MODE;

    String[] spinnerItems = new String[] {
            "수정",
            "재발",
            "분만",
            "기타",
    };

    int id = -1;
    int prop_id = -1;
    String content;
    String etc;
    int[] date;

    Spinner sPcontent;
    EditText eTetc;
    EditText eTyear, eTmonth, eTday;
    CheckBox cBjb, cBbm;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dbHelper = new DatabaseHelper(this);

        sPcontent = (Spinner) findViewById(R.id.sPcontent);
        eTetc = (EditText) findViewById(R.id.eTetc);
        eTyear = (EditText) findViewById(R.id.eTyear);
        eTmonth = (EditText) findViewById(R.id.eTmonth);
        eTday = (EditText) findViewById(R.id.eTday);
        cBjb = (CheckBox) findViewById(R.id.cBjb);
        cBbm = (CheckBox) findViewById(R.id.cBbm);

        connect(eTyear, eTmonth);
        connect(eTmonth, eTday);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner, spinnerItems);
        sPcontent.setAdapter(adapter);
        sPcontent.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                content = spinnerItems[position];
                if(position==0) {
                    cBjb.setEnabled(true);
                    cBbm.setEnabled(true);
                } else {
                    cBbm.setChecked(false);
                    if(position==1) {
                        cBjb.setEnabled(true);
                    } else {
                        cBjb.setEnabled(false);
                        cBjb.setChecked(false);
                    }
                    cBbm.setEnabled(false);
                }
                eTetc.requestFocus();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        Intent intent = getIntent();
        prop_id = intent.getIntExtra("prop_id", -1);
        MODE = intent.getIntExtra("requestCode", 0);
        if(MODE==REQUEST_RECODE_MODIFY) {
            getSupportActionBar().setTitle("이력 수정");

            id = intent.getIntExtra("id", -1);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor c = db.rawQuery("select prop_id, content, etc, year, month, day from "+DatabaseHelper.detailTable+" where _id="+id, null);
            c.moveToNext();
            prop_id = c.getInt(0);
            content = c.getString(1);
            etc = c.getString(2);
            int year = c.getInt(3);
            int month = c.getInt(4);
            int day = c.getInt(5);
            date = new int[] {year, month, day};
            c.close();

            for(int i=0; i<spinnerItems.length; i++) {
                sPcontent.setSelection(i);
                if(content.equals(spinnerItems[i])) {
                    break;
                }
            }
            eTetc.setText(etc);

            eTyear.setText(String.valueOf(date[0]));
            eTmonth.setText(String.valueOf(date[1]));
            eTday.setText(String.valueOf(date[2]));
        } else {
            getSupportActionBar().setTitle("이력 추가");
            eTyear.setText(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
        }
        eTyear.setSelection(eTyear.getText().toString().length());

        setResult(RESULT_CANCELED);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        if(id==android.R.id.home){
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    protected void connect(final EditText eT1, final EditText eT2) {
        int maxLength = -1;
        for(InputFilter filter : eT1.getFilters()) {
            if(filter instanceof InputFilter.LengthFilter) {
                try {
                    Field maxLengthField = filter.getClass().getDeclaredField("mMax");
                    maxLengthField.setAccessible(true);
                    if(maxLengthField.isAccessible()) {
                        maxLength = maxLengthField.getInt(filter);
                    }
                } catch (Exception e) {}
            }
        }
        final int finalMaxLength = maxLength;
        eT1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                if(eT1.getText().toString().length()== finalMaxLength) {
                    eT2.requestFocus();
                }
            }
        });
    }

    public void pad(View v) {
        if(v.getId()==R.id.bTcancel) {
            finish();
            return;
        }

        etc = eTetc.getText().toString();
        if(content.equals(spinnerItems[spinnerItems.length-1]) && etc.length()==0) {
            toast("내용이 비어있습니다.");
            return;
        }

        int year, month, day;
        try {
            year = Integer.valueOf(eTyear.getText().toString());
            month = Integer.valueOf(eTmonth.getText().toString());
            day = Integer.valueOf(eTday.getText().toString());
        } catch (Exception e) {
            toast(e.getMessage());
            return;
        }

        if(!(0<month && month<=12 && 0<day && day<=31)) {
            toast("날짜가 올바르지 않습니다.");
            return;
        }

        ContentValues values = new ContentValues();
        values.put("prop_id", prop_id);
        values.put("content", content);
        values.put("etc", etc);
        values.put("year", year);
        values.put("month", month);
        values.put("day", day);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if(MODE==REQUEST_RECODE_ADD) {
            db.insert(DatabaseHelper.detailTable, null, values);
        } else {
            db.update(DatabaseHelper.detailTable, values, "_id="+id, null);
        }

        if(content.equals("수정")) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month-1, day);
            if(cBbm.isChecked()) {
                calendar.add(Calendar.DAY_OF_MONTH, 285);
                values = new ContentValues();
                values.put("prop_id", prop_id);
                values.put("content", "분만");
                values.put("etc", etc+"(추정)");
                values.put("year", calendar.get(Calendar.YEAR));
                values.put("month", calendar.get(Calendar.MONTH)+1);
                values.put("day", calendar.get(Calendar.DAY_OF_MONTH));
                db.insert(DatabaseHelper.detailTable, null, values);
            }
        }
        if(content.equals("수정") || content.equals("재발")) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month-1, day);
            Calendar now = Calendar.getInstance();
            if(cBjb.isChecked()) {
                calendar.add(Calendar.DAY_OF_MONTH, 21);
                while(calendar.before(now)) {
                    calendar.add(Calendar.DAY_OF_MONTH, 21);
                }
                if(calendar.get(Calendar.DAY_OF_MONTH)==now.get(Calendar.DAY_OF_MONTH)) {
                    calendar.add(Calendar.DAY_OF_MONTH, 21);
                }
                values = new ContentValues();
                values.put("prop_id", prop_id);
                values.put("content", "재발");
                values.put("etc", "(추정)");
                values.put("year", calendar.get(Calendar.YEAR));
                values.put("month", calendar.get(Calendar.MONTH)+1);
                values.put("day", calendar.get(Calendar.DAY_OF_MONTH));
                db.insert(DatabaseHelper.detailTable, null, values);
            }
        }
        db.close();

        setResult(RESULT_OK);
        finish();
    }

    public void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
