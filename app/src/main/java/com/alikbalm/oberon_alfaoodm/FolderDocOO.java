package com.alikbalm.oberon_alfaoodm;

import com.orm.SugarRecord;

public class FolderDocOO extends SugarRecord {

    public FolderDocOO(){}

    String name;
    Integer orderBy,imgRs;

    public FolderDocOO(String name){
        switch (name) {
            case "my" :
                this.name = "Мои документы";
                this.imgRs = R.drawable.my_folder;
                this.orderBy = 1;
                break;
            case "share" :
                this.name = "Доступно для меня";
                this.imgRs = R.drawable.share_folder;
                this.orderBy = 2;
                break;
            case "common" :
                this.name = "Общие документы";
                this.imgRs = R.drawable.common_folder;
                this.orderBy = 3;
                break;
            case "projects" :
                this.name = "Документы проекта";
                this.imgRs = R.drawable.projects_folder;
                this.orderBy = 4;
                break;
            case "trash" :
                this.name = "Корзина";
                this.imgRs = R.drawable.trash_folder;
                this.orderBy = 5;
                break;
            default:
                this.name = name;
                this.imgRs = R.drawable.folder;
                this.orderBy = 666123;
                break;
        }

    }

}
