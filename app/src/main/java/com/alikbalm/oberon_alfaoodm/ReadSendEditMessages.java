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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ReadSendEditMessages extends AppCompatActivity {

    ConstraintLayout sendMessageLayout, viewMessageLayout;

    WebView readMessage;
    TextView from, to, date, subject;
    ImageView reply, forward, delete, block, send_message_image;
    EditText from_edit_text, to_edit_text, subject_edit_text, message_edit_text;

    Integer messageIdToWork, messageFolderId;
    String messageFrom, messageTo, messageSubject, messageHTML, messageSignature;

    Boolean newMessage;

    // Объекты для Retrofit
    Retrofit retrofit;
    OnlyOfficeApi service;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_send_edit_messages);

        retrofit = new Retrofit.Builder()
                .baseUrl(MainActivity.currentUser.server)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(OnlyOfficeApi.class);

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

        Call<ResponseBody> readMessagebyId = service.readMessageById(
                MainActivity.currentUser.token,messageId,true);

        readMessagebyId.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {

                try {
                    JSONObject respo = new JSONObject(response.body().string());
                    JSONObject resp = respo.getJSONObject("response");

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
                    Log.i("!!! JSONException", "readMessageOnClick " + e.getMessage());
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.i("!!! IOException", "readMessageOnClick " + e.getMessage());
                    e.printStackTrace();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.i("!!! onFailure", "readMessageOnClick " + t.getMessage());
            }
        });
    }

    void sendMessageReplyOrForward(){


        // и в дальнейшем какие нибудь вложения
        // также нужно разобраться как менять подпись если понадобится

        Call<ResponseBody> sendMessageNew = service.sendMessage(
                MainActivity.currentUser.token,
                from_edit_text.getText().toString(),
                to_edit_text.getText().toString(),
                subject_edit_text.getText().toString(),
                message_edit_text.getText().toString()
        );

        sendMessageNew.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {

                try {
                    JSONObject respo = new JSONObject(response.body().string());
                    Integer statusCode = respo.getInt("statusCode");
                    if (statusCode==200){
                        Toast.makeText(ReadSendEditMessages.this, "Message Send", Toast.LENGTH_SHORT).show();
                        ReadSendEditMessages.super.onBackPressed();
                    }
                } catch (JSONException e) {
                    Log.i("!!! JSONException", "sendMessageReplyOrForward " + e.getMessage());
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.i("!!! IOException", "sendMessageReplyOrForward " + e.getMessage());
                    e.printStackTrace();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.i("!!! onFailure", "sendMessageReplyOrForward " + t.getMessage());
            }
        });
    }

    void moveMessageToFolder(Integer messageId){

        Call<ResponseBody> moveOrRemoveMessage = service.moveOrRemoveMessage(
                MainActivity.currentUser.token,
                messageFolderId != 4 ? "move" : "remove",
                messageId,
                messageFolderId != 4 ? 4 : null);

        moveOrRemoveMessage.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                try {
                    JSONObject resp = new JSONObject(response.body().string());
                    Integer statusCode = resp.getInt("statusCode");
                    if (statusCode == 200) {
                        Toast.makeText(ReadSendEditMessages.this, "Message Deleted", Toast.LENGTH_SHORT).show();
                        ReadSendEditMessages.super.onBackPressed();
                    }
                } catch (JSONException e) {
                    Log.i("!!! JSONException", "moveMessageToFolder " + e.getMessage());
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.i("!!! IOException", "moveMessageToFolder " + e.getMessage());
                    e.printStackTrace();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.i("!!! onFailure", "moveMessageToFolder " + t.getMessage());
            }
        });
    }

}
