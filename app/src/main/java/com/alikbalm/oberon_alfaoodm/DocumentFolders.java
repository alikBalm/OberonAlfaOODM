package com.alikbalm.oberon_alfaoodm;

import android.util.Log;

import com.orm.SugarRecord;
import com.orm.query.Condition;
import com.orm.query.Select;

import java.util.List;

public class DocumentFolders extends SugarRecord {

    public DocumentFolders(){}


    String docFolderTitle;
    Integer docFolderId, docFolderPngId, docFolderListId;

    public DocumentFolders(Integer docFolderId, String docFolderTitle){
        this.docFolderId = docFolderId;
        this.docFolderTitle = docFolderTitle;
        this.docFolderPngId = R.drawable.folder;
    }

    public static void clearTableByScreenId(Integer id){
        List<DocumentFolders> delFolders = Select.from(DocumentFolders.class).where(Condition.prop("doc_folder_list_id").eq(id)).list();
        for (DocumentFolders folderToDelete :
                delFolders) {
            Log.i("Dell from LDB ", folderToDelete.docFolderTitle);
            folderToDelete.delete();

        }

    }
}
