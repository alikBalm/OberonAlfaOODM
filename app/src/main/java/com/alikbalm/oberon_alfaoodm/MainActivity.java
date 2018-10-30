package com.alikbalm.oberon_alfaoodm;


import android.app.Activity;
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
                connect.setVisibility(View.INVISIBLE);
                syncImage.startAnimation(animation);

                if (currentUser != null) {
                    listIdDateStack = new HashMap<>();

                    listIdStack = new ArrayList<>();

                    listIdStack.add(hardcoredListId.getRoot_screen());

                    // чистим поля ввода чтоб при возврате на экран не создавался дубликат пользователя
                    server.setText("");
                    e_mail.setText("");
                    password.setText("");
                    logedUsersList = LogedUsers.listAll(LogedUsers.class);

                    connect.setVisibility(View.VISIBLE);
                    syncImage.clearAnimation();
                    syncImage.setVisibility(View.INVISIBLE);


                    Intent intent = new Intent(getApplicationContext(), OOItemListActivity.class);
                    startActivity(intent);

                } else {

                    String serverFromInput = server.getText().toString();

                    serverUrl = serverFromInput.contains("https://") ?
                            serverFromInput:
                            "https://" + serverFromInput;
                    serverUrl = serverFromInput.endsWith("/") ?
                            serverFromInput:
                            serverFromInput + "/";

                    username = e_mail.getText().toString();
                    passwordText = password.getText().toString();


                    if (serverUrl == null || serverUrl.equals("https:///")) {

                        Toast.makeText(MainActivity.this, "Server are required", Toast.LENGTH_SHORT).show();
                        connect.setVisibility(View.VISIBLE);
                        syncImage.clearAnimation();
                        syncImage.setVisibility(View.INVISIBLE);
                    } else if (username == null || username.equals("")) {
                        Toast.makeText(MainActivity.this, "Username are required", Toast.LENGTH_SHORT).show();
                        connect.setVisibility(View.VISIBLE);
                        syncImage.clearAnimation();
                        syncImage.setVisibility(View.INVISIBLE);
                    } else if (passwordText == null || passwordText.equals("")) {
                        Toast.makeText(MainActivity.this, "Password are required", Toast.LENGTH_SHORT).show();
                        connect.setVisibility(View.VISIBLE);
                        syncImage.clearAnimation();
                        syncImage.setVisibility(View.INVISIBLE);
                    } else {


                        connect.setVisibility(View.INVISIBLE);
                        syncImage.setVisibility(View.VISIBLE);
                        // здесь меняем с volley на retrofit

                        // два нижних метода нужно перенести в
                        // метод checkForUserInLocalDB
                        // или нет нужно над логикой подумать
                        initializeApiService();

                        getTokenFromServer();



                    }

                }
            }
        });

    }

    void checkForUserInLocalDB(String serverUrl, String username, String passwordText) {
        List<LogedUsers> checkList = LogedUsers.listAll(LogedUsers.class);
        for (LogedUsers user :
                checkList) {
            if (serverUrl.equals(user.server)) {
                if (username.equals(user.userName)) {
                    if (passwordText.equals(user.password)){
                        // здесь выводим алерт диалог
                        // о том что в бд есть уже пользователь с такими сервером логином и паролем
                        // и вопрос использовать его или нет
                    } else {
                        // здесь код если не совпадают пароли
                        // нужно добавить алерт диалог с предупреждением что пароль в базе не совпадает с
                        // введеным паролем, и вопрос изменить пароль в базе либо вы ошиблись при вводе?
                    }
                }
            }
        }
    }

    // менюшку нужно доработать, а именно добавить возможность вводить новый сервер
    // и записывать его в базу
    // первым делом надо это сделать потому что нужно перес\установить приложение, а для этого нужно иметь возможность ввести
    // сервер логин и пароль для подключения

    // здесь из трёх нижних нужно сделать один метод, но это в дальнейшем, а пока довольствуйтесь тем что есть))))

    // нужно добавить кнопку для очитски полей ввода и также сброса logedUsersList на первоначальный

    public void showPopUpServer(View view) {


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

                List<LogedUsers> tempList = new ArrayList<>();
                for (LogedUsers user :
                        logedUsersList) {
                    if (user.server.equals(selectedItem)) {

                        tempList.add(user);
                    }
                }
                logedUsersList = tempList;

                return true;

            }
        });
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.server_menu, popup.getMenu());
        popup.show();

    }

    public void showPopUpEmail(View view) {

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

                List<LogedUsers> tempList = new ArrayList<>();

                serverUrl = server.getText().toString();
                username = e_mail.getText().toString();

                for (LogedUsers user :
                        logedUsersList) {
                    if (user.userName.equals(selectedItem)) {

                        if (user.server.equals(serverUrl)) {

                            if (user.userName.equals(username)) {

                                password.setText(user.password);
                                currentUser = user;

                            }
                        }
                    }
                }
                logedUsersList = tempList;


                return true;
            }
        });
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.server_menu, popup.getMenu());
        popup.show();

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

                    e.printStackTrace();
                    connect.setVisibility(View.VISIBLE);
                    syncImage.clearAnimation();
                    syncImage.setVisibility(View.INVISIBLE);

                } catch (IOException e) {

                    e.printStackTrace();
                    connect.setVisibility(View.VISIBLE);
                    syncImage.clearAnimation();
                    syncImage.setVisibility(View.INVISIBLE);

                } catch (NullPointerException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "No answer from server! Check your Username or Password", Toast.LENGTH_SHORT).show();
                    connect.setVisibility(View.VISIBLE);
                    syncImage.clearAnimation();
                    syncImage.setVisibility(View.INVISIBLE);
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

                Log.i("!!! Error", t.getMessage());
                Toast.makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                connect.setVisibility(View.VISIBLE);
                syncImage.clearAnimation();
                syncImage.setVisibility(View.INVISIBLE);
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

                    listIdDateStack = new HashMap<>();

                    listIdStack = new ArrayList<>();

                    listIdStack.add(hardcoredListId.getRoot_screen());

                    // чистим поля ввода чтоб при возврате на экран не создавался дубликат пользователя
                    server.setText("");
                    e_mail.setText("");
                    password.setText("");

                    // убираем анимацию
                    connect.setVisibility(View.VISIBLE);
                    syncImage.clearAnimation();
                    syncImage.setVisibility(View.INVISIBLE);
                    // конец уборки анимации можно перенести в отдельный метод но пока и так сойдёт)))))

                    Intent intent = new Intent(getApplicationContext(), OOItemListActivity.class);
                    startActivity(intent);

                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.i("!!! onFailure", t.getMessage());

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
        listIdStack = null;
        listIdDateStack = null;

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
