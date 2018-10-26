package com.alikbalm.oberon_alfaoodm;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.orm.query.Condition;
import com.orm.query.Select;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocFilesReadWriteSaveDownload extends AppCompatActivity {

    WebView docWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doc_files_read_write_save_download);

        docWebView = (WebView) findViewById(R.id.docWebView);

        getSupportActionBar().hide();

        // тут всё классно всё работает но нужно переделать потому как
        // открывает просто страницу и можно ввести логин и пароль, потому как
        // токен не передаём, нужно почитать как работать с документ сервером, и уже от него плясать

        docWebView.getSettings().setJavaScriptEnabled(true);

        docWebView.setWebViewClient(new WebViewClient());

        Map<String, String> extraHeaders = new HashMap<String, String>();
        //extraHeaders.put("Host",MainActivity.currentUser.server);
        //extraHeaders.put("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        extraHeaders.put("Authorization",MainActivity.currentUser.token );
        //extraHeaders.put("Connection","keep-alive");
        //extraHeaders.put("Host",MainActivity.currentUser.server);


        //Host: alikoffice.onlyoffice.eu
        //User-Agent: Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:62.0) Gecko/20100101 Firefox/62.0
        //Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
        //Accept-Language: ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3
        //Accept-Encoding: gzip, deflate, br
        //Upgrade-Insecure-Requests: 1
        //Cookie: %2Fproducts%2Ffiles%2F=%5B%22%22%2C%22%22%2C%22%22%2C%22%22%2C%22%22%2C%22%22%2C%22%22%2C%22%22%2C%22%22%5D; _ga=GA1.2.1588201736.1539252779; _gcl_au=1.1.2037872142.1539252780; _gid=GA1.2.1208027286.1539586963; lc_sso2673891=1539590227989; __lc.visitor_id.2673891=S1537791128.21b1ed5a65; lc_window_state=minimized; ASP.NET_SessionId=gikp1dquj2edilq4h353s2d2; _ga=GA1.3.1588201736.1539252779; _gid=GA1.3.1208027286.1539586963; asc_auth_key=2WrLhDf6yF8QkqpD7pp0c1GDZQnrch4G60CsieUC/O+GDtVn850ElIsVRw5sxwDTn3En42+5suKo3hv8b4TfY8IzkfwU98a6AoGpBMq0FWrzleRo0vmj/k3+/LGi0+Z5m7zUt5zihlEae2eCDgaLxOyJykO82Y0CPWA7kWnqPAk=; socketio.sid=s%3ApSnxVFGEXIIrfmOztJswdWernkotSHZF.K0OaQisWZjAACgA9%2BO153VFgQYd7WdkYCTvSoJkfN1M; %2F=%5B%22open%22%2C%22%22%2C%22%22%2C%22%22%2C%22open%22%2C%22%22%2C%22%22%2C%22%22%2C%22%22%2C%22open%22%2C%22%22%2C%22%22%2C%22%22%2C%22open%22%2C%22%22%2C%22%22%2C%22%22%2C%22%22%2C%22%22%2C%22%22%2C%22%22%2C%22%22%5D; _gat=1
        //Connection: keep-alive


        String webUrl = getIntent().getStringExtra("webUrl");

        Log.i("111 webUrl", webUrl);

        String url = webUrl.contains("https") ? webUrl : "https://" + MainActivity.currentUser.server + webUrl ;

        Log.i("111 url", url);

        //getTextFromUrl(url);


        docWebView.loadUrl(url,extraHeaders);


    }

    void getTextFromUrl(String urlWeb){



        StringRequest jsonObjectRequest = new StringRequest
                (Request.Method.GET, urlWeb, new Response.Listener<String>() {

                    @Override
                    public void onResponse(String response) {

                         Log.i("response",response);

                        docWebView.loadData(response,"text/html; charset=utf-8","UTF-8");

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {

                        error.printStackTrace();

                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Authorization", MainActivity.currentUser.token);
                return params;
            }

        };
        MySingleton.getInstance(DocFilesReadWriteSaveDownload.this).addToRequestQueue(jsonObjectRequest);



    }
}
