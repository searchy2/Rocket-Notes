package stream.notesapp;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.Calendar;

public class SaveNoteService extends Service {
    private final String TAG = "SaveNoteService";

    private Context mContext = this;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getAction().equals(Constants.NEW_NOTE)) {
            Bundle extras = intent.getExtras();
            String body = extras.getString(Constants.BODY);
            String image = extras.getString(Constants.IMAGE);
            Calendar calendar = Calendar.getInstance();
            Long currentTime = calendar.getTimeInMillis();
            DatabaseHelper dbHelper = new DatabaseHelper(mContext);
            NotesItem savedNote = dbHelper.AddNewNote(body, currentTime, image);
            NotificationSender(savedNote);

            int widgetIDs[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), NotesWidget.class));
            for (int id : widgetIDs) {
                AppWidgetManager.getInstance(getApplication()).notifyAppWidgetViewDataChanged(id, R.id.notes_listview);
            }

            int imageWidgetIDs[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), ImageWidget.class));
            for (int id : imageWidgetIDs) {
                AppWidgetManager.getInstance(getApplication()).notifyAppWidgetViewDataChanged(id, R.id.image_gridview);
            }
        }
        stopSelf();
        return super.onStartCommand(intent, flags, startId);
    }

    public void NotificationSender(NotesItem note)
    {
        EventBus.getDefault().postSticky(new UpdateMainEvent(Constants.RECEIVED, note.getNotesID()));
        Log.d("Note ID", Integer.toString(note.getNotesID()));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}