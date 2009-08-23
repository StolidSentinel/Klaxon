// This class was generated from com.koushikdutta.klaxon.IAlarmSettings by a tool
// Do not edit this file directly! PLX THX
package com.koushikdutta.klaxon;

public abstract class AlarmSettingsBase extends com.antlersoft.android.dbimpl.IdImplementationBase implements IAlarmSettings {

    public static final String GEN_TABLE_NAME = "ALARMSETTINGS";
    public static final int GEN_COUNT = 12;

    // Field constants
    public static final String GEN_FIELD__id = "_id";
    public static final int GEN_ID__id = 0;
    public static final String GEN_FIELD_ENABLED = "ENABLED";
    public static final int GEN_ID_ENABLED = 1;
    public static final String GEN_FIELD_VIBRATEENABLED = "VIBRATEENABLED";
    public static final int GEN_ID_VIBRATEENABLED = 2;
    public static final String GEN_FIELD_ALARMDAYSBASE = "ALARMDAYSBASE";
    public static final int GEN_ID_ALARMDAYSBASE = 3;
    public static final String GEN_FIELD_HOUR = "HOUR";
    public static final int GEN_ID_HOUR = 4;
    public static final String GEN_FIELD_MINUTES = "MINUTES";
    public static final int GEN_ID_MINUTES = 5;
    public static final String GEN_FIELD_RINGTONEBASE = "RINGTONEBASE";
    public static final int GEN_ID_RINGTONEBASE = 6;
    public static final String GEN_FIELD_SNOOZETIME = "SNOOZETIME";
    public static final int GEN_ID_SNOOZETIME = 7;
    public static final String GEN_FIELD_NEXTSNOOZE = "NEXTSNOOZE";
    public static final int GEN_ID_NEXTSNOOZE = 8;
    public static final String GEN_FIELD_VOLUME = "VOLUME";
    public static final int GEN_ID_VOLUME = 9;
    public static final String GEN_FIELD_VOLUMERAMP = "VOLUMERAMP";
    public static final int GEN_ID_VOLUMERAMP = 10;
    public static final String GEN_FIELD_NAME = "NAME";
    public static final int GEN_ID_NAME = 11;

    // SQL Command for creating the table
    public static String GEN_CREATE = "CREATE TABLE ALARMSETTINGS (" +
    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
    "ENABLED INTEGER," +
    "VIBRATEENABLED INTEGER," +
    "ALARMDAYSBASE INTEGER," +
    "HOUR INTEGER," +
    "MINUTES INTEGER," +
    "RINGTONEBASE TEXT," +
    "SNOOZETIME INTEGER," +
    "NEXTSNOOZE INTEGER," +
    "VOLUME INTEGER," +
    "VOLUMERAMP INTEGER," +
    "NAME TEXT" +
    ")";

    // Members corresponding to defined fields
    private long gen__id;
    private boolean gen_enabled;
    private boolean gen_vibrateEnabled;
    private int gen_alarmDaysBase;
    private int gen_hour;
    private int gen_minutes;
    private java.lang.String gen_ringtoneBase;
    private int gen_snoozeTime;
    private int gen_nextSnooze;
    private int gen_volume;
    private int gen_volumeRamp;
    private java.lang.String gen_name;


    public String Gen_tableName() { return GEN_TABLE_NAME; }

    // Field accessors
    public long get_Id() { return gen__id; }
    public void set_Id(long arg__id) { gen__id = arg__id; }
    public boolean getEnabled() { return gen_enabled; }
    public void setEnabled(boolean arg_enabled) { gen_enabled = arg_enabled; }
    public boolean getVibrateEnabled() { return gen_vibrateEnabled; }
    public void setVibrateEnabled(boolean arg_vibrateEnabled) { gen_vibrateEnabled = arg_vibrateEnabled; }
    public int getAlarmDaysBase() { return gen_alarmDaysBase; }
    public void setAlarmDaysBase(int arg_alarmDaysBase) { gen_alarmDaysBase = arg_alarmDaysBase; }
    public int getHour() { return gen_hour; }
    public void setHour(int arg_hour) { gen_hour = arg_hour; }
    public int getMinutes() { return gen_minutes; }
    public void setMinutes(int arg_minutes) { gen_minutes = arg_minutes; }
    public java.lang.String getRingtoneBase() { return gen_ringtoneBase; }
    public void setRingtoneBase(java.lang.String arg_ringtoneBase) { gen_ringtoneBase = arg_ringtoneBase; }
    public int getSnoozeTime() { return gen_snoozeTime; }
    public void setSnoozeTime(int arg_snoozeTime) { gen_snoozeTime = arg_snoozeTime; }
    public int getNextSnooze() { return gen_nextSnooze; }
    public void setNextSnooze(int arg_nextSnooze) { gen_nextSnooze = arg_nextSnooze; }
    public int getVolume() { return gen_volume; }
    public void setVolume(int arg_volume) { gen_volume = arg_volume; }
    public int getVolumeRamp() { return gen_volumeRamp; }
    public void setVolumeRamp(int arg_volumeRamp) { gen_volumeRamp = arg_volumeRamp; }
    public java.lang.String getName() { return gen_name; }
    public void setName(java.lang.String arg_name) { gen_name = arg_name; }

    public android.content.ContentValues Gen_getValues() {
        android.content.ContentValues values=new android.content.ContentValues();
        values.put(GEN_FIELD__id,Long.toString(this.gen__id));
        values.put(GEN_FIELD_ENABLED,(this.gen_enabled ? "1" : "0"));
        values.put(GEN_FIELD_VIBRATEENABLED,(this.gen_vibrateEnabled ? "1" : "0"));
        values.put(GEN_FIELD_ALARMDAYSBASE,Integer.toString(this.gen_alarmDaysBase));
        values.put(GEN_FIELD_HOUR,Integer.toString(this.gen_hour));
        values.put(GEN_FIELD_MINUTES,Integer.toString(this.gen_minutes));
        values.put(GEN_FIELD_RINGTONEBASE,this.gen_ringtoneBase);
        values.put(GEN_FIELD_SNOOZETIME,Integer.toString(this.gen_snoozeTime));
        values.put(GEN_FIELD_NEXTSNOOZE,Integer.toString(this.gen_nextSnooze));
        values.put(GEN_FIELD_VOLUME,Integer.toString(this.gen_volume));
        values.put(GEN_FIELD_VOLUMERAMP,Integer.toString(this.gen_volumeRamp));
        values.put(GEN_FIELD_NAME,this.gen_name);
        return values;
    }

    /**
     * Return an array that gives the column index in the cursor for each field defined
     * @param cursor Database cursor over some columns, possibly including this table
     * @return array of column indices; -1 if the column with that id is not in cursor
     */
    public int[] Gen_columnIndices(android.database.Cursor cursor) {
        int[] result=new int[GEN_COUNT];
        result[0] = cursor.getColumnIndex(GEN_FIELD__id);
        result[1] = cursor.getColumnIndex(GEN_FIELD_ENABLED);
        result[2] = cursor.getColumnIndex(GEN_FIELD_VIBRATEENABLED);
        result[3] = cursor.getColumnIndex(GEN_FIELD_ALARMDAYSBASE);
        result[4] = cursor.getColumnIndex(GEN_FIELD_HOUR);
        result[5] = cursor.getColumnIndex(GEN_FIELD_MINUTES);
        result[6] = cursor.getColumnIndex(GEN_FIELD_RINGTONEBASE);
        result[7] = cursor.getColumnIndex(GEN_FIELD_SNOOZETIME);
        result[8] = cursor.getColumnIndex(GEN_FIELD_NEXTSNOOZE);
        result[9] = cursor.getColumnIndex(GEN_FIELD_VOLUME);
        result[10] = cursor.getColumnIndex(GEN_FIELD_VOLUMERAMP);
        result[11] = cursor.getColumnIndex(GEN_FIELD_NAME);
        return result;
    }

    /**
     * Populate one instance from a cursor 
     */
    public void Gen_populate(android.database.Cursor cursor,int[] columnIndices) {
        if ( columnIndices[GEN_ID__id] >= 0 && ! cursor.isNull(columnIndices[GEN_ID__id])) {
            gen__id = cursor.getLong(columnIndices[GEN_ID__id]);
        }
        if ( columnIndices[GEN_ID_ENABLED] >= 0 && ! cursor.isNull(columnIndices[GEN_ID_ENABLED])) {
            gen_enabled = (cursor.getInt(columnIndices[GEN_ID_ENABLED]) != 0);
        }
        if ( columnIndices[GEN_ID_VIBRATEENABLED] >= 0 && ! cursor.isNull(columnIndices[GEN_ID_VIBRATEENABLED])) {
            gen_vibrateEnabled = (cursor.getInt(columnIndices[GEN_ID_VIBRATEENABLED]) != 0);
        }
        if ( columnIndices[GEN_ID_ALARMDAYSBASE] >= 0 && ! cursor.isNull(columnIndices[GEN_ID_ALARMDAYSBASE])) {
            gen_alarmDaysBase = (int)cursor.getInt(columnIndices[GEN_ID_ALARMDAYSBASE]);
        }
        if ( columnIndices[GEN_ID_HOUR] >= 0 && ! cursor.isNull(columnIndices[GEN_ID_HOUR])) {
            gen_hour = (int)cursor.getInt(columnIndices[GEN_ID_HOUR]);
        }
        if ( columnIndices[GEN_ID_MINUTES] >= 0 && ! cursor.isNull(columnIndices[GEN_ID_MINUTES])) {
            gen_minutes = (int)cursor.getInt(columnIndices[GEN_ID_MINUTES]);
        }
        if ( columnIndices[GEN_ID_RINGTONEBASE] >= 0 && ! cursor.isNull(columnIndices[GEN_ID_RINGTONEBASE])) {
            gen_ringtoneBase = cursor.getString(columnIndices[GEN_ID_RINGTONEBASE]);
        }
        if ( columnIndices[GEN_ID_SNOOZETIME] >= 0 && ! cursor.isNull(columnIndices[GEN_ID_SNOOZETIME])) {
            gen_snoozeTime = (int)cursor.getInt(columnIndices[GEN_ID_SNOOZETIME]);
        }
        if ( columnIndices[GEN_ID_NEXTSNOOZE] >= 0 && ! cursor.isNull(columnIndices[GEN_ID_NEXTSNOOZE])) {
            gen_nextSnooze = (int)cursor.getInt(columnIndices[GEN_ID_NEXTSNOOZE]);
        }
        if ( columnIndices[GEN_ID_VOLUME] >= 0 && ! cursor.isNull(columnIndices[GEN_ID_VOLUME])) {
            gen_volume = (int)cursor.getInt(columnIndices[GEN_ID_VOLUME]);
        }
        if ( columnIndices[GEN_ID_VOLUMERAMP] >= 0 && ! cursor.isNull(columnIndices[GEN_ID_VOLUMERAMP])) {
            gen_volumeRamp = (int)cursor.getInt(columnIndices[GEN_ID_VOLUMERAMP]);
        }
        if ( columnIndices[GEN_ID_NAME] >= 0 && ! cursor.isNull(columnIndices[GEN_ID_NAME])) {
            gen_name = cursor.getString(columnIndices[GEN_ID_NAME]);
        }
    }

    /**
     * Populate one instance from a ContentValues 
     */
    public void Gen_populate(android.content.ContentValues values) {
        gen__id = values.getAsLong(GEN_FIELD__id);
        gen_enabled = (values.getAsInteger(GEN_FIELD_ENABLED) != 0);
        gen_vibrateEnabled = (values.getAsInteger(GEN_FIELD_VIBRATEENABLED) != 0);
        gen_alarmDaysBase = (int)values.getAsInteger(GEN_FIELD_ALARMDAYSBASE);
        gen_hour = (int)values.getAsInteger(GEN_FIELD_HOUR);
        gen_minutes = (int)values.getAsInteger(GEN_FIELD_MINUTES);
        gen_ringtoneBase = values.getAsString(GEN_FIELD_RINGTONEBASE);
        gen_snoozeTime = (int)values.getAsInteger(GEN_FIELD_SNOOZETIME);
        gen_nextSnooze = (int)values.getAsInteger(GEN_FIELD_NEXTSNOOZE);
        gen_volume = (int)values.getAsInteger(GEN_FIELD_VOLUME);
        gen_volumeRamp = (int)values.getAsInteger(GEN_FIELD_VOLUMERAMP);
        gen_name = values.getAsString(GEN_FIELD_NAME);
    }
}
