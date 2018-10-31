package com.alikbalm.oberon_alfaoodm;

import com.orm.SugarRecord;

public class LogedUsers extends SugarRecord{

    public LogedUsers(){}

    String server, userName, password, token, email, mailSignature;

    public LogedUsers(String server, String userName, String password, String token, String email, String mailSignature){

        this.server = server;
        this.userName = userName;
        this.password = password;
        this.token = token;
        this.email = email;
        this.mailSignature = mailSignature;
    }


}
