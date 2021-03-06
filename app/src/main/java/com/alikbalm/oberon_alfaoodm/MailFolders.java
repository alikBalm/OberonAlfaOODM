package com.alikbalm.oberon_alfaoodm;

import com.orm.SugarRecord;

public class MailFolders extends SugarRecord {

    public MailFolders () {}

    Integer folderId, unreadCount, totalCount, pngId;

    String folderName;

    public MailFolders(Integer folderId, Integer unreadCount, Integer totalCount, String folderName) {


        this.folderId = folderId;
        this.unreadCount = unreadCount;
        this.totalCount = totalCount;
        this.folderName = folderName;
        switch (folderId) {
            case 1 :
                this.pngId = R.drawable.in;
                break;
            case 2:
                this.pngId = R.drawable.out;
                break;
            case 3:
                this.pngId = R.drawable.edit;
                break;
            case 4:
                this.pngId = R.drawable.trash;
                break;
            case 5:
                this.pngId = R.drawable.spam;
                break;
            default:
                this.pngId = R.drawable.documents;
                break;
        }
    }

}
