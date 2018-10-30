package com.alikbalm.oberon_alfaoodm;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface OnlyOfficeApi {


    @POST("api/2.0/authentication")
    Call<ResponseBody> getToken(
            @Query("userName") String usename,
            @Query("password") String password
    );

    @GET("api/2.0/mail/folders")
    Call<ResponseBody> getRootMailFolders(
            @Header("Authorization") String token
    );

    @GET("api/2.0/mail/messages")
    Call<ResponseBody> getMailMessages(
            @Header("Authorization") String token,
            @Query("folder") Integer folderId,
            @Query("page_size") Integer page_size
    );

    @PUT("api/2.0/mail/messages/{action}")
    Call<ResponseBody> moveOrRemoveMessage(
            @Header("Authorization") String token,
            @Path("action") String action,
            @Query("ids") Integer messageId,
            @Query("folder") Integer folderId
    );

    @GET("api/2.0/files/{folderId}")
    Call<ResponseBody> getDocFoldersAndFiles(
            @Header("Authorization") String token,
            @Path("folderId") String folderId
    );

    @POST("api/2.0/files/folder/{folderId}")
    Call<ResponseBody> createFolder(
            @Header("Authorization") String token,
            @Path("folderId") Integer folderId,
            @Query("title") String folderTitle
    );

    @POST("api/2.0/files/{folderId}/{type}")
    Call<ResponseBody> createFile(
            @Header("Authorization") String token,
            @Path("folderId") Integer folderId,
            @Path("type") String fileType,
            @Query("title") String fileTitle,
            @Query("content") String content
    );

    @Multipart
    @POST("api/2.0/files/{folderId}/upload")
    Call<ResponseBody> uploadFile(
            @Header("Authorization") String token,
            @Path("folderId") Integer folderId,
            @Part("description") RequestBody description,
            @Part MultipartBody.Part file);

    @GET("api/2.0/mail/accounts")
    Call<ResponseBody> getEmailAddress(
            @Header("Authorization") String token
    );

    @GET("api/2.0/mail/messages/{id}")
    Call<ResponseBody> readMessageById(
            @Header("Authorization") String token,
            @Path("id") Integer messageId,
            @Query("markRead") Boolean markRead
    );

    @PUT("api/2.0/mail/messages/send")
    Call<ResponseBody> sendMessage(
            @Header("Authorization") String token,
            @Query("from") String from,
            @Query("to") String to,
            @Query("subject") String subject,
            @Query("body") String body
    );

    @GET("api/2.0/community/wiki")
    Call<ResponseBody> getWikiPages(
            @Header("Authorization") String token
    );

    @GET("api/2.0/community/wiki/{name}")
    Call<ResponseBody> getWikiPageContent(
            @Header("Authorization") String token,
            @Path("name") String wikiPageName
    );

    @GET("api/2.0/people")
    Call<ResponseBody> getContactsOO(
            @Header("Authorization") String token
    );

    @GET("{avatar}")
    Call<ResponseBody> getContactsAvatar(
            @Header("Authorization") String token,
            @Path("avatar") String avatar
    );
}
