package com.alikbalm.oberon_alfaoodm;

import android.util.Log;

import com.orm.SugarRecord;
import com.orm.query.Condition;
import com.orm.query.Select;

import java.util.List;

public class MailMessage extends SugarRecord {

    public MailMessage(){}

    Integer messageId, folderId;
    String messageSubject, messageReceivedDate;
    Boolean isNew;

    public MailMessage(Integer messageId, Integer folderId, String messageSubject, String messageReceivedDate, Boolean isNew) {

        this.messageId = messageId;
        this.folderId = folderId;
        this.messageSubject = messageSubject;
        this.messageReceivedDate = messageReceivedDate;
        this.isNew = isNew;
    }

    public static void clearTableByScreenId(Integer id){
        List<MailMessage> delMessages = Select.from(MailMessage.class).where(Condition.prop("folder_id").eq(id)).list();
        for (MailMessage messageToDelete :
                delMessages) {
            Log.i("Dell from LDB ", messageToDelete.messageSubject);
            messageToDelete.delete();

        }

    }
}
