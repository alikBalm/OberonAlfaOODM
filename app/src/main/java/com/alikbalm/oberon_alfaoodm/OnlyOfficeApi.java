package com.alikbalm.oberon_alfaoodm;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
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
    Call<ResponseBody> rootMailFolders(
            @Header("Authorization") String token
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
}
