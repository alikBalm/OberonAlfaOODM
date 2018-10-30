package com.alikbalm.oberon_alfaoodm;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import com.orm.query.Condition;
import com.orm.query.Select;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DocFilesReadWriteSaveDownload extends AppCompatActivity {

    WebView docWebView;

    Retrofit retrofit;
    OnlyOfficeApi service;

    String webUrl, wikiPageName, wikiPageContent, contactId;

    ConstraintLayout parent1;
    ImageView contactImage;
    File IconFile;
    TextView fio, email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doc_files_read_write_save_download);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(100, TimeUnit.SECONDS)
                .readTimeout(100,TimeUnit.SECONDS).build();


        retrofit = new Retrofit.Builder()
                .baseUrl(MainActivity.currentUser.server)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(OnlyOfficeApi.class);

        parent1 = (ConstraintLayout) findViewById(R.id.parent1);
        contactImage = (ImageView) findViewById(R.id.contactImage);
        fio = (TextView) findViewById(R.id.fio);
        email = (TextView) findViewById(R.id.email);

        // тут слушатель нажатия для отправки почты
        email.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(getApplicationContext(), ReadSendEditMessages.class);
                intent.putExtra("newMessage", true);
                intent.putExtra("to", email.getText().toString());
                startActivity(intent);

            }
        });


        docWebView = (WebView) findViewById(R.id.docWebView);

        getSupportActionBar().hide();

        docWebView.getSettings().setJavaScriptEnabled(true);

        docWebView.setWebViewClient(new WebViewClient());

        webUrl = getIntent().getStringExtra("webUrl");
        wikiPageName = getIntent().getStringExtra("wikiPageName");
        contactId = getIntent().getStringExtra("contactId");
        if (webUrl != null) {
            Map<String, String> extraHeaders = new HashMap<String, String>();
            extraHeaders.put("Authorization",MainActivity.currentUser.token );

            String url =
                    webUrl.contains(MainActivity.currentUser.server) ?
                            webUrl :
                            MainActivity.currentUser.server + webUrl ;
            docWebView.setVisibility(View.VISIBLE);

            docWebView.loadUrl(url,extraHeaders);
        } else if (wikiPageName != null ){
           openWikiPageContent(wikiPageName);
        } else if (contactId != null) {

            //здесь нужно переделать так как открывается страничка в онлиофисе с боковой панелью
            // а нам такая шняжкка не нужна
            // поэтому лучше будет скачивать фото профиля и подставлять значения в элементы parent1

            List<ContactsOO> contactL = Select.from(ContactsOO.class)
                    .where(Condition.prop("contact_id").eq(contactId))
                    .list();
            fio.setText(contactL.get(0).lastName + " " + contactL.get(0).firstName);
            email.setText(contactL.get(0).email);
            String avatar = contactL.get(0).avatar;
            getContactAvatar(avatar);

        }
    }

    void getContactAvatar(String avatar){
        Log.i("!!! ", avatar);
        Call<ResponseBody> getImage = service.getContactsAvatar(MainActivity.currentUser.token, avatar);

        getImage.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.i("!!! ", "server contacted and has file");

                    boolean writtenToDisk = writeResponseBodyToDisk(response.body(),contactId);
                    if (writtenToDisk) {
                        Bitmap myBitmap = BitmapFactory.decodeFile(IconFile.getAbsolutePath());
                        contactImage.setImageBitmap(myBitmap);
                        parent1.setVisibility(View.VISIBLE);
                    }

                    Log.i("!!! ", "file download was a success? " + writtenToDisk);

                } else {
                    Log.i("!!! ", "server contact failed");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.i("!!! ", "error");
            }
        });
    }

    private boolean writeResponseBodyToDisk(ResponseBody body, String contactId) {
        try {

            IconFile = new File(getFilesDir(),contactId +".png");

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(IconFile);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    fileSizeDownloaded += read;

                    Log.i("!!! ", "file download: " + fileSizeDownloaded + " of " + fileSize);
                }

                outputStream.flush();

                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }

    void openWikiPageContent(String name) {


        Call<ResponseBody> getWikiPageContent = service.getWikiPageContent(
                MainActivity.currentUser.token,
                name
        );
        getWikiPageContent.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                try {
                    JSONObject respo = new JSONObject(response.body().string());
                    JSONObject resp = respo.getJSONObject("response");
                    wikiPageContent = resp.getString("content");
                    docWebView.setVisibility(View.VISIBLE);
                    docWebView.loadData(wikiPageContent,"text/html; charset=utf-8","UTF-8");
                } catch (JSONException e) {
                    Log.i("!!! JSONException", "getWikiPageContent " + e.getMessage());
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.i("!!! IOException", "getWikiPageContent " + e.getMessage());
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.i("!!! onFailure", "getWikiPageContent " + t.getMessage());
            }
        });

    }
}
