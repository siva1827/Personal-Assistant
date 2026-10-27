package com.siva.assistant.push;

import java.util.Map;

/** What to show on the phone, plus string data the app uses when the message is tapped. */
public record PushMessage(String title, String body, Map<String, String> data) {
}
