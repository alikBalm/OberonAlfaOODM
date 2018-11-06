package com.alikbalm.oberon_alfaoodm;

import com.orm.SugarRecord;

public class FolderDocOO extends SugarRecord {

    public FolderDocOO(){}

    String name;
    Integer orderBy,imgRs;

    public FolderDocOO(String name){
        if (name.equals("My Documents") || name.equals("Мои документы")){
            this.name = name;
            this.imgRs = R.drawable.my_folder;
            this.orderBy = 1;
        } else if (name.equals("Shared with Me") || name.equals("Доступно для меня")){
            this.name = name;
            this.imgRs = R.drawable.share_folder;
            this.orderBy = 2;
        } else if (name.equals("Common Documents") || name.equals("Общие документы")){
            this.name = name;
            this.imgRs = R.drawable.share_folder;
            this.orderBy = 3;
        } else if (name.equals("Project Documents") || name.equals("Документы проектов")){
            this.name = name;
            this.imgRs = R.drawable.projects_folder;
            this.orderBy = 4;
        } else if (name.equals("Recycle Bin") || name.equals("Корзина")){
            this.name = name;
            this.imgRs = R.drawable.trash_folder;
            this.orderBy = 5;
        } else {
            this.name = name;
            this.imgRs = R.drawable.folder;
            this.orderBy = 666123;
        }


    }

}
