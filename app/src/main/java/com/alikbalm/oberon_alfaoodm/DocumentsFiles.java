package com.alikbalm.oberon_alfaoodm;

import android.util.Log;

import com.orm.SugarRecord;
import com.orm.query.Condition;
import com.orm.query.Select;

import java.util.List;

public class DocumentsFiles extends SugarRecord {

    public DocumentsFiles(){}


    String docFileTitle, docFileExtension, webUrl, viewUrl;
    Integer docFileId, docFileListId;

    public DocumentsFiles(Integer docFileId, String docFileTitle, String docFileExtension, String viewUrl, String webUrl){

        this.docFileId = docFileId;
        this.docFileTitle = docFileTitle;
        this.docFileExtension = docFileExtension;
        this.viewUrl = viewUrl;
        this.webUrl = webUrl;

    }

    public static void clearTableByScreenId(Integer id){
        List<DocumentsFiles> delFiles = Select.from(DocumentsFiles.class).where(Condition.prop("doc_file_list_id").eq(id)).list();

        for (DocumentsFiles fileToDelete :
                delFiles) {
            Log.i("Dell from LDB ", fileToDelete.docFileTitle);
            fileToDelete.delete();

        }

    }
}