package com.alikbalm.oberon_alfaoodm;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.orm.query.Condition;
import com.orm.query.Select;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MainActivity extends AppCompatActivity implements Animation.AnimationListener {

    EditText server, e_mail, password;
    Boolean notUpdatePasswordOnQuestion;

    Button connect;

    String serverUrl, username, passwordText, userEmail, userMailSignature;

    static LogedUsers currentUser;
    static String tokenForThisSession;

    static ArrayList<Integer> listIdStack;
    static HashMap<Integer,String> listIdDateStack;

    static Conf hardcoredListId;

    List<LogedUsers> logedUsersList;

    ImageView imageView, syncImage;

    Animation animation;

    Retrofit retrofit;
    OnlyOfficeApi service;

    // метод прячащий клавиатуру
    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                activity.getCurrentFocus().getWindowToken(), 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().hide();


        server = (EditText) findViewById(R.id.server);
        e_mail = (EditText) findViewById(R.id.e_mail);
        password = (EditText) findViewById(R.id.password);

        connect = (Button) findViewById(R.id.connect);
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                notUpdatePasswordOnQuestion = false;
                currentUser = null;
                clearDBOnBackPressed();

                startConnectingAnimation();

                String serverFromInput = server.getText().toString();

                serverUrl = serverFromInput.contains("https://") ?
                        serverFromInput :
                        "https://" + serverFromInput;
                serverUrl = serverUrl.endsWith("/") ?
                        serverUrl :
                        serverUrl + "/";

                username = e_mail.getText().toString();
                passwordText = password.getText().toString();

                if (serverUrl.equals("https://")) {

                    Toast.makeText(MainActivity.this, R.string.server_required, Toast.LENGTH_SHORT).show();
                    stopConnectingAnimation();
                } else if (username == null || username.equals("")) {
                    Toast.makeText(MainActivity.this, R.string.username_required, Toast.LENGTH_SHORT).show();
                    stopConnectingAnimation();
                } else if (passwordText == null || passwordText.equals("")) {
                    Toast.makeText(MainActivity.this, R.string.password_required, Toast.LENGTH_SHORT).show();
                    stopConnectingAnimation();
                } else {
                    checkForUserInLocalDB(serverUrl, username, passwordText);
                }

            }
        });

        hardcoredListId = new Conf();

        logedUsersList = LogedUsers.listAll(LogedUsers.class);

        imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                hideSoftKeyboard(MainActivity.this);

            }
        });

        syncImage = (ImageView) findViewById(R.id.syncImage);
        syncImage.setVisibility(View.INVISIBLE);
        animation = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.rotate);
        animation.setAnimationListener(MainActivity.this);

    }

    void checkForUserInLocalDB(String serverUrl, String username, final String passwordText) {

        // сначала проверяем есть ли такой сервер в лок бд

        List<LogedUsers> serverCheckList = Select.from(LogedUsers.class)
                .where(Condition.prop("server").eq(serverUrl))
                .list();
        if (serverCheckList.size() < 1) {
            // создание нового пользователя потому как нет такого сервера
            // т.е. отправка запроса на сср

            initializeApiService();
            getTokenFromServer();

        } else {
            final List<LogedUsers> serverAndUsernameCheckList = Select.from(LogedUsers.class)
                    .where(Condition.prop("server").eq(serverUrl),
                            Condition.prop("user_name").eq(username))
                    .list();
            if (serverAndUsernameCheckList.size() < 1) {
                // создание нового пользователя потому как с таким сервером нет такого пользователя
                // т.е. отправка запроса на сср

                initializeApiService();
                getTokenFromServer();

            } else {
                List<LogedUsers> serverAndUsernameAndPasswordCheckList = Select.from(LogedUsers.class)
                        .where(Condition.prop("server").eq(serverUrl),
                                Condition.prop("user_name").eq(username),
                                Condition.prop("password").eq(passwordText))
                        .list();
                if (serverAndUsernameAndPasswordCheckList.size() < 1) {
                    // здесь код обновления пароля пользователя который есть в лок бд

                    new AlertDialog.Builder(MainActivity.this)
                            .setIcon(R.drawable.question)
                            .setTitle(R.string.password_change_question)
                            .setMessage(R.string.password_change_question_text)
                            .setPositiveButton(R.string.positive_answer, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    // здесь обновляем пароль для пользователя
                                    // и следовательно нужно запросить новый токен, и возможно новый
                                    // почтовый адресс так что лучше будет удалить пользователя из базы и
                                    // посласть новый запрос
                                    serverAndUsernameCheckList.get(0).delete();
                                    notUpdatePasswordOnQuestion = false;

                                    initializeApiService();

                                    getTokenFromServer();
                                }
                            })
                            .setNegativeButton(R.string.negative_answer, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    currentUser = serverAndUsernameCheckList.get(0);

                                    //goToRootListScreenIfCurrentUserNotNull();

                                    //Log.i("!!! No Нет", currentUser.server + "\n" + currentUser.userName + "\n" + currentUser.password);
                                    notUpdatePasswordOnQuestion = true;


                                    // строчки ниже расскоментировать после проверки отрицательного ответа
                                    initializeApiService();
                                    getTokenFromServer();
                                }
                            })
                            .show();

                } else {
                    // здесь использование пользователя который в базе
                    currentUser = serverAndUsernameAndPasswordCheckList.get(0);

                    //goToRootListScreenIfCurrentUserNotNull();

                    initializeApiService();
                    getTokenFromServer();
                }
            }
        }
    }

    public void showPopUpServer(View view) {

        getLoggedUserListTempForPopUpMenu();

        PopupMenu popup = new PopupMenu(this, view);

        // если бд пустая, т.е. при первом запуске
        if (logedUsersList.size() < 1 || logedUsersList == null) {

            // ничего не делаем так как доставать не из чего

        } else {

            for (LogedUsers user :
                    logedUsersList) {
                popup.getMenu().add(user.server);
            }

        }

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                String selectedItem = menuItem.getTitle().toString();

                server.setText(selectedItem);

                return true;

            }
        });
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.server_menu, popup.getMenu());
        popup.show();

    }

    public void showPopUpEmail(View view) {
        getLoggedUserListTempForPopUpMenu();

        PopupMenu popup = new PopupMenu(this, view);

        if (logedUsersList == null || logedUsersList.size() < 1 ) {

            // ничего не делаем так как доставать не из чего

        } else {

            for (LogedUsers user :
                    logedUsersList) {
                popup.getMenu().add(user.userName);
            }

        }


        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                String selectedItem = menuItem.getTitle().toString();

                e_mail.setText(selectedItem);



                serverUrl = server.getText().toString();
                username = e_mail.getText().toString();

                for (LogedUsers user :
                        logedUsersList) {
                    if (user.userName.equals(selectedItem)) {

                        if (user.server.equals(serverUrl)) {

                            if (user.userName.equals(username)) {
                                password.setText(user.password);
                            }
                        }
                    }
                }



                return true;
            }
        });
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.server_menu, popup.getMenu());
        popup.show();

    }

    void getLoggedUserListTempForPopUpMenu(){
        String
                serverAdress = server.getText().toString(),
                serverUsername = e_mail.getText().toString();
        if (serverAdress.equals("") ){
            // сервер -
            if (serverUsername.equals("")){
                // пользователь -
                logedUsersList = LogedUsers.listAll(LogedUsers.class);
            } else {
                // пользователь +
                logedUsersList = Select.from(LogedUsers.class).where(Condition.prop("user_name").eq(serverUsername)).list();
            }
        } else {
            //сервер +
            if (serverUsername.equals("")){
                // пользователь -
                logedUsersList = Select.from(LogedUsers.class).where(Condition.prop("server").eq(serverAdress)).list();
            } else {
                // пользователь +
                logedUsersList = Select.from(LogedUsers.class).where(Condition.prop("server").eq(serverAdress),Condition.prop("user_name").eq(serverUsername)).list();
            }
        }

    }

    void initializeApiService(){

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(100, TimeUnit.SECONDS)
                .readTimeout(100,TimeUnit.SECONDS).build();


        retrofit = new Retrofit.Builder()
                .baseUrl(serverUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(OnlyOfficeApi.class);
    }


    // этот метод нужно выполнять каждый раз когда логинится пользователь в приожении
    // так как некоторые сервера (почти все ) выдают токен на очень короткий промежуток времени
    // и в результате токен не действителен
    void getTokenFromServer(){

        Call<ResponseBody> getToken = notUpdatePasswordOnQuestion ?
                service.getToken(username, currentUser.password) :
                service.getToken(username, passwordText);

        getToken.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                try {
                    JSONObject resp = new JSONObject(response.body().string());

                    JSONObject r = resp.getJSONObject("response");

                    tokenForThisSession = r.getString("token");

                    getEmailByToken(tokenForThisSession);

                } catch (JSONException e) {

                    Log.i("!!! JSONException", "getTokenFromServer " + e.getMessage());
                    e.printStackTrace();
                    stopConnectingAnimation();

                } catch (IOException e) {

                    Log.i("!!! IOException", "getTokenFromServer " + e.getMessage());
                    e.printStackTrace();
                    stopConnectingAnimation();

                } catch (NullPointerException e) {
                    Log.i("!!! NullPointerExceptio", "getTokenFromServer " + e.getMessage());
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "No answer from server! Check your Username or Password", Toast.LENGTH_SHORT).show();
                    stopConnectingAnimation();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

                Log.i("!!! onFailure", "getTokenFromServer " + t.getMessage());
                Toast.makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                stopConnectingAnimation();
            }
        });
    }

    void getEmailByToken(final String tokenToSend){

        Call<ResponseBody> getEmail = service.getEmailAddress(tokenToSend);

        getEmail.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                try {
                    JSONObject resp = new JSONObject(response.body().string());
                    JSONArray r = resp.getJSONArray("response");
                    if (r.length()<1) {
                        if (currentUser == null) {
                            currentUser = new LogedUsers(serverUrl, username, passwordText,"default.mail@default.default",getString(R.string.mail_signature));
                            currentUser.save();
                        }

                    } else {
                        JSONObject account = r.getJSONObject(0);
                        userEmail = account.getString("email");



                        //JSONObject signature = account.getJSONObject("signature");
                        userMailSignature = /*signature.getString("html") + */getString(R.string.mail_signature);


                        //! теперь у нас есть подпись для почты, для отправки писем, но естьнюанс
                        // если у человека несколько почтовых аккаунтов, то возьмётся первый попавшийся,
                        // а нужно переделать чтоб выходил какой нибудь спискок почтовых адресов
                        // и уже при нажатии на один из них он записывался в базу
                        // также нужно добавить здесь на главном экране кнопку по смене почтвого ящика в локальной бд

                        //Log.i("!!! email", userEmail);

                        if (currentUser == null) {
                            currentUser = new LogedUsers(serverUrl, username, passwordText, userEmail, userMailSignature);
                            currentUser.save();
                        }

                    }

                    goToRootListScreenIfCurrentUserNotNull();

                } catch (JSONException e) {
                    Log.i("!!! JSONException","getEmailByToken " + e.getMessage());
                    e.printStackTrace();
                    stopConnectingAnimation();
                } catch (IOException e) {
                    Log.i("!!! IOException","getEmailByToken " + e.getMessage());
                    e.printStackTrace();
                    stopConnectingAnimation();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.i("!!! onFailure","getEmailByToken " + t.getMessage());
                stopConnectingAnimation();
            }
        });

    }

    static void clearDBOnBackPressed() {
        // метод для очистки базы от данных пользователя, поскольку мы предполагаем что
        // приложение будет завязано на онлайн сервер, и не предполагает хранение документов на устройстве

        // тут нужно что то типа удалить из базы все классы которые наследуются от SugarRecord кроме LogedUsers
        // также нужно очистить стек экранов и словарь с последней синхронизацией

        MailFolders.deleteAll(MailFolders.class);
        MailMessage.deleteAll(MailMessage.class);
        DocumentFolders.deleteAll(DocumentFolders.class);
        DocumentsFiles.deleteAll(DocumentsFiles.class);
        WikiPages.deleteAll(WikiPages.class);
        ContactsOO.deleteAll(ContactsOO.class);
        FolderDocOO.deleteAll(FolderDocOO.class);
        //listIdStack = null;
        //listIdDateStack = null;

    }

    void startConnectingAnimation(){ //!!
        connect.setVisibility(View.INVISIBLE);
        syncImage.startAnimation(animation);
    }
    void stopConnectingAnimation(){ //!!
        connect.setVisibility(View.VISIBLE);
        syncImage.clearAnimation();
        syncImage.setVisibility(View.INVISIBLE);
    }

    void goToRootListScreenIfCurrentUserNotNull(){
        listIdDateStack = new HashMap<>();

        listIdStack = new ArrayList<>();

        listIdStack.add(hardcoredListId.getRoot_screen());

        // чистим поля ввода чтоб при возврате на экран не создавался дубликат пользователя
        server.setText("");
        e_mail.setText("");
        password.setText("");
        logedUsersList = LogedUsers.listAll(LogedUsers.class);

        stopConnectingAnimation();


        Intent intent = new Intent(getApplicationContext(), OOItemListActivity.class);
        startActivity(intent);
    }


    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationEnd(Animation animation) {

    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }
}
