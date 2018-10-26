package com.alikbalm.oberon_alfaoodm;

import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class ReadSendEditMessages extends AppCompatActivity {

    ConstraintLayout sendMessageLayout, viewMessageLayout;

    WebView readMessage;
    TextView from, to, date, subject;
    ImageView reply, forward, delete, block, send_message_image;
    EditText from_edit_text, to_edit_text, subject_edit_text, message_edit_text;

    Integer messageIdToWork, messageFolderId;
    String messageFrom, messageTo, messageSubject, messageHTML, messageSignature;

    Boolean newMessage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_send_edit_messages);

        viewMessageLayout = (ConstraintLayout) findViewById(R.id.viewMessageLayout);
        sendMessageLayout = (ConstraintLayout) findViewById(R.id.sendMessageLayout);

        messageIdToWork = getIntent().getIntExtra("messageId",0);
        messageFolderId = getIntent().getIntExtra("folderId",0);
        newMessage = getIntent().getBooleanExtra("newMessage",false);

        if (newMessage){
            sendMessageLayout.setVisibility(View.VISIBLE);
            viewMessageLayout.setVisibility(View.INVISIBLE);

        }

        readMessage = (WebView) findViewById(R.id.readMessage);

        from = (TextView)findViewById(R.id.from);
        to = (TextView)findViewById(R.id.to);
        date = (TextView)findViewById(R.id.date);
        subject = (TextView)findViewById(R.id.subject);

        from_edit_text = (EditText) findViewById(R.id.from_edit_text);
        to_edit_text = (EditText) findViewById(R.id.to_edit_text);
        subject_edit_text = (EditText) findViewById(R.id.subject_edit_text);
        message_edit_text = (EditText) findViewById(R.id.message_edit_text);

        from_edit_text.setText(MainActivity.currentUser.email);

        reply = (ImageView) findViewById(R.id.reply);
        forward = (ImageView) findViewById(R.id.forward);
        delete = (ImageView) findViewById(R.id.delete);
        block = (ImageView) findViewById(R.id.block);
        send_message_image = (ImageView) findViewById(R.id.send_message_image);


        // код ниже это на будущее когда буду думать о том как вставлять подпись в письмо
        /*messageSignature = "<p style=\"font-family:open sans,sans-serif; font-size:12px; margin: 0;\">&nbsp;</p>\n" +
                "\n" +
                "<div class=\"tlmail_signature\" mailbox_id=\"52\" style=\"font-family:open sans,sans-serif; font-size:12px; margin:0px;\">\n" +
                "<div>\n" +
                "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:470px\" width=\"470\">\n" +
                "\t<tbody>\n" +
                "\t\t<tr valign=\"top\">\n" +
                "\t\t\t<td style=\"padding-left:10px;width:10px;padding-right:10px\"><img alt=\"photo\" height=\"86\" src=\"https://s3.amazonaws.com/ucwebapp.wisestamp.com/002e2c03-9f4f-4bed-b6a3-55cfe756c78b/7.crop_599x600_0,0.preview.format_png.resize_200x.png#logo\" style=\"border-radius:50%;width:86px;height:86px;max-width:120px\" width=\"86\" /></td>\n" +
                "\t\t\t<td style=\"border-right:1px solid rgb(69,102,142)\">&nbsp;</td>\n" +
                "\t\t\t<td style=\"text-align:initial;font:normal normal normal normal 12px Arial;color:rgb(100,100,100);padding:0px 10px\">\n" +
                "\t\t\t<div>\n" +
                "\t\t\t<p><strong>С уважением, Алик Балмагамбетов</strong></p>\n" +
                "\n" +
                "\t\t\t<p><strong>Cистемный администратор</strong></p>\n" +
                "\n" +
                "\t\t\t<p><strong>ЗАО &quot;ОБЕРОН&quot;</strong></p>\n" +
                "\n" +
                "\t\t\t<p>&nbsp;</p>\n" +
                "\n" +
                "\t\t\t<p><strong>+7 905 708 86 06</strong></p>\n" +
                "\t\t\t</div>\n" +
                "\n" +
                "\t\t\t<div style=\"margin-top:5px\"><strong>balmagambetov.alik@oberon-alfa.ru</strong>\n" +
                "\n" +
                "\t\t\t<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\">\n" +
                "\t\t\t\t<tbody>\n" +
                "\t\t\t\t\t<tr style=\"padding-top:10px\">\n" +
                "\t\t\t\t\t</tr>\n" +
                "\t\t\t\t</tbody>\n" +
                "\t\t\t</table>\n" +
                "\t\t\t</div>\n" +
                "\t\t\t</td>\n" +
                "\t\t</tr>\n" +
                "\t</tbody>\n" +
                "</table>\n" +
                "</div>\n" +
                "</div>";*/

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                moveMessageToFolder(messageIdToWork);

            }
        });
        reply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewMessageLayout.setVisibility(View.INVISIBLE);
                sendMessageLayout.setVisibility(View.VISIBLE);
                to_edit_text.setText(messageFrom);
                subject_edit_text.setText("Re: "+ messageSubject);
            }
        });
        forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewMessageLayout.setVisibility(View.INVISIBLE);
                sendMessageLayout.setVisibility(View.VISIBLE);

                subject_edit_text.setText("Fwd: "+ messageSubject);
            }
        });

        send_message_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessageReplyOrForward();
            }
        });



        getSupportActionBar().hide();
        Log.i("messageId",String.valueOf(messageIdToWork));
        Log.i("folderId",String.valueOf(messageFolderId));
        readMessageOnClick(messageIdToWork);
    }

    void readMessageOnClick(Integer messageId){


        String url = "https://"+ MainActivity.currentUser.server +"/api/2.0/mail/messages/" + String.valueOf(messageId) + "?markRead=true";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                        try {
                            JSONObject resp = response.getJSONObject("response");

                            messageHTML = resp.getString("htmlBody");


                            // адрес того кто мне прислал
                            messageFrom = resp.getString("from");
                            // адрес на который прислали , мой адрес
                            messageTo = resp.getString("to");
                            // дата письма
                            String dateS = resp.getString("date");
                            // тема письма
                            messageSubject = resp.getString("subject");

                            from.setText(from.getText()+messageFrom);
                            to.setText(to.getText()+messageTo);
                            date.setText(date.getText()+dateS);
                            subject.setText(subject.getText()+messageSubject);

                            readMessage.loadData(messageHTML,"text/html; charset=utf-8","UTF-8");

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {

                        error.printStackTrace();

                    }
                })
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Authorization", MainActivity.currentUser.token);
                return params;
            }

        };
        MySingleton.getInstance(ReadSendEditMessages.this).addToRequestQueue(jsonObjectRequest);

    }

    void sendMessageReplyOrForward(){


        //здесь нужен put запрос PUT api/2.0/mail/messages/send
        // при этом нужна новая активность либо просто создавать здесь макет отправки письма,
        // куда нужно ввести адрес получателя, тему писма, сам текст письма
        // и в дальнейшем какие нибудь вложения
        // также нужно разобраться как менять подпись если понадобится

        String url = "https://" + MainActivity.currentUser.server + "/api/2.0/mail/messages/send";
        //String urlTest = "https://oberon-alfa.ru:8080";


        // это нужно для того чтоб применять параметры
        Map<String, String> paramis = new HashMap<String, String>();


        // в дальней
        // тут можно ещё указывать id но я так и не понял дя чего он нужен
        // поскольку я думаю сср сам должен генерировать id писем, в документации написано если не указать будет 0
        // посмотрим после тестов что у нас получится и отпишуст сюда
        // этот метод можно использовать на главном экране почты где отображаются папки писем,


        // здесь в поле from обязательно указание почты currentUser, потому как если указать что-то другое то
        // выдаёт ошибку 400 короче не проходит запрос на отправку письма

        // чтоб создавать новое письмо для отправки
        paramis.put("from", from_edit_text.getText().toString());
        paramis.put("to", to_edit_text.getText().toString());
        paramis.put("subject", subject_edit_text.getText().toString());
        paramis.put("body", message_edit_text.getText().toString());




        //запрос Put на отправку письма
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.PUT/* тут мы указываем тип запроса*/,
                        url,
                        new JSONObject(paramis)/* здесь мы указываем параметры*/,
                        new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                        Log.i(" response ",response.toString());


                        try {
                            Integer statusCode = response.getInt("statusCode");

                            //Log.i("111 StatusCode", statusCode.toString());
                            if (statusCode==200){

                                Toast.makeText(ReadSendEditMessages.this, "Message Send", Toast.LENGTH_SHORT).show();
                                ReadSendEditMessages.super.onBackPressed();

                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            //Log.i("JSONException", e.getMessage());
                        }


                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {

                        error.printStackTrace();





                    }
                })
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Authorization", MainActivity.currentUser.token);
                return params;
            }

        };
        MySingleton.getInstance(ReadSendEditMessages.this).addToRequestQueue(jsonObjectRequest);

        //Log.i(" 111", jsonObjectRequest.toString());

    }

    void moveMessageToFolder(Integer messageId){


        String url = "https://" + MainActivity.currentUser.server + "/api/2.0/mail/messages/";

        String folderName = messageFolderId!=4 ? "move" : "remove";


        // это нужно для того чтоб применять параметры
        Map<String, Integer> paramis = new HashMap<String, Integer>();


        // в дальней
        // список id писем которые хотим переместить
        paramis.put("ids", messageId);

        // id папки в которую хотим переместить, тест на корзину, потом нужно придумать как предоставлять выбор папки
        // тут всё просто если папка не корзина то перемещается в корзину, если корзина то удаляется навсегда

        if (messageFolderId!=4) { paramis.put("folder", 4); }

        //запрос POST при котором возвращается JSONOBJECT с которым уже можно работать и извлекать из него данные
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.PUT/* тут мы указываем тип запроса*/,
                        url + folderName,
                        new JSONObject(paramis)/* здесь мы указываем параметры*/,
                        new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {


                        try {
                            Integer statusCode = response.getInt("statusCode");
                            if (statusCode==200){
                                ReadSendEditMessages.super.onBackPressed();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {

                        //Log.i("111 Error", error.getMessage());
                        error.printStackTrace();

                    }
                })
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Authorization", MainActivity.currentUser.token);
                return params;
            }

        };
        MySingleton.getInstance(ReadSendEditMessages.this).addToRequestQueue(jsonObjectRequest);
    }
}
