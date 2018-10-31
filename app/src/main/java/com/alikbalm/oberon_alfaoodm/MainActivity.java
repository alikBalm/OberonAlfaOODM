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
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MainActivity extends AppCompatActivity implements Animation.AnimationListener {

    EditText server, e_mail, password;

    Button connect;

    String serverUrl, username, passwordText, token, userEmail;

    static LogedUsers currentUser;

    static ArrayList<Integer> listIdStack;
    // здесь нужно добавить словарь с ключом айди экрана, и значением вермя последней синхронизации
    static HashMap<Integer,String> listIdDateStack;

    static Conf hardcoredListId;

    List<LogedUsers> logedUsersList;

    ImageView imageView, syncImage;

    Animation animation;


    // задача послать get запрос https://office.oberon-alfa.ru/api/2.0/mail/messages/187419 тут id сообщения

    Retrofit retrofit;
    OnlyOfficeApi service;


    /*@Override
    protected void onResume() {

        // тут пока так в дальнейшем нужно будет настроить метод log out
        // для того чтоб можно было на одном устройстве входить на разные сервера
        // и под разными логинами и паролями
        // а пока здесь при нажатии назад из первоначального списка, он сразу выходит из приложения,
        // и если пользователь залогинился то он не попадёт на начальный экран, потребуется переустановка
        // приложения для этого но тогда обнулится вся бд
        super.onBackPressed();
        super.onResume();
    }*/


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

        hardcoredListId = new Conf();

        //listIdStack = new ArrayList<>();

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


        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //startConnectingAnimation();

                /*if (currentUser != null) {
                    if (listIdStack!=null) {
                        currentUser = null;
                    } else {
                        goToRootListScreenIfCurrentUserNotNull();
                    }*/


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

                Log.i("!!! Login Pressed", serverUrl);
                Log.i("!!! Login Pressed", username);
                Log.i("!!! Login Pressed", passwordText);


                if (serverUrl.equals("https://")) {

                    Toast.makeText(MainActivity.this, "Server are required", Toast.LENGTH_SHORT).show();
                    stopConnectingAnimation();
                } else if (username == null || username.equals("")) {
                    Toast.makeText(MainActivity.this, "Username are required", Toast.LENGTH_SHORT).show();
                    stopConnectingAnimation();
                } else if (passwordText == null || passwordText.equals("")) {
                    Toast.makeText(MainActivity.this, "Password are required", Toast.LENGTH_SHORT).show();
                    stopConnectingAnimation();
                } else {


                    // здесь меняем с volley на retrofit

                    // два нижних метода нужно перенести в
                    // метод checkForUserInLocalDB
                    // или нет, нужно над логикой подумать
                    checkForUserInLocalDB(serverUrl, username, passwordText);


                }


            }
        });

    }

    void checkForUserInLocalDB(String serverUrl, String username, String passwordText) {

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
                            .setIcon(R.drawable.ic_launcher_background)
                            .setTitle("?")
                            .setMessage("Password for pair server-username in local DB does not match to entered one. Do you want to update it? \n" +
                                    "If No, password from local DB will be used!")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    // здесь обновляем пароль для пользователя
                                    // и следовательно нужно запросить новый токен, и возможно новый
                                    // почтовый адресс так что лучше будет удалить пользователя из базы и
                                    // посласть новый запрос
                                    serverAndUsernameCheckList.get(0).delete();

                                    initializeApiService();

                                    getTokenFromServer();
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    currentUser = serverAndUsernameCheckList.get(0);

                                    goToRootListScreenIfCurrentUserNotNull();
                                }
                            })
                            .show();

                } else {
                    // здесь использование пользователя который в базе
                    currentUser = serverAndUsernameAndPasswordCheckList.get(0);

                    goToRootListScreenIfCurrentUserNotNull();
                }
            }
        }

        // думаю нужны два временных массива с серверами и логинами из баз
        /*List<LogedUsers> checkList = LogedUsers.listAll(LogedUsers.class);
        for (final LogedUsers user :
                checkList) {
            if (user.server.equals(serverUrl)) {
                if (user.userName.equals(username)) {
                    if (user.password.equals(passwordText)){
                        // если есть в базе такой пользователь то запоминаем его в кеше и переходим на корневой список
                        currentUser = user;
                        goToRootListScreenIfCurrentUserNotNull();

                    } else {
                        // здесь код если не совпадают пароли
                        // нужно добавить алерт диалог с предупреждением что пароль в базе не совпадает с
                        // введеным паролем, и вопрос изменить пароль в базе либо вы ошиблись при вводе?
                        new AlertDialog.Builder(MainActivity.this)
                                .setIcon(R.drawable.ic_launcher_background)
                                .setTitle("?")
                                .setMessage("Password for pair server-username in local DB does not match to entered one. Do you want to update it? \n" +
                                        "If No, password from local DB will be used!")
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        // здесь обновляем пароль для пользователя
                                        // и следовательно нужно запросить новый токен, и возможно новый
                                        // почтовый адресс так что лучше будет удалить пользователя из базы и
                                        // посласть новый запрос
                                        user.delete();

                                        initializeApiService();

                                        getTokenFromServer();
                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        currentUser = user;
                                    }
                                })
                                .show();
                    }
                } else {
                    // здесь код если в этом сервере не такого пользователя
                }
            } else {
                // здесь код если нет такого сервера
            }
        }*/
    }

    // менюшку нужно доработать, а именно добавить возможность вводить новый сервер
    // и записывать его в базу
    // первым делом надо это сделать потому что нужно перес\установить приложение, а для этого нужно иметь возможность ввести
    // сервер логин и пароль для подключения

    // здесь из трёх нижних нужно сделать один метод, но это в дальнейшем, а пока довольствуйтесь тем что есть))))

    // нужно добавить кнопку для очитски полей ввода и также сброса logedUsersList на первоначальный

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

        if (logedUsersList.size() < 1 || logedUsersList == null) {

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

    // это нужно поменять то бишь он вообще не нужен,
    // я имею в виду нижний

    public void showPopUpPassword(View view) {

        PopupMenu popup = new PopupMenu(this, view);

        if (logedUsersList.size() < 1 || logedUsersList == null) {

            // ничего не делаем так как доставать не из чего

        } else {

            serverUrl = server.getText().toString();
            username = e_mail.getText().toString();
            if (serverUrl == null || serverUrl.equals("")) {

                Toast.makeText(MainActivity.this, "Server are required", Toast.LENGTH_SHORT).show();
            } else if (username == null || username.equals("")) {
                Toast.makeText(MainActivity.this, "Username are required", Toast.LENGTH_SHORT).show();
            } else {

                for (LogedUsers user :
                        logedUsersList) {
                    popup.getMenu().add(user.userName + " on "
                            + user.server);
                }
            }

        }


        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                //String selectedItem = menuItem.getTitle().toString();
                for (LogedUsers user :
                        logedUsersList) {
                    if (user.server.equals(serverUrl)) {

                        if (user.userName.equals(username)) {

                            password.setText(user.password);
                            currentUser = user;

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

    void getTokenFromServer(){
        Call<ResponseBody> getToken = service.getToken(username,passwordText);

        getToken.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {

                try {
                    JSONObject resp = new JSONObject(response.body().string());

                    JSONObject r = resp.getJSONObject("response");

                    token = r.getString("token");

                    getEmailByToken(token);

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
            public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {

                try {
                    JSONObject resp = new JSONObject(response.body().string());
                    JSONArray r = resp.getJSONArray("response");
                    if (r.length()<1) {
                        currentUser = new LogedUsers(serverUrl, username, passwordText, token, "default.mail@default.default");
                        currentUser.save();
                    } else {
                        JSONObject account = r.getJSONObject(0);
                        userEmail = account.getString("email");


                        //! кстати отсюда можно и подпись доставать  также как и почту signature

                        Log.i("!!! email", userEmail);

                        currentUser = new LogedUsers(serverUrl, username, passwordText, token, userEmail);
                        currentUser.save();

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
