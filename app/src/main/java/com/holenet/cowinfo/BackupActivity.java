package com.holenet.cowinfo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static java.net.HttpURLConnection.HTTP_OK;

public class BackupActivity extends AppCompatActivity {
    final static int REQUEST_LOGIN = 1234;
    final static int REQUEST_LOGIN_BACKUP = 4321;

    ProgressBar pBloading;
    LinearLayout lLcontent;
    ListView lVdb;

    DBBackupTask backupTask;
    boolean loggedIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        pBloading = (ProgressBar) findViewById(R.id.pBloading);
        lLcontent = (LinearLayout) findViewById(R.id.lLcontent);
        lVdb = (ListView) findViewById(R.id.lVdb);

        FloatingActionButton fABaddDB = (FloatingActionButton) findViewById(R.id.fABaddDB);
        fABaddDB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String[] itemNames = new String[] {"기기에 백업", "서버에 백업"};
                final boolean[] items = new boolean[] {false, false};
                new AlertDialog.Builder(BackupActivity.this)
                        .setTitle("데이터베이스 백업")
                        .setMultiChoiceItems(itemNames, items, new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                                items[i] = b;
                            }
                        })
                        .setPositiveButton("네", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if(!items[0] && !items[1]) {
                                    Toast.makeText(BackupActivity.this, "백업되지 않았습니다.", Toast.LENGTH_SHORT).show();
                                } else {
                                    tryBackup(items);
                                }
                            }
                        })
                        .setNegativeButton("아니요", null)
                        .create().show();
            }
        });

        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN);
    }

    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        lLcontent.setVisibility(show ? View.GONE : View.VISIBLE);
        lLcontent.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                lLcontent.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        pBloading.setVisibility(show ? View.VISIBLE : View.GONE);
        pBloading.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                pBloading.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED) {
            tryBackup(new boolean[] {requestCode>=2, requestCode%2==1});
        } else {
            Toast.makeText(this, R.string.error_permission, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUEST_LOGIN) {
            if(resultCode==1) {
                loggedIn = true;
            } else {
                Toast.makeText(this, "로그인 되지 않았습니다.", Toast.LENGTH_SHORT).show();
            }
        } else if(requestCode==REQUEST_LOGIN_BACKUP) {
            if(resultCode==1) {
                loggedIn = true;
                tryBackup(data.getBooleanArrayExtra("items"));
            } else {
                Toast.makeText(this, "서버에 데이터베이스를 저장하려면 로그인 해야합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void tryBackup(boolean[] items) {
        if(items[0] && ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
            Log.e("request", "not granted");
            Toast.makeText(this, R.string.error_permission, Toast.LENGTH_LONG).show();
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Log.e("request", "??");
            } else {
                Log.e("request", "permmm");
            }
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, (items[0] ? 2 : 0) + (items[1] ? 1 : 0));
        } else if(items[1]) {
            if(loggedIn) {
                backup(items);
            } else {
                Intent intent = new Intent(BackupActivity.this, LoginActivity.class);
                intent.putExtra("items", items);
                startActivityForResult(intent, REQUEST_LOGIN_BACKUP);
            }
        } else {
            backup(items);
        }
    }

    private void backup(boolean[] items) {
        if(backupTask!=null) {
            Toast.makeText(BackupActivity.this, R.string.error_task_exist, Toast.LENGTH_LONG).show();
            return;
        }
        showProgress(true);
        backupTask = new DBBackupTask(BackupActivity.this, items);
        backupTask.execute((Void) null);
    }

    public class DBBackupTask extends AsyncTask<Void, Integer, Void> {
        Context context;
        boolean[] items;

        public DBBackupTask(Context context, boolean[] items) {
            this.context = context;
            this.items = items;
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.e("doInBackground", items[0]+"/"+items[1]);

            if(items[0]) {
                publishProgress(0, backupToDevice() ? 1: 0);
            }
            if(items[1]) {
                publishProgress(1, backupToServer() ? 1: 0);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            Log.d("onProgressUpdate", values[0]+"/"+values[1]);
            if(values[1]==1) {
                Toast.makeText(BackupActivity.this, (values[0]==0?"기기로 ":"서버로 ")+getString(R.string.success_backup), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(BackupActivity.this, (values[0]==0?"기기로 ":"서버로 ")+getString(R.string.fail_backup), Toast.LENGTH_LONG).show();
            }
        }

        boolean backupToDevice() {
            String backupDir = Environment.getExternalStorageDirectory()+File.separator+"cowinfo"+File.separator+"database";
            String backupFilePath = backupDir+File.separator+new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(Calendar.getInstance().getTime())+".db";
            Log.e("backupDir", backupDir);
            Log.e("backupFilePath", backupFilePath);
            File inFile = new File(String.valueOf(context.getExternalFilesDir(null)) + File.separator + "cows.db");
            File outFile = new File(backupDir);
            if(!outFile.exists()) {
                if(!outFile.mkdirs()) {
                    Log.e("outFile.mkdirs Error", "");
                    return false;
                }
            }
            outFile = new File(backupFilePath);
            Log.e("file", "in and out prepared");
            try {
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(inFile));
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
                int data;
                while(true) {
                    data = in.read();
                    if(data==-1)
                        break;
                    out.write(data);
                }
                in.close();
                out.close();
            } catch(IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        boolean backupToServer() {
            String url = NetworkManager.DATABASE_URL+"upload/";
            File file = new File(String.valueOf(context.getExternalFilesDir(null)) + File.separator + "cows.db");
            String fileName = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(Calendar.getInstance().getTime())+".db";
            return NetworkManager.upload(context, url, file, fileName)==HTTP_OK;
        }

        boolean restoreFromDevice(Context context, String filePath) {
            return false;
        }

        boolean restoreFromServer(Context context, String filePath) {
            return false;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            backupTask = null;
            showProgress(false);

        }

        @Override
        protected void onCancelled() {
            backupTask = null;
            showProgress(false);
        }
    }
}
