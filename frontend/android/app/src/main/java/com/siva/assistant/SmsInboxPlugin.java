package com.siva.assistant;

import android.Manifest;
import android.database.Cursor;
import android.provider.Telephony;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

/**
 * Reads received text messages so the web layer can look for bank transaction alerts.
 *
 * This plugin only returns raw rows. Deciding which messages are transactions, and everything
 * else, happens in the web app on the device. Nothing is sent anywhere from here.
 */
@CapacitorPlugin(
    name = "SmsInbox",
    permissions = {
        @Permission(alias = "sms", strings = { Manifest.permission.READ_SMS })
    }
)
public class SmsInboxPlugin extends Plugin {

    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 500;

    /** Asks for the permission if needed, then returns messages received at or after sinceMs. */
    @PluginMethod
    public void readInbox(PluginCall call) {
        if (getPermissionState("sms") != PermissionState.GRANTED) {
            requestPermissionForAlias("sms", call, "afterPermissionRequest");
            return;
        }
        read(call);
    }

    @PermissionCallback
    private void afterPermissionRequest(PluginCall call) {
        if (getPermissionState("sms") == PermissionState.GRANTED) {
            read(call);
        } else {
            call.reject("Permission to read SMS was denied", "PERMISSION_DENIED");
        }
    }

    private void read(PluginCall call) {
        long sinceMs = call.getLong("sinceMs", 0L);
        int limit = Math.max(1, Math.min(call.getInt("limit", DEFAULT_LIMIT), MAX_LIMIT));

        String[] projection = {
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        };
        String selection = Telephony.Sms.DATE + " >= ?";
        String[] selectionArgs = { String.valueOf(sinceMs) };

        JSArray messages = new JSArray();
        try (Cursor cursor = getContext().getContentResolver().query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                Telephony.Sms.DATE + " DESC")) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(Telephony.Sms._ID);
                int addressCol = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS);
                int bodyCol = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY);
                int dateCol = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE);
                // The limit is applied here, not in the query, because "LIMIT" in the sort order
                // is rejected by the SMS provider on newer Android versions.
                while (cursor.moveToNext() && messages.length() < limit) {
                    JSObject message = new JSObject();
                    message.put("id", cursor.getString(idCol));
                    message.put("address", cursor.getString(addressCol));
                    message.put("body", cursor.getString(bodyCol));
                    message.put("date", cursor.getLong(dateCol));
                    messages.put(message);
                }
            }
        } catch (RuntimeException e) {
            call.reject("Could not read messages: " + e.getMessage(), e);
            return;
        }

        JSObject result = new JSObject();
        result.put("messages", messages);
        call.resolve(result);
    }
}
