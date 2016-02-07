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
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity2 extends AppCompatActivity {
    Button imgsel,upload;
    ImageView img;
    String path;
    ProgressDialog mDialog;
    AsyncUploadImage asyncUploadImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        img = (ImageView)findViewById(R.id.img);
        imgsel = (Button)findViewById(R.id.selimg);

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

//        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);


        upload =(Button)findViewById(R.id.uploadimg);
        upload.setVisibility(View.INVISIBLE);
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("logtag", "Button pressed" + path);
                imgsel.setVisibility(View.INVISIBLE);

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity2.this);
                String url = sharedPref.getString("URL", getString(R.string.pref_URL));
                Log.e("logtag", url);

                asyncUploadImage = new AsyncUploadImage();
                asyncUploadImage.execute(path, url);

                imgsel.setVisibility(View.INVISIBLE);
                mDialog = new ProgressDialog(MainActivity2.this);
                mDialog.setMessage("Uploading and processing");
                mDialog.setCancelable(true);
                mDialog.show();
                mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                        imgsel.setVisibility(View.VISIBLE);
                        asyncUploadImage.cancel(true);
                    }
                });
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
                    path = dumpImageUriToFile(data.getData());
//                    path = getRealPathFromURI(data.getData());
                    if (path == null) {
                        showOKAlert("That file does not seem to be available locally");
                    } else {
                        Log.e("logtag", path);
//                        img.setImageURI(data.getData());
                        upload.setVisibility(View.VISIBLE);
                    }

                }
        }
    }

    private String dumpImageUriToFile(Uri uri) {

//        final int chunkSize = 1024;  // We'll read in one kB at a time
//        byte[] imageData = new byte[chunkSize];

        File file;
//        String destinationFilename = android.os.Environment.getExternalStorageDirectory().getPath()+File.separatorChar+"abc.jpg";
//        file = new File(destinationFilename);
        file = new File(this.getCacheDir(), "tmp.jpg");
        String filename = file.getPath();

        InputStream in = null;
        OutputStream out = null;

        try {
//            in = getContentResolver().openInputStream(uri);
            int REQUIRED_SIZE;
            int compression;
            out = new FileOutputStream(file);


            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, o);

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity2.this);
            // The new size we want to scale to
            try {
                REQUIRED_SIZE = Integer.parseInt(sharedPref.getString("bitmap_size", getString(R.string.pref_bitmap_size)));
            } catch (Exception e) {
                REQUIRED_SIZE = Integer.parseInt(getString(R.string.pref_bitmap_size));
                Toast.makeText(MainActivity2.this, "Bitmap size preference is not a valid integer.",
                        Toast.LENGTH_LONG).show();
            }
            // The JPEG compression we want
            try {
                compression = Integer.parseInt(sharedPref.getString("compression", getString(R.string.pref_compression)));
            } catch (Exception e) {
                compression = Integer.parseInt(getString(R.string.pref_compression));
                Toast.makeText(MainActivity2.this, "JPEG compression preference is not a valid integer.",
                        Toast.LENGTH_LONG).show();
            }

            // Find the correct scale value. It should be the power of 2.
            int scale = 1;
            while(o.outWidth / scale / 2 >= REQUIRED_SIZE &&
                    o.outHeight / scale / 2 >= REQUIRED_SIZE) {
                scale *= 2;
            }

            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            Bitmap imageBitmap = BitmapFactory
                    .decodeStream(getContentResolver().openInputStream(uri), null, o2);

/*            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inTempStorage = new byte[64 * 1024];
            opt.inSampleSize = 1;
            opt.outWidth = 1024;
            opt.outHeight = 768;
            Bitmap imageBitmap = BitmapFactory
                    .decodeStream(in, null, opt);*/
//            Bitmap map = Bitmap.createScaledBitmap(imageBitmap, 100, 100, true);
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, compression, out);
            img.setImageBitmap(imageBitmap);


/*
            int bytesRead;
            while ((bytesRead = in.read(imageData)) > 0) {
                out.write(Arrays.copyOfRange(imageData, 0, Math.max(0, bytesRead)));
            }*/
            out.close();
            return filename;
        } catch (Exception e) {
            Log.e("logtag", e.getMessage());
            try {
                out.close();
            } catch (Exception e2) {
                Log.e("logtag", e2.getMessage());
            }
            return null;
        } finally {
            try {
                in.close();
            } catch (Exception e2) {
                Log.e("logtag", e2.getMessage());
            }
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

    private class AsyncUploadImage extends AsyncTask<String, Void, String> {
        BufferedReader responseStreamReader;

        protected String doInBackground(String... attachmentFileName) {
//            String myURL = "http://zackgleit.ddns.net:5000/upload";
            String myURL = "http://" + attachmentFileName[1] + ":5000/upload";
            Log.e("logtag",myURL);
            String attachmentName = "image";
            String crlf = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            HttpURLConnection httpUrlConnection = null;
            String response;
            try {
                URL url = new URL(myURL);
                httpUrlConnection = (HttpURLConnection) url.openConnection();
                httpUrlConnection.setUseCaches(false);
                httpUrlConnection.setDoOutput(true);

                httpUrlConnection.setRequestMethod("POST");
                httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
                httpUrlConnection.setRequestProperty("Cache-Control", "no-cache");
                httpUrlConnection.setRequestProperty(
                        "Content-Type", "multipart/form-data;boundary=" + boundary);
                DataOutputStream request = new DataOutputStream(
                        httpUrlConnection.getOutputStream());

                request.writeBytes(twoHyphens + boundary + crlf);
                request.writeBytes("Content-Disposition: form-data; name=\"" +
                        attachmentName + "\";filename=\"" +
                        attachmentFileName[0] + "\"" + crlf);
                request.writeBytes(crlf);


                FileInputStream fis = new FileInputStream(attachmentFileName[0]);

                //            OutputStream os = new BufferedOutputStream(urlConnection.getOutputStream());
                //           OutputStreamWriter out = new OutputStreamWriter(os);

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    request.write(buffer, 0, bytesRead);
                }
                request.writeBytes(crlf);
                request.writeBytes(twoHyphens + boundary +
                        twoHyphens + crlf);
                request.flush();
                request.close();
                InputStream responseStream = new BufferedInputStream(httpUrlConnection.getInputStream());

                responseStreamReader = new BufferedReader(new InputStreamReader(responseStream));

                String line = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((line = responseStreamReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                responseStreamReader.close();

                response = stringBuilder.toString();


            } catch (Exception e) {
                Log.e("logtag", e.toString());
                response = "";
            }
            return response;
        }

        protected void onPreExecute() {

        }

        protected void onPostExecute(String result) {
            mDialog.cancel();
            try {
                JSONObject jobj = new JSONObject(result);
                showOKAlert(jobj.getString("response"));
            } catch (Exception e) {
                showOKAlert("Error");
            }
        }

    }
}