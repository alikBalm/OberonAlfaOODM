package com.alikbalm.oberon_alfaoodm;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
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

    String webUrl, wikiPageName, wikiPageContent;

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


        docWebView = (WebView) findViewById(R.id.docWebView);

        getSupportActionBar().hide();

        docWebView.getSettings().setJavaScriptEnabled(true);

        docWebView.setWebViewClient(new WebViewClient());

        webUrl = getIntent().getStringExtra("webUrl");
        wikiPageName = getIntent().getStringExtra("wikiPageName");
        if (webUrl != null) {
            Map<String, String> extraHeaders = new HashMap<String, String>();
            extraHeaders.put("Authorization",MainActivity.currentUser.token );

            String url =
                    webUrl.contains(MainActivity.currentUser.server) ?
                            webUrl :
                            MainActivity.currentUser.server + webUrl ;

            docWebView.loadUrl(url,extraHeaders);
        } else if (wikiPageName != null ){
           openWikiPageContent(wikiPageName);
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
