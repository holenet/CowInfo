package com.holenet.cowinfo;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.Calendar;

import static com.holenet.cowinfo.RecodeActivity.REQUEST_COW_ADD;
import static com.holenet.cowinfo.RecodeActivity.REQUEST_COW_MODIFY;

public class CowEditActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    int MODE;

    int id = -1;
    String number;
    boolean female;
    int[] birthday;
    String mnumber;

    RadioButton rBfemale, rBmale;
    EditText eTnum1, eTnum2, eTnum3, eTnum4;
    EditText eTyear, eTmonth, eTday;
    EditText eTmnum1, eTmnum2, eTmnum3, eTmnum4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prop);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dbHelper = new DatabaseHelper(this);

        rBfemale = (RadioButton) findViewById(R.id.rBfemale);
        rBmale = (RadioButton) findViewById(R.id.rBmale);
        eTnum1 = (EditText) findViewById(R.id.eTnum1);
        eTnum2 = (EditText) findViewById(R.id.eTnum2);
        eTnum3 = (EditText) findViewById(R.id.eTnum3);
        eTnum4 = (EditText) findViewById(R.id.eTnum4);
        eTyear = (EditText) findViewById(R.id.eTyear);
        eTmonth = (EditText) findViewById(R.id.eTmonth);
        eTday = (EditText) findViewById(R.id.eTday);
        eTmnum1 = (EditText) findViewById(R.id.eTmnum1);
        eTmnum2 = (EditText) findViewById(R.id.eTmnum2);
        eTmnum3 = (EditText) findViewById(R.id.eTmnum3);
        eTmnum4 = (EditText) findViewById(R.id.eTmnum4);

        eTnum1.setSelection(eTnum1.getText().toString().length());
        eTmnum1.setSelection(eTmnum1.getText().toString().length());

        connect(eTnum1, eTnum2);
        connect(eTnum2, eTnum3);
        connect(eTnum3, eTnum4);
        connect(eTnum4, eTyear);
        connect(eTyear, eTmonth);
        connect(eTmonth, eTday);
        connect(eTday, eTmnum1);
        connect(eTmnum1, eTmnum2);
        connect(eTmnum2, eTmnum3);
        connect(eTmnum3, eTmnum4);

        Intent intent = getIntent();
        MODE = intent.getIntExtra("requestCode", 0);
        if(MODE==REQUEST_COW_MODIFY) {
            getSupportActionBar().setTitle("개체 정보 수정");

            id = intent.getIntExtra("id", -1);
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

            String[] numbers = number.split("-");
            eTnum1.setText(numbers[0]);
            eTnum2.setText(numbers[1]);
            eTnum3.setText(numbers[2]);
            eTnum4.setText(numbers[3]);

            if(!female) {
                rBfemale.setChecked(false);
                rBmale.setChecked(true);
            }

            eTyear.setText(String.valueOf(birthday[0]));
            eTmonth.setText(String.valueOf(birthday[1]));
            eTday.setText(String.valueOf(birthday[2]));

            if(mnumber!=null) {
                String[] nums = mnumber.split("-");
                eTmnum1.setText(nums[0]);
                eTmnum2.setText(nums[1]);
                eTmnum3.setText(nums[2]);
                eTmnum4.setText(nums[3]);
            }
        } else {
            getSupportActionBar().setTitle("한우 개체 추가");
            eTyear.setText(String.valueOf(20));
            eTyear.setSelection(2);
            eTnum2.requestFocus();
            eTnum2.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(eTnum2, 0);
                }
            }, 100);
        }

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

        female = rBfemale.isChecked();

        number = "";
        number += eTnum1.getText().toString() + "-";
        number += eTnum2.getText().toString() + "-";
        number += eTnum3.getText().toString() + "-";
        number += eTnum4.getText().toString();

        if(number.length()!=15) {
            toast("식별번호가 올바르지 않습니다.");
            return;
        }

        birthday = new int[3];
        try {
            birthday[0] = Integer.valueOf(eTyear.getText().toString());
            birthday[1] = Integer.valueOf(eTmonth.getText().toString());
            birthday[2] = Integer.valueOf(eTday.getText().toString());
        } catch (Exception e) {
            toast(e.getMessage());
            return;
        }

        if(!(0<birthday[1] && birthday[1]<=12 && 0<birthday[2] && birthday[2]<=31)) {
            toast("출생일자가 올바르지 않습니다.");
            return;
        }

        mnumber = "";
        mnumber += eTmnum1.getText().toString() + "-";
        mnumber += eTmnum2.getText().toString() + "-";
        mnumber += eTmnum3.getText().toString() + "-";
        mnumber += eTmnum4.getText().toString();

        if(mnumber.length()>6 && mnumber.length()<15) {
            toast("모개체 식별번호가 올바르지 않습니다.");
            return;
        }

        ContentValues values = new ContentValues();
        values.put("number", number);
        values.put("female", String.valueOf(female));
        values.put("year", birthday[0]);
        values.put("month", birthday[1]);
        values.put("day", birthday[2]);
        if(mnumber.length()==15)
            values.put("mnumber", mnumber);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if(MODE==REQUEST_COW_ADD) {
            db.insert(DatabaseHelper.propTable, null, values);
        } else {
            db.update(DatabaseHelper.propTable, values, "_id="+id, null);
        }
        db.close();

        setResult(RESULT_OK);
        finish();
    }

    public void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
