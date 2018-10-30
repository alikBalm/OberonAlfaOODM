package com.alikbalm.oberon_alfaoodm;

import java.util.HashMap;

public class Conf {

    public Conf(){}

    public Integer getRoot_screen() {
        return root_screen;
    }

    public Integer getRoot_documents() {
        return root_documents;
    }

    public Integer getDocuments_mydoc() {
        return documents_mydoc;
    }

    public Integer getDocuments_shared() {
        return documents_shared;
    }

    public Integer getDocuments_common() {
        return documents_common;
    }

    public Integer getDocuments_project() {
        return documents_project;
    }

    public Integer getDocuments_trash() {
        return documents_trash;
    }

    public Integer getRoot_mail() {
        return root_mail;
    }

    public Integer getMail_inbox() {
        return mail_inbox;
    }

    public Integer getMail_outbox() {
        return mail_outbox;
    }

    public Integer getMail_edit() {
        return mail_edit;
    }

    public Integer getMail_trash() {
        return mail_trash;
    }

    public Integer getMail_spam() {
        return mail_spam;
    }

    public Integer getRoot_wiki() {
        return root_wiki;
    }

    public Integer getRoot_contacts() {
        return root_contacts;
    }

    private Integer
            root_screen = 0,
            root_documents = 6,
                documents_mydoc = 11,
                documents_shared = 12,
                documents_common = 13,
                documents_project = 14,
                documents_trash = 15,
            root_mail = 7,
                mail_inbox = 1,
                mail_outbox = 2,
                mail_edit = 3,
                mail_trash = 4,
                mail_spam = 5,
            root_wiki = 8,
            root_contacts = 9;

    HashMap<String,Integer> fileExstImgId = new HashMap<String, Integer>()
    {{
        put(".doc",R.drawable.doc);
        put(".docx",R.drawable.doc);
        put(".xls",R.drawable.xls);
        put(".xlsx",R.drawable.xls);
        put(".ppt",R.drawable.ppt);
        put(".pptx",R.drawable.ppt);
        put(".txt",R.drawable.txt);
        put(".pdf",R.drawable.pdf);
        put(".png",R.drawable.png);
        put(".jpg",R.drawable.jpg);
        put(".jpeg",R.drawable.jpg);
        put(".zip",R.drawable.zip);

    }};

    HashMap<String,String> fileTypeForCreation = new HashMap<String, String>()
    {{
        put("txt", ".txt");
        put("Документ", ".docx");
        put("Таблица", ".xlsx");
        put("Презентация", ".pptx");
    }};





}
