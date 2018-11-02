package com.alikbalm.oberon_alfaoodm;

import com.orm.SugarRecord;

public class LogedUsers extends SugarRecord{

    public LogedUsers(){}

    String server, userName, password, email, mailSignature;

    public LogedUsers(String server, String userName, String password, String email, String mailSignature){

        this.server = server;
        this.userName = userName;
        this.password = password;
        this.email = email;
        this.mailSignature = mailSignature;
    }


}
