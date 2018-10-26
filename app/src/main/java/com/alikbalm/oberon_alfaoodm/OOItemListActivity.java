package com.alikbalm.oberon_alfaoodm;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.orm.query.Condition;
import com.orm.query.Select;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class OOItemListActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    //  в этой активности будет отображаться список из пунктов почта документы википедия и т.п.
    // на данный экран будет перекидывать только при успешном подключении к сср и уже имея токен для запросов всего необходимого
    ConstraintLayout createFileFolderDocs;
    Spinner spinnerForFileType;
    String fileTypeFromSpinner;

    ListView listView;
    CustomListAdapter cla;

    ImageView syncImage, logoutImage, sendMessageImage, createFolderImage;

    File fileToUpload;

    //Calendar rightNow = Calendar.getInstance();
    TextView lastSyncData, textViewForType;

    EditText createTitle, createTextForTXT;

    Button createButton, uploadFile;

    // эта перем нужна для того чтоб можно было создавать папки и файлы внутри неё
    // для остальных папок есть айди, а эта инициируется через 11 или @my
    Integer myDocumentsFolderId;

    ArrayList<String> itemString;
    ArrayList<Integer> itemPng;
    ArrayList<Integer> itemId;
    ArrayList<Boolean> itemTypeFolder;

    // это нужно для хождения по папкам в документах, потому как там могут быть пустые папки
    // эта переменная меняется в методе GetFolderFoldersAndFilesByListId, и в зависимости от неё
    // строится список папок и файлов в подпапках в методе initializeArraylistsForListView
    Boolean noFoldersInDocFoldersOnOO, noFilesInDocFoldersOnOO;

    // Объекты для Retrofit
    Retrofit retrofit;
    OnlyOfficeApi service;

    // эти два метода нужны для спиннера при выборе типа создаваемого документа <<<
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {

        String item = adapterView.getItemAtPosition(pos).toString();
        createTextForTXT.setVisibility( item.equals("txt") ? View.VISIBLE : View.INVISIBLE );
        fileTypeFromSpinner = item;

    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
    // вот здесь конец двух методов <<<

    // думаю создать какой нибудь id списков чтоб при нажатии назад в зависимости от
    // него строился новый спискок или выход на главный экран
    // также нужно изменить построение списка чтоб было на основе этого самого id

    @Override
    public void onBackPressed() {

        if (createFileFolderDocs.getVisibility() == View.VISIBLE) {
            createFileFolderDocs.setVisibility(View.INVISIBLE);
            clearCreationEditTexts();
        } else if (getListIndex() == 0) {

            //Log.i("111 List Index onBack" , String.valueOf(getListIndex()));
            MainActivity.currentUser = null;
            MainActivity.clearDBOnBackPressed();
            super.onBackPressed();

        } else {

            Log.i("111 List Index remove" , String.valueOf(getListIndex()));
            removeCurrentListIndexFromStack();
            Log.i("111 List Index remove" , String.valueOf(getListIndex()));

            initializeArraylistsForListView(getListIndex());

        }

    }

    void initializeFileFromUri(Uri uri) {

        String filePath = null;
        if ("content".equals(uri.getScheme())) {
            Cursor cursor = this.getContentResolver().query(uri,
                    new String[]{android.provider.MediaStore.Images.ImageColumns.DATA},
                    null,
                    null,
                    null);
            cursor.moveToFirst();
            filePath = cursor.getString(0);
            cursor.close();
        } else {
            filePath = uri.getPath();
        }
        fileToUpload = new File(filePath);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (data!=null) {

            initializeFileFromUri(data.getData());

            //Log.i("filePath", fileToUpload.getPath());

            uploadFileToMyDocFolder(getListIndex());

            //super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ooitem_list);//android.R.id.list

        getSupportActionBar().hide();

        noFoldersInDocFoldersOnOO = false;
        noFilesInDocFoldersOnOO = false;

        lastSyncData = (TextView) findViewById(R.id.lastSyncData);

        textViewForType = (TextView)findViewById(R.id.textViewForType);

        createTitle = (EditText) findViewById(R.id.createTitle);
        createTextForTXT = (EditText) findViewById(R.id.createTextForTXT);

        uploadFile = (Button) findViewById(R.id.uploadFile);

        uploadFile.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                        i.setType("file/*");
                        startActivityForResult(i, 1);
                    }
                }
        );


        createButton = (Button) findViewById(R.id.createButton);

        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // здесь обрабатываем создание папки или файла на сср

                String type = textViewForType.getText().toString();
                String title = createTitle.getText().toString();
                if (title.equals("")) {
                    Toast.makeText(OOItemListActivity.this, "Title required!", Toast.LENGTH_SHORT).show();
                } else {

                    /*

                     */
                    if (type.equals("Folder")) {

                        createFolderInDocs(getListIndex());
                        clearCreationEditTexts();
                        createFileFolderDocs.setVisibility(View.INVISIBLE);

                    } else if (type.equals("Document")) {

                        createFileInDocs(getListIndex());
                        clearCreationEditTexts();
                        createFileFolderDocs.setVisibility(View.INVISIBLE);

                    }
                }



            }
        });

        Log.i("111 token", MainActivity.currentUser.token);

        syncImage = (ImageView) findViewById(R.id.syncImage);

        syncImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {



                syncScreenWithOO();


            }
        });
        logoutImage = (ImageView) findViewById(R.id.logoutImage);

        logoutImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                MainActivity.currentUser = null;
                MainActivity.listIdStack = new ArrayList<>();
                OOItemListActivity.super.onBackPressed();


            }
        });

        sendMessageImage = (ImageView) findViewById(R.id.sendMessageImage);

        sendMessageImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(getApplicationContext(), ReadSendEditMessages.class);
                intent.putExtra("newMessage", true);
                startActivity(intent);

            }
        });

        createFolderImage = (ImageView) findViewById(R.id.createFolderImage);



        createFileFolderDocs = (ConstraintLayout) findViewById(R.id.createFileFolderDocs);
        createFileFolderDocs.setVisibility(View.INVISIBLE);
        spinnerForFileType = (Spinner) findViewById(R.id.spinnerForFileType);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.file_type_to_add, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerForFileType.setAdapter(spinnerAdapter);
        spinnerForFileType.setOnItemSelectedListener(OOItemListActivity.this);







        listView = (ListView) findViewById(R.id.list);


        // инициализация первого экрана почта документы ...

        initializeArraylistsForListView(getListIndex());




        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                String selectedItemName = itemString.get(position);


                // Давайка мы сделаем через id списка

                if (getListIndex() == MainActivity.hardcoredListId.getRoot_screen()) {
                    switch (selectedItemName) {
                        case "Почта":
                            MainActivity.listIdStack.add(MainActivity.hardcoredListId.getRoot_mail());
                            initializeArraylistsForListView(getListIndex());
                            //inititalizeMailFolders();
                            break;
                        case "Документы":

                            // отрисовка списка папок и документов для документов

                            MainActivity.listIdStack.add(MainActivity.hardcoredListId.getRoot_documents());
                            //initializeDocumentFolders();
                            initializeArraylistsForListView(getListIndex());

                            break;
                        case "Википедия":

                            // отрисовка списка википедии
                            /*
                            addListIdAndPutSyncTime(8);
                            initializeArraylistsForListView(getListIndex());
                             */
                            break;
                        case "Контакты":

                            // отрисовка списка контактов
                            /*
                            addListIdAndPutSyncTime(9);
                            initializeArraylistsForListView(getListIndex());
                             */
                            break;
                        default:
                            break;
                    }
                } else if (getListIndex() >= MainActivity.hardcoredListId.getMail_inbox() && getListIndex() <= MainActivity.hardcoredListId.getMail_spam()) {
                    // сообщения папок почты
                    // тут нужно проработать нажатие на одно из сообщений
                    Intent intent = new Intent(getApplicationContext(), ReadSendEditMessages.class);
                    intent.putExtra("messageId", itemId.get(position));
                    intent.putExtra("folderId", getListIndex());
                    startActivity(intent);


                } else if (getListIndex() == MainActivity.hardcoredListId.getRoot_documents()) {
                    // папки документов
                    MainActivity.listIdStack.add(position+11);
                    initializeArraylistsForListView(getListIndex());

                } else if (getListIndex() == MainActivity.hardcoredListId.getRoot_mail()) {
                    // папки почты
                    //Toast.makeText(OOItemListActivity.this, "Toast on " + selectedItemName, Toast.LENGTH_SHORT).show();
                    MainActivity.listIdStack.add(position + 1);
                    initializeArraylistsForListView(getListIndex());
                    //getMessagesFromOOByListId(getListIndex());
                }  else if (getListIndex() >= MainActivity.hardcoredListId.getDocuments_mydoc() && getListIndex() <= MainActivity.hardcoredListId.getDocuments_trash()) {
                    // хождение по папкам документов и открытие файлов

                    if (itemTypeFolder.get(position)) {
                        MainActivity.listIdStack.add(itemId.get(position));
                        initializeArraylistsForListView(getListIndex());
                    } else {

                        // открываем сами файлы, если не можем скачиваем, напримен файл с расширением .bin или .exe
                        openDocumentFromList(position);

                    }
                } else {
                    // это для папок внутри папок внутри папок в документах  ))))

                    if (itemTypeFolder.get(position)) {

                        // здесь открытие папки

                        MainActivity.listIdStack.add(itemId.get(position));
                        initializeArraylistsForListView(getListIndex());
                    } else {
                        // здесь открытие документа
                        openDocumentFromList(position);

                    }
                }


            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int position, long l) {
                if (getListIndex() >= 1 && getListIndex() <= 5) {
                    // сначала сделать AlertDialog
                    // затем указать в нём метод удаления письма используя messageId(position)
                    String folderName = getListIndex() != 4 ? "move to Trash" : "Delete";

                    new AlertDialog.Builder(OOItemListActivity.this)
                            .setTitle("Are you sure?")
                            .setMessage("Do you definitly want to " + folderName + " ?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //Toast.makeText(OOItemListActivity.this, "Yes button pressed", Toast.LENGTH_SHORT).show();
                                    moveMessageToTrashOrDelete(itemId.get(position));
                                }
                            })
                            .setNegativeButton("No", null)
                            .show();
                }

                return true;
            }
        });

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(100, TimeUnit.SECONDS)
                .readTimeout(100,TimeUnit.SECONDS).build();


        retrofit = new Retrofit.Builder()
                .baseUrl("https://" + MainActivity.currentUser.server)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(OnlyOfficeApi.class);
    }

    void openDocumentFromList(Integer position) {
        Intent intent = new Intent(getApplicationContext(),DocFilesReadWriteSaveDownload.class);

        intent.putExtra("webUrl",
                itemPng.get(position) == R.drawable.jpg ||
                        itemPng.get(position) == R.drawable.png ?
                        getFileViewUrlById(itemId.get(position)) :
                        getFileWebUrlById(itemId.get(position))) ;
        startActivity(intent);
    }

    void uploadFileToMyDocFolder(Integer folderId){



        // думаю нужно попробовать retrofit если ок тогда изучить эту библиотеку
        // и заменить все запросы с volley на retrofit

        folderId =
                folderId == MainActivity.hardcoredListId.getDocuments_mydoc()
                && myDocumentsFolderIdPresents()
                ? myDocumentsFolderId
                : folderId;

        // Создаем RequestBody
        RequestBody requestFile =
                RequestBody.create(MediaType.parse("multipart/form-data"), fileToUpload);

        // MultipartBody.Part используется, чтобы передать имя файла
        MultipartBody.Part body =
                MultipartBody.Part.createFormData("picture", fileToUpload.getName(), requestFile);

        // Добавляем описание
        String descriptionString = "hello, this is description speaking";
        RequestBody description =
                RequestBody.create(
                        MediaType.parse("multipart/form-data"), descriptionString);




        // Выполняем запрос
        Call<ResponseBody> uploadFile = service.uploadFile(MainActivity.currentUser.token,folderId,description,body);
        uploadFile.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                    Toast.makeText(OOItemListActivity.this, "File Upload Success", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                Log.i("!!! onFailure", t.getMessage());
            }
        });


        /* это способ с volley который так и не заработал
        //String testUrl = "https://oberon-alfa.ru:8080";


        String url = "https://"+ MainActivity.currentUser.server +"/api/2.0/files/482/upload";

        Map<String, String> paramis = new HashMap<String, String>();

        paramis.put("data", fileUrl);

        paramis.put("Content-Disposition", "form-data");

        //paramis.put("filename", "network.png");

        paramis.put("name", "network.png");

        paramis.put("filename","network.png");

        paramis.put("Content-Type", "image/png");



        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, new JSONObject(paramis),  new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                        Log.i(" Upload File ", response.toString());

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {

                        if (error instanceof NoConnectionError) {
                            Toast.makeText(OOItemListActivity.this, "Error while connecting to "+ MainActivity.currentUser.server + "\nTry Sync Later!", Toast.LENGTH_SHORT).show();
                            syncImage.setImageResource(R.drawable.sync2);
                        }
                        error.printStackTrace();
                        //Log.i(" Error", error.getMessage());
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();


                //params.put("accept", "application/json");
                //params.put("accept-encoding", "gzip, deflate");
                //params.put("accept-language", "en-US,en;q=0.8");
                params.put("content-type", "multipart/form-data");
                params.put("authorization", MainActivity.currentUser.token);

                return params;
            }

        };
        MySingleton.getInstance(OOItemListActivity.this).addToRequestQueue(jsonObjectRequest);*/

        //syncScreenWithOO();


    }

    void createFileInDocs(Integer screenId){
        //   POST api/2.0/files/{folderId}/file api/2.0/files/{folderId}/text

        String typeInUrl, fileTitle;

        typeInUrl = fileTypeFromSpinner.equals("txt") ? "text" : "file" ;

        Integer parenFolderId = screenId == MainActivity.hardcoredListId.getDocuments_mydoc() ? myDocumentsFolderId : screenId;

        fileTitle = createTitle.getText().toString() + MainActivity.hardcoredListId.fileTypeForCreation.get(fileTypeFromSpinner);

        String url = "https://"+ MainActivity.currentUser.server +"/api/2.0/files/"+ parenFolderId.toString() +"/"+ typeInUrl ;

        Map<String, String> paramis = new HashMap<String, String>();

        paramis.put("title", fileTitle);
        if (typeInUrl.equals("text")) { paramis.put("content", createTextForTXT.getText().toString()); }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, new JSONObject(paramis),  new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                        Log.i(" Create File ", response.toString());

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {

                        if (error instanceof NoConnectionError) {
                            Toast.makeText(OOItemListActivity.this, "Error while connecting to "+ MainActivity.currentUser.server + "\nTry Sync Later!", Toast.LENGTH_SHORT).show();
                            syncImage.setImageResource(R.drawable.sync2);
                        }
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
        MySingleton.getInstance(OOItemListActivity.this).addToRequestQueue(jsonObjectRequest);

        syncScreenWithOO();

    }

    // этот метод создаёт папку на сср в папке список которой отображается на экране
    // нужно добавить возможность вводить имя новой папки потому как сейчас это захардкоренно
    // в переменной folderTitle
    void createFolderInDocs(final Integer screenId){

        Integer parenFolderId = screenId == MainActivity.hardcoredListId.getDocuments_mydoc() ? myDocumentsFolderId : screenId;
        String folderTitle = createTitle.getText().toString();

        String url = "https://"+ MainActivity.currentUser.server +"/api/2.0/files/folder/"+
                parenFolderId.toString();

        Map<String, String> paramis = new HashMap<String, String>();
        paramis.put("title", folderTitle);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, new JSONObject(paramis),  new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                        Log.i(" Create Folder ", response.toString());

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {

                        if (error instanceof NoConnectionError) {
                            Toast.makeText(OOItemListActivity.this, "Error while connecting to "+ MainActivity.currentUser.server + "\nTry Sync Later!", Toast.LENGTH_SHORT).show();
                            syncImage.setImageResource(R.drawable.sync2);
                        }
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
        MySingleton.getInstance(OOItemListActivity.this).addToRequestQueue(jsonObjectRequest);

        syncScreenWithOO();


    }

    // этот метод получает один из айди папок или списков, в данном случае они совпадают
    // делает get запрос на сср и обновляет базу
    void getMessagesFromOOByListId(final Integer folderId) {

        Boolean emptyMessageBD = false;
        try {
            List<MailMessage> messagesToUpdate = Select.from(MailMessage.class).where(Condition.prop("folder_id").eq(folderId)).list();
            if (messagesToUpdate == null || messagesToUpdate.size() < 1) {
                emptyMessageBD = true;
            }
        } catch (SQLiteException e) {
            Log.i("SQLLITE NO SUCH TABLE", e.getMessage());
            emptyMessageBD = true;
        }

        String url = "https://" +
                MainActivity.currentUser.server + "/api/2.0/mail/messages?folder=" + String.valueOf(folderId) + "&page_size=100";
        final Boolean finalEmptyMessageBD = emptyMessageBD;


        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        if (!finalEmptyMessageBD) { // если не пустая база

                            //это пока нет обновления а просто очистка базы и заполнение новыми значениями

                            MailMessage.clearTableByScreenId(folderId);

                        }

                        try {
                            JSONArray resp = response.getJSONArray("response");
                            // здесь у нас есть массив json в который входят 5 объектов json
                            // выполняем foreach и заполняем базу для дальнейшего расположения на экране
                            for (int j = 0; j < resp.length(); j++) {

                                JSONObject messageObject = resp.getJSONObject(j);

                                Integer messageId = messageObject.getInt("id");
                                Integer messageFolderId = messageObject.getInt("folder");
                                String messageSubject = messageObject.getString("subject");
                                String messageReceivedDate = messageObject.getString("receivedDate");
                                Boolean messageIsNew = messageObject.getBoolean("isNew");

                                new MailMessage(messageId, messageFolderId, messageSubject, messageReceivedDate, messageIsNew).save();

                            }

                            List<MailFolders> findNameByFolderId = Select.from(MailFolders.class).where(Condition.prop("folder_id").eq(folderId)).list();
                            Toast.makeText(OOItemListActivity.this, findNameByFolderId.get(0).folderName + " Synchronized", Toast.LENGTH_SHORT).show();
                            initializeArraylistsForListView(getListIndex());


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

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
        MySingleton.getInstance(OOItemListActivity.this).addToRequestQueue(jsonObjectRequest);


    }

    Boolean checkDocsClassBDNotEmpty(Class SugarOrmClass, String listIdColumn) {
        Boolean returnBoolean = true;
        try {
            List<Class> classToCheck = Select.from(SugarOrmClass).where(Condition.prop(listIdColumn).eq(getListIndex())).list();
            if (classToCheck == null || classToCheck.size() < 1) {
                returnBoolean = false;
            }
        } catch (SQLiteException e) {
            Log.i("SQLLITE NO SUCH TABLE", e.getMessage());
            returnBoolean = false;
        }
        return returnBoolean;
    }

    void clearDocFoldersAndFilesOfCurrentScreenId(){

        // удаление из бд работает хорошо
        //Log.i(" Not Empty Folders",checkDocsClassBDNotEmpty(DocumentFolders.class,"doc_folder_list_id").toString() );

        if (checkDocsClassBDNotEmpty(DocumentFolders.class,"doc_folder_list_id")) {

            DocumentFolders.clearTableByScreenId(getListIndex());
            //Log.i(" Not Empty Folders",checkDocsClassBDNotEmpty(DocumentFolders.class,"doc_folder_list_id").toString() );

        }
        //Log.i(" Not Empty Files",checkDocsClassBDNotEmpty(DocumentsFiles.class,"doc_file_list_id").toString() );
        if (checkDocsClassBDNotEmpty(DocumentsFiles.class,"doc_file_list_id")) {

            DocumentsFiles.clearTableByScreenId(getListIndex());
            //Log.i(" Not Empty Folders",checkDocsClassBDNotEmpty(DocumentsFiles.class,"doc_file_list_id").toString() );

        }

        // конец удаления
    }

    String docFolderNameForHttpGET(Integer id){
        String docFolderName;
        switch (id) {
            case 11 :
                docFolderName = "@my";
                break;
            case 12 :
                docFolderName = "@share";
                break;
            case 13 :
                docFolderName = "@common";
                break;
            case 14 :
                docFolderName = "@projects";
                break;
            case 15 :
                docFolderName = "@trash";
                break;
            default:
                docFolderName = String.valueOf(id);
                break;
        }
        return docFolderName;
    }

    void GetFolderFoldersAndFilesByListId(final Integer id){

        noFoldersInDocFoldersOnOO = false;
        noFilesInDocFoldersOnOO = false;

        String url = "https://" + MainActivity.currentUser.server + "/api/2.0/files/" + docFolderNameForHttpGET(id);

        // get запрос

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                        // если базы не пустые

                        clearDocFoldersAndFilesOfCurrentScreenId();

                        // здесь работаем на json ответом

                        try {

                            JSONObject resp = response.getJSONObject("response");
                            if (id == MainActivity.hardcoredListId.getDocuments_mydoc()) {
                                // тут извлекаем current id и приравниваем его к myDocumentsFolderId
                                JSONObject current = resp.getJSONObject("current");
                                myDocumentsFolderId = current.getInt("id");
                            }
                            // работа с массивом папок
                            JSONArray foldersArray = resp.getJSONArray("folders");
                            if (foldersArray.length() < 1) {
                                noFoldersInDocFoldersOnOO = true;
                            } else {
                                for (int i = 0; i < foldersArray.length(); i++) {

                                    JSONObject folder = foldersArray.getJSONObject(i);
                                    DocumentFolders docFolder = new DocumentFolders(folder.getInt("id"), folder.getString("title"));
                                    docFolder.docFolderListId = id;
                                    docFolder.save();

                                }
                            }
                            // работа с массивом файлов
                            JSONArray filesArray = resp.getJSONArray("files");
                            if (filesArray.length() < 1) {
                                noFilesInDocFoldersOnOO = true;
                            } else {
                                for (int i = 0; i < filesArray.length(); i++) {

                                    JSONObject file = filesArray.getJSONObject(i);
                                    DocumentsFiles docFile = new DocumentsFiles(
                                            file.getInt("id"),
                                            file.getString("title"),
                                            file.getString("fileExst"),
                                            file.getString("viewUrl"),
                                            file.getString("webUrl"));
                                    docFile.docFileListId = id;
                                    docFile.save();

                                }
                            }
                                initializeArraylistsForListView(id);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {

                        if (error instanceof NoConnectionError) {
                            Toast.makeText(OOItemListActivity.this, "Error while connecting to "+ MainActivity.currentUser.server + "\nTry Sync Later!", Toast.LENGTH_SHORT).show();
                            syncImage.setImageResource(R.drawable.sync2);
                        }
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
        MySingleton.getInstance(OOItemListActivity.this).addToRequestQueue(jsonObjectRequest);



    }

    // метод для чтения писем по id письма
    // думаю нужно переходить на новую активность чтоб в дальнейшем уже добавить функцию отправки
    // ответа и т.д. как в полноценном почтовом агенте

    void inititalizeMailFolders() {

        //MailFolders.deleteAll(MailFolders.class);


        // здесь нужно сделать запрос к сср чтоб получить список папок , их параметры ( количество писем прочита и новых, их айди и т.д)
        // сделать запись к бд при помощи класса MailFolders , и расположить их на экране в виде списка


        // не нужно удалять, нужно проверить если есть записи то обновить количество
        // прочитанных и не очень прочитанных)) всех папок, если нет папок то выполнить get запрос

        //делаем выборку из локальной бд
        // тут нужно обработать исключения если запуск первый раз и нет никакой MailFolders в бд
        Log.i("111 InitialMailFolders" , String.valueOf(getListIndex()));

        Boolean emptyFolderBD = false;
        try {
            List<MailFolders> foldersToUpdate = Select.from(MailFolders.class).list();
            if (foldersToUpdate == null || foldersToUpdate.size() < 5) {
                emptyFolderBD = true;
            }
        } catch (SQLiteException e) {
            Log.i("SQLLITE NO SUCH TABLE", e.getMessage());
            emptyFolderBD = true;
        }


        String url = "https://" + MainActivity.currentUser.server + "/api/2.0/mail/folders";// сам запрос
        final Boolean finalEmptyFolderBD = emptyFolderBD;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                        try {
                            JSONArray resp = response.getJSONArray("response");
                            // здесь у нас есть массив json в который входят 5 объектов json
                            // выполняем foreach и заполняем базу для дальнейшего расположения на экране
                            for (int j = 0; j < resp.length(); j++) {

                                JSONObject folderObject = resp.getJSONObject(j);
                                Integer id = (Integer) folderObject.getInt("id");
                                Integer unread_messages = (Integer) folderObject.getInt("unread_messages");
                                Integer total_count = (Integer) folderObject.getInt("total_count");

                                if (finalEmptyFolderBD) {
                                    new MailFolders(id, unread_messages, total_count).save();
                                } else {
                                    List<MailFolders> folderToUpdate = Select.from(MailFolders.class).where(Condition.prop("folder_id").eq(id)).list();
                                    folderToUpdate.get(0).unreadCount = unread_messages;
                                    folderToUpdate.get(0).totalCount = total_count;
                                    folderToUpdate.get(0).save();
                                }
                            }

                            initializeArraylistsForListView(getListIndex());

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error

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
        MySingleton.getInstance(OOItemListActivity.this).addToRequestQueue(jsonObjectRequest);

    }

    void initializeDocumentFolders(){


        // здесь инициируем вид папок документов
        // мои документы, доступно для меня, Общие документы, документы проекта, корзина
        // my , share , common , projects , trash


        // проверка на случай существования папок


        try {
            List<FolderDocOO> docFoldersToUpdate = Select.from(FolderDocOO.class).list();
            if (docFoldersToUpdate == null || docFoldersToUpdate.size() < 5) {
                new FolderDocOO("my").save();
                new FolderDocOO("share").save();
                new FolderDocOO("common").save();
                new FolderDocOO("projects").save();
                new FolderDocOO("trash").save();

                initializeArraylistsForListView(getListIndex());
            }
            else {

                initializeArraylistsForListView(getListIndex());

            }
        } catch (SQLiteException e) {
            Log.i("SQLLITE NO SUCH TABLE", e.getMessage());

            new FolderDocOO("my").save();
            new FolderDocOO("share").save();
            new FolderDocOO("common").save();
            new FolderDocOO("projects").save();
            new FolderDocOO("trash").save();

            initializeArraylistsForListView(getListIndex());
        }







    }

    String getFileViewUrlById(Integer fileId){

        List<DocumentsFiles> files = Select.from(DocumentsFiles.class).where(Condition.prop("doc_file_id").eq(fileId)).list();

        return  files.get(0).viewUrl;
    }

    String getFileWebUrlById(Integer fileId){

        List<DocumentsFiles> files = Select.from(DocumentsFiles.class).where(Condition.prop("doc_file_id").eq(fileId)).list();

        return files.get(0).webUrl;
    }

    // метод нужен для списка писем и обработки долгово нажатия, при котором выходит AlertDialog  с вопросом вы действительно хотите удалить
    // письмо, причём если вы не в корзине тогда он дожен спрашивать вы действительно хотите переместить в корзину
    void moveMessageToTrashOrDelete(Integer messageId) {


        String url = "https://" + MainActivity.currentUser.server + "/api/2.0/mail/messages/";

        String folderName = getListIndex() != 4 ? "move" : "remove";

        // это нужно для того чтоб применять параметры
        Map<String, Integer> paramis = new HashMap<String, Integer>();


        // в дальней
        // список id писем которые хотим переместить
        paramis.put("ids", messageId);

        // id папки в которую хотим переместить, тест на корзину, потом нужно придумать как предоставлять выбор папки

        if (getListIndex() != 4) {
            paramis.put("folder", 4);
        }

        //запрос POST при котором возвращается JSONOBJECT с которым уже можно работать и извлекать из него данные
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.PUT/* тут мы указываем тип запроса*/, url + folderName, new JSONObject(paramis)/* здесь мы указываем параметры*/, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {


                        try {
                            Integer statusCode = response.getInt("statusCode");
                            if (statusCode == 200) {
                                // обновляем список, либо сообщаем пользователю что нужно обновить список т.к. данные не актуальны
                                Toast.makeText(OOItemListActivity.this, "The Sync Button Needs To Be Pressed!", Toast.LENGTH_SHORT).show();

                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


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
        MySingleton.getInstance(OOItemListActivity.this).addToRequestQueue(jsonObjectRequest);
    }

    void initializeArraylistsForListView(Integer indexofList) {

        lastSyncData.setText(lastSyncTimeDate(getListIndex()));


        if (indexofList == MainActivity.hardcoredListId.getRoot_screen()) {

            initializeArraysForRootScreen();


        } else if (indexofList == MainActivity.hardcoredListId.getRoot_mail()) {

            initializeRootMailScreen();

        } else if (indexofList == MainActivity.hardcoredListId.getRoot_contacts()) {

            // здесь построение списка контактов

        } else if (indexofList == MainActivity.hardcoredListId.getRoot_wiki()) {

            // здесь построение списка википедии

        } else if (indexofList == MainActivity.hardcoredListId.getRoot_documents()) {

            initializeRootDocsScreen();

            createFolderImage.setVisibility(View.INVISIBLE);

        } else if (indexofList >= MainActivity.hardcoredListId.getMail_inbox() && indexofList <= MainActivity.hardcoredListId.getMail_spam()) {

            initializeMailFoldersScreen();

        } else if (indexofList >= MainActivity.hardcoredListId.getDocuments_mydoc() && indexofList <= MainActivity.hardcoredListId.getDocuments_trash()) {

            initializeDocsFoldersScreen();

            if (getListIndex() == MainActivity.hardcoredListId.getDocuments_mydoc()) {
                createFolderImage.setVisibility(View.VISIBLE);} else {createFolderImage.setVisibility(View.INVISIBLE);}

        } else {

            initializeDocsFoldersScreen();

            if (MainActivity.listIdStack.contains(MainActivity.hardcoredListId.getDocuments_mydoc())){
            createFolderImage.setVisibility(View.VISIBLE);} else {createFolderImage.setVisibility(View.INVISIBLE);}

        }

        cla = new CustomListAdapter(this, itemString, itemPng);

        cla.setNotifyOnChange(true);// это не срабатывает
        cla.notifyDataSetChanged(); // тоже ни хрена не срабатывает, нужно адаптер поглубже покопать

        listView.setAdapter(cla);
        syncImage.setImageResource(R.drawable.sync2);


    }

    Integer getListIndex() {
        Integer indexOfList = MainActivity.listIdStack.get(MainActivity.listIdStack.size() - 1);
        //Log.i("111 getIndex",String.valueOf(indexOfList));
        return indexOfList;
    }

    void removeCurrentListIndexFromStack() {
        //Log.i("111 remove from stack",String.valueOf(MainActivity.listIdStack.get(MainActivity.listIdStack.size()-1)));
        MainActivity.listIdStack.remove(MainActivity.listIdStack.size() - 1);
    }

    String lastSyncTimeDate(Integer listId){

        try {
            Log.i("111 get time "," " + listId + "/" + MainActivity.listIdDateStack.get(listId));
        } catch (NullPointerException e) {
            Log.i(" NUllPointException", "YET");
        }
        return MainActivity.listIdDateStack.get(listId);
    }

    void add1ListIdAndPutSyncTime(Integer listId){

        MainActivity.listIdStack.add(listId);
        MainActivity.listIdDateStack.put(listId," "
                + Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + ":"
                + Calendar.getInstance().get(Calendar.MINUTE) + " "
                + Calendar.getInstance().get(Calendar.DAY_OF_MONTH) + "."
                + (Calendar.getInstance().get(Calendar.MONTH ) + 1) + "."
                + Calendar.getInstance().get(Calendar.YEAR));
    }

    void syncScreenWithOO(){

        syncImage.setImageResource(R.drawable.sync2_red);
        //Log.i("111 List Index sync" , String.valueOf(getListIndex()));

        MainActivity.listIdDateStack.put(getListIndex()," "
                + Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + ":"
                + Calendar.getInstance().get(Calendar.MINUTE) + " "
                + Calendar.getInstance().get(Calendar.DAY_OF_MONTH) + "."
                + (Calendar.getInstance().get(Calendar.MONTH ) + 1) + "."
                + Calendar.getInstance().get(Calendar.YEAR));

        if (getListIndex() == MainActivity.hardcoredListId.getRoot_screen()) {
            // если первый экран синхронизация ничего не даст
        } else if (getListIndex() >= MainActivity.hardcoredListId.getMail_inbox() && getListIndex() <= MainActivity.hardcoredListId.getMail_spam()) {
            // папки почты
            getMessagesFromOOByListId(getListIndex());
        } else if (getListIndex() == MainActivity.hardcoredListId.getRoot_documents()) {
            // папки документов
        } else if (getListIndex() >= MainActivity.hardcoredListId.getDocuments_mydoc() && getListIndex() <= MainActivity.hardcoredListId.getDocuments_trash()) {
            // внутрение папки документов
            GetFolderFoldersAndFilesByListId(getListIndex());
        } else if (getListIndex() == MainActivity.hardcoredListId.getRoot_mail()) {
            // почта
            inititalizeMailFolders();
        } else if (getListIndex() == MainActivity.hardcoredListId.getRoot_wiki()) {
            // википедия
        } else {
            // синхро внутри папок документов
            GetFolderFoldersAndFilesByListId(getListIndex());
        }
    }

    void initializeArraysForRootScreen(){
        syncImage.setVisibility(View.INVISIBLE);
        sendMessageImage.setVisibility(View.INVISIBLE);
        itemString = new ArrayList<>(
                Arrays.asList(
                        getString(
                                R.string.documents_string
                        ),
                        getString(
                                R.string.email_string
                        ),
                        getString(
                                R.string.wiki_string
                        ),
                        getString(
                                R.string.contacts_string
                        )
                )
        );
        itemPng = new ArrayList<>(
                Arrays.asList(
                        R.drawable.documents,
                        R.drawable.mail,
                        R.drawable.wiki,
                        R.drawable.contacts
                )
        );
    }
    void initializeRootMailScreen(){
        itemString = new ArrayList<>();
        itemPng = new ArrayList<>();

        syncImage.setVisibility(View.VISIBLE);
        sendMessageImage.setVisibility(View.VISIBLE);

        // тут нужно всё таки использовать класс MailFolders для того чтоб привязать id папки на сср
        // к id списка в приложении
        // нужно переделать get запрос и просто применить его здесь
        //intitalizeMailFolders();

        // делаем запрос в локал бд чтоб извлеч все папки и настройки

        List<MailFolders> foldersToList = Select.from(MailFolders.class).orderBy("folder_id").list();
        if (foldersToList.size() < 1 || foldersToList == null ){

            syncScreenWithOO();
            //inititalizeMailFolders();
        } else {
            for (MailFolders folder :
                    foldersToList) {

                // чтоб писать количество писем прочитанных и непрочитанных нужно добавить в список ещё одно поле, потому как не получается искать по Входящие
                // это в дальнейшем переделаем а пока
                itemString.add(folder.folderName + " " + folder.unreadCount + "/" + folder.totalCount);

                //itemString.add(folder.folderName);
                itemPng.add(folder.pngId);

                //String s = folder.folderName + " " + folder.unreadCount + "/" + folder.totalCount;
                //int pngInt = folder.pngId;
            }
        }
    }
    void initializeRootDocsScreen(){
        syncImage.setVisibility(View.VISIBLE);

        itemString = new ArrayList<>();
        itemPng = new ArrayList<>();

        // здесь построение списка документов

        List<FolderDocOO> docFolderList = Select.from(FolderDocOO.class).orderBy("order_by").list();

        if (docFolderList.size() < 1 || docFolderList == null ){

            syncScreenWithOO();
            initializeDocumentFolders();

        } else {

            for (FolderDocOO docFolder :
                    docFolderList) {
                itemString.add(docFolder.name);
                itemPng.add(docFolder.imgRs);
            }
        }
    }
    void initializeMailFoldersScreen(){
        // здесь построение списка входящих писем и всех других папок почты
        itemString = new ArrayList<>();
        itemPng = new ArrayList<>();
        itemId = new ArrayList<>();

        List<MailMessage> messages = Select.from(MailMessage.class).where(Condition.prop("folder_id").eq(getListIndex())).list();
        if (messages.size() < 1 || messages == null ){

            syncScreenWithOO();

        } else {
            for (MailMessage message :
                    messages) {
                itemString.add(message.messageReceivedDate + "\n" + message.messageSubject);
                itemId.add(message.messageId);
                if (message.isNew) {
                    itemPng.add(R.drawable.message_letter_unread);
                } else {
                    itemPng.add(R.drawable.message_letter);
                }
            }
        }
    }
    void initializeDocsFoldersScreen(){
        // здесь построение списка папок и файлов внутри папок документов
        itemString = new ArrayList<>();
        itemPng = new ArrayList<>();
        itemId = new ArrayList<>();
        itemTypeFolder = new ArrayList<>();
        Boolean existFoldersInLocBD = checkDocsClassBDNotEmpty(DocumentFolders.class, "doc_folder_list_id");
        Boolean existFilesInLocBD = checkDocsClassBDNotEmpty(DocumentsFiles.class,"doc_file_list_id");

        if (existFoldersInLocBD) {
            List<DocumentFolders> folders = Select.from(DocumentFolders.class).where(Condition.prop("doc_folder_list_id").eq(getListIndex())).orderBy("doc_folder_title").list();

            for (DocumentFolders folder :
                    folders) {
                itemString.add(folder.docFolderTitle);
                itemPng.add(folder.docFolderPngId);
                itemId.add(folder.docFolderId);
                itemTypeFolder.add(true);
            }
        } else if (!noFoldersInDocFoldersOnOO) {
            syncScreenWithOO();
        }

        if (existFilesInLocBD) {
            List<DocumentsFiles> files = Select.from(DocumentsFiles.class).where(Condition.prop("doc_file_list_id").eq(getListIndex())).orderBy("doc_file_title").list();

            for (DocumentsFiles file :
                    files) {
                itemString.add(file.docFileTitle);
                if (MainActivity.hardcoredListId.fileExstImgId.containsKey(file.docFileExtension)) {
                    itemPng.add(MainActivity.hardcoredListId.fileExstImgId.get(file.docFileExtension));
                } else {
                    itemPng.add(R.drawable.unknown);
                }
                itemId.add(file.docFileId);
                itemTypeFolder.add(false);
            }
        } else if (!noFilesInDocFoldersOnOO) {
            syncScreenWithOO();
        }


    }

    public void showPopUpCreate(View view) {

        if (myDocumentsFolderIdPresents()) {

            PopupMenu popup = new PopupMenu(this, view);

            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    String selectedItem = menuItem.getTitle().toString();
                    textViewForType.setText(selectedItem);
                    switch (selectedItem) {
                        case "Folder":
                            // createFolder
                            //Log.i(" Folder Create", selectedItem);
                            createFileFolderDocs.setVisibility(View.VISIBLE);
                            spinnerForFileType.setVisibility(View.INVISIBLE);
                            createTextForTXT.setVisibility(View.INVISIBLE);

                            return true;
                        case "Document":
                            // createDocument
                            //Log.i(" Document Create", selectedItem);
                            createFileFolderDocs.setVisibility(View.VISIBLE);
                            spinnerForFileType.setVisibility(View.VISIBLE);

                            return true;
                        default:
                            return true;

                    }
                }
            });
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.doc_add_menu, popup.getMenu());
            popup.show();
        }
    }

    void clearCreationEditTexts(){
        createTitle.setText("");
        createTextForTXT.setText("");
    }
    
    Boolean myDocumentsFolderIdPresents(){
        if (myDocumentsFolderId == null) {
            Toast.makeText(this, "Необходимо сделать синхронизацию в папке Мои Докукменты", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            return true;
        }
    }


}
