package com.example.evernote;
import com.github.scribejava.core.builder.api.DefaultApi10a;
public class EvernoteApi extends DefaultApi10a {
    private static final String BASE = "https://www.evernote.com"; // Production
    @Override public String getRequestTokenEndpoint() { return BASE + "/oauth"; }
    @Override public String getAccessTokenEndpoint() { return BASE + "/oauth"; }
    @Override protected String getAuthorizationBaseUrl() { return BASE + "/OAuth.action"; }
}