package com.vibaroo.gim;


import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {
    Button imgsel,upload;
    ImageView img;
    String path;
    ProgressDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        img = (ImageView)findViewById(R.id.img);
        Ion.getDefault(this).configure().setLogging("ion-sample", Log.DEBUG);
        imgsel = (Button)findViewById(R.id.selimg);
        upload =(Button)findViewById(R.id.uploadimg);
        upload.setVisibility(View.INVISIBLE);
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("logtag", "Button pressed" + path);
                imgsel.setVisibility(View.INVISIBLE);
                mDialog = new ProgressDialog(MainActivity.this);
                mDialog.setMessage("Uploading and processing");
                mDialog.setCancelable(false);
                mDialog.show();

                File f = new File(path);

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                String url = sharedPref.getString("URL", getString(R.string.pref_URL));
                Log.e("logtag", url);

                Future uploading = Ion.with(MainActivity.this)
                        .load("http://"+url+":5000/upload")
                        .setMultipartFile("image", f)
                        .asString()
                        .withResponse()
                        .setCallback(new FutureCallback<Response<String>>() {
                            @Override
                            public void onCompleted(Exception e, Response<String> result) {
                                imgsel.setVisibility(View.VISIBLE);
                                mDialog.cancel();
                                try {
                                    JSONObject jobj = new JSONObject(result.getResult());

                                    showOKAlert(jobj.getString("response"));

                                } catch (JSONException e1) {
                                    e1.printStackTrace();
                                }

                            }
                        });
            }

        });

        imgsel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent fintent = new Intent(Intent.ACTION_GET_CONTENT);
                fintent.setType("image/jpeg");
                try {
                    startActivityForResult(fintent, 100);
                } catch (ActivityNotFoundException e) {

                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            Intent intent;
            intent = new Intent( this, SettingsActivity.class );
            intent.putExtra( AppCompatPreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GeneralPreferenceFragment.class.getName() );
            intent.putExtra( AppCompatPreferenceActivity.EXTRA_NO_HEADERS, true );
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null)
            return;
        switch (requestCode) {
            case 100:
/*


                Bitmap bm = BitmapFactory.d
                ByteArrayOutputStream bao = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.JPEG, 90, bao);
                byte[] ba = bao.toByteArray();
                ba1 = Base64.encodeBytes(ba);

*/

                if (resultCode == RESULT_OK) {
                    dumpImageUriToFile(data.getData());
                    Log.e("URI", data.getData().toString());
                    path = getRealPathFromURI(data.getData());
                    if (path == null) {
                        showOKAlert("That file does not seem to be available locally");
                    } else {
                        Log.e("logtag", path);
                        img.setImageURI(data.getData());
                        upload.setVisibility(View.VISIBLE);
                    }

                }
        }
    }

    private OutputStream dumpImageUriToFile(Uri uri) {

        final int chunkSize = 1024;  // We'll read in one kB at a time
        byte[] imageData = new byte[chunkSize];
        String destinationFilename = android.os.Environment.getExternalStorageDirectory().getPath()+File.separatorChar+"abc.jpg";
        InputStream in = null;
        OutputStream out = null;
        try {
            in = getContentResolver().openInputStream(uri);
            out = new FileOutputStream(destinationFilename);
            int bytesRead;
            while ((bytesRead = in.read(imageData)) > 0) {
                out.write(Arrays.copyOfRange(imageData, 0, Math.max(0, bytesRead)));
            }
            return out;
        } catch (Exception e) {
            Log.e("logtag", e.getMessage());
            try {out.close();} catch (Exception e2) {}
            return null;
        } finally {
            try {in.close();} catch (Exception e2) {}
        }
    }


    private String getPathFromURI(Uri contentUri) {
        String filePath = null;
        String wholeID;
        try {
            wholeID = DocumentsContract.getDocumentId(contentUri);

            // Split at colon, use second item in the array
            String id = wholeID.split(":")[1];

            String[] column = { MediaStore.Images.Media.DATA };

            // where id is equal to
            String sel = MediaStore.Images.Media._ID + "=?";

            Cursor cursor = this.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    column, sel, new String[]{ id }, null);

            int columnIndex = cursor.getColumnIndex(column[0]);

            if (cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex);
            }
            cursor.close();
        } catch (Exception e) {
            Log.e("logtag", e.toString());
        }
        return filePath;
    }

    private String getPathFromURI2(Uri uri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = this.getContentResolver().query(uri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String tempUri = cursor.getString(column_index);
            return cursor.getString(column_index);
        } catch (Exception e) {
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private String getRealPathFromURI(Uri uri) {
        String path;
        path = getPathFromURI(uri);
        if (path == null) path = getPathFromURI2(uri);
        return path;
    };

    private String getRealPathFromURI2(Uri uri) {
        Cursor cursor = null;
        Pattern pattern = Pattern.compile("(content://media/.*\\d)");
        String uriText = uri.toString();
        if (uriText.contains("content")) {
            Matcher matcher = pattern.matcher(uriText);
            if (matcher.find()) {
                try {
                    String[] proj = {MediaStore.Images.Media.DATA};
                    cursor = this.getContentResolver().query(uri, proj, null, null, null);
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    cursor.moveToFirst();
                    String tempUri = cursor.getString(column_index);
                    return cursor.getString(column_index);
                } catch (Exception e) {
                    return null;
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }
        return getPathFromURI(uri);
    }

    private Uri handleImageUri(Uri uri) {
        Pattern pattern = Pattern.compile("(content://media/.*\\d)");
        if (uri.getPath().contains("content")) {
            Matcher matcher = pattern.matcher(uri.getPath());
            if (matcher.find())
                return Uri.parse(matcher.group(1));
            else
                throw new IllegalArgumentException("Cannot handle this URI");
        } else
            return uri;
    }

    private void showOKAlert(String message) {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage(message);
        builder1.setCancelable(false);

        builder1.setNeutralButton(
                "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert11 = builder1.create();
        alert11.show();
        TextView alertView = (TextView)alert11.findViewById(android.R.id.message);
        alertView.setGravity(Gravity.CENTER);
    }

/*        String[] proj = { MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(getApplicationContext(), contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }*/

}