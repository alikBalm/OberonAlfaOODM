package com.alikbalm.oberon_alfaoodm;

import com.orm.SugarRecord;

public class MailFolders extends SugarRecord {

    public MailFolders () {}

    Integer folderId, unreadCount, totalCount, pngId;

    String folderName;

    public MailFolders(Integer folderId, Integer unreadCount, Integer totalCount) {


        this.folderId = folderId;
        this.unreadCount = unreadCount;
        this.totalCount = totalCount;
        switch (folderId) {
            case 1 : //ID #0x7f06005d
                this.folderName = "Входящие";
                this.pngId = R.drawable.in;
                break;
            case 2:
                this.folderName = "Отправленные";
                this.pngId = R.drawable.out;
                break;
            case 3:
                this.folderName = "Черновики";
                this.pngId = R.drawable.edit;
                break;
            case 4:
                this.folderName = "Корзина";
                this.pngId = R.drawable.trash;
                break;
            case 5:
                this.folderName = "Спам";
                this.pngId = R.drawable.spam;
                break;
            default:
                this.folderName = "Непонятки с id папки";
                this.pngId = R.drawable.documents;
                break;
        }
    }

}
