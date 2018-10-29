package com.alikbalm.oberon_alfaoodm;

import android.util.Log;

import com.orm.SugarRecord;
import com.orm.query.Condition;
import com.orm.query.Select;

import java.util.List;

public class WikiPages extends SugarRecord {
    public WikiPages(){}

    String wikiPageName, wikiPageContent;

    public WikiPages(String wikiPageName, String wikiPageContent) {
        this.wikiPageName = wikiPageName;
        this.wikiPageContent = wikiPageContent;
    }


    // это пока не нужно но на всякий случай создал
    public static void clearWikiPages(Integer id){
        List<WikiPages> delPages = Select.from(WikiPages.class).list();

        for (WikiPages pageToDelete :
                delPages) {
            Log.i("Dell from LDB ", pageToDelete.wikiPageName);
            pageToDelete.delete();

        }

    }
}
