package toandoan.framgia.com.asyntasksample;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by framgia on 15/05/2017.
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int WRITE_EXTERNAL_REQUEST = 1;
    private final static String URL_SAMPLE =
            "http://mp3.zing.vn/download/song/Yeu-La-Tha-Thu-Em-Chua-18-OST-OnlyC-OnlyC"
                    + "/ZHJGtLmNgkDkilhyLbctDnkmtZAESczZmCh?sig=6dc2cd852f9a168618e70618a7dbcbe3";
    private final static String FILE_NAME = "Yeu_La_Tha_Thu.mp3";

    private EditText mEditUrl, mEditFileName;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.button_download).setOnClickListener(this);
        mEditUrl = (EditText) findViewById(R.id.edit_url);
        mEditUrl.setText(URL_SAMPLE);

        mEditFileName = (EditText) findViewById(R.id.edit_file_name);
        mEditFileName.setText(FILE_NAME);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage(getString(R.string.msg_downloading));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    }

    private boolean isPermissonGrant() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                    WRITE_EXTERNAL_REQUEST);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_EXTERNAL_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            downloadFiles();
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_download) {
            if (isPermissonGrant()) {
                downloadFiles();
            }
        }
    }

    private void downloadFiles() {
        String url = mEditUrl.getText().toString();
        String fileName = mEditFileName.getText().toString();
        mProgressDialog.show();
        String[] parrams = { url, fileName };
        new DownloadAsyn(this).execute(parrams);
    }

    public class DownloadAsyn extends AsyncTask<String, Integer, String> {

        private Context mContext;
        private PowerManager.WakeLock mWakeLock;

        public DownloadAsyn(Context context) {
            mContext = context;
        }

        @Override
        protected String doInBackground(String... urls) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(urls[0]);
                String fileName = urls[1];
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode() + " " + connection
                            .getResponseMessage();
                }

                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream("/sdcard/" + fileName);

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;

                    if (fileLength > 0) {
                        publishProgress((int) (total * 100 / fileLength));
                    }
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null) output.close();
                    if (input != null) input.close();
                } catch (IOException ignored) {
                }

                if (connection != null) connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
            mWakeLock.acquire();
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            mProgressDialog.dismiss();
            if (result != null) {
                Toast.makeText(mContext, "Download error: " + result, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(mContext, "File downloaded", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
