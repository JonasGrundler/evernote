package com.example.evernote.auth;

import com.example.evernote.internet.LocalTokenStore;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.oauth.OAuth10aService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class EvernoteOAuthController {
    @Value("${trocr.url:http://127.0.0.1:8000}")
    private String trocrBaseUrl;

    @Value("${evernote.consumer.key}")
    private String consumerKey;
    @Value("${evernote.consumer.secret}")
    private String consumerSecret;
    @Value("${evernote.oauth.callback}")
    private String callbackUrl;
    @Value("${evernote.access.token:}")
    private String configuredAccessToken; // optional (properties)
    private static final String SESSION_REQUEST_TOKEN = "evernote_request_token";
    private static final String SESSION_ACCESS_TOKEN = "evernote_access_token";

    @GetMapping("/evernote/oauth/start")
    public String startOAuth(HttpSession session) throws Exception {
        OAuth10aService service = new ServiceBuilder(consumerKey).apiSecret(consumerSecret).callback(callbackUrl).build(new EvernoteApi());
        OAuth1RequestToken requestToken = service.getRequestToken();
        session.setAttribute(SESSION_REQUEST_TOKEN, requestToken);
        String authUrl = service.getAuthorizationUrl(requestToken);
        return "redirect:" + authUrl;
    }

    @GetMapping({"/evernote/oauth/callback", "/evernote/oauth/callback/", "/evernote/oauth/callback/**"})
    public String handleCallback(@RequestParam("oauth_token") String oauthToken, @RequestParam("oauth_verifier") String oauthVerifier, HttpSession session, HttpServletRequest req) throws Exception {
        OAuth1RequestToken requestToken = (OAuth1RequestToken) session.getAttribute(SESSION_REQUEST_TOKEN);
        if (requestToken == null)
            throw new IllegalStateException("Kein Request-Token in der Session. Bitte erneut unter /evernote/oauth/start beginnen.");
        OAuth10aService service = new ServiceBuilder(consumerKey).apiSecret(consumerSecret).callback(callbackUrl).build(new EvernoteApi());
        OAuth1AccessToken accessToken = service.getAccessToken(requestToken, oauthVerifier);
        LocalTokenStore.getSingleton().save(accessToken.getToken());
        session.setAttribute(SESSION_ACCESS_TOKEN, accessToken.getToken());
        return "redirect:/oauth-success";
    }

    @GetMapping("/oauth-success")
    public String success() {
        return "<html><body><h3>OAuth erfolgreich</h3><p>Token gespeichert.</p>" + "<p><a href='/'>Home</a> | <a href='/notes'>Notizen</a> | <a href='/auth/status'>Status</a></p></body></html>";
    }

    private String getToken(HttpSession session) {
        String token = (String) session.getAttribute(SESSION_ACCESS_TOKEN);
        if (token == null || token.isBlank())
            token = (configuredAccessToken != null && !configuredAccessToken.isBlank()) ? configuredAccessToken : null;
        if (token == null || token.isBlank()) token = System.getenv("EVERNOTE_ACCESS_TOKEN");
        if (token == null || token.isBlank()) token = LocalTokenStore.getSingleton().load();
        if (token == null || token.isBlank())
            throw new IllegalStateException("Kein Evernote-Access-Token vorhanden. Bitte zuerst /evernote/oauth/start aufrufen.");
        return token;
    }

    @GetMapping(value = "/auth/status", produces = "text/plain")
    @ResponseBody
    public String authStatus(HttpSession session) {
        String s = (String) session.getAttribute(SESSION_ACCESS_TOKEN);
        String p = (configuredAccessToken != null && !configuredAccessToken.isBlank()) ? "ja" : "nein";
        String e = (System.getenv("EVERNOTE_ACCESS_TOKEN") != null && !System.getenv("EVERNOTE_ACCESS_TOKEN").isBlank()) ? "ja" : "nein";
        String f = (LocalTokenStore.getSingleton().load() != null) ? "ja (~/.evernote-oauth/access-token.txt)" : "nein";
        return "Session: " + (s != null && !s.isBlank() ? "ja" : "nein") + ", properties: " + p + ", env: " + e + ", file: " + f;
    }

    @GetMapping(value = "/auth/forget", produces = "text/plain")
    @ResponseBody
    public String forgetToken(HttpSession session) {
        session.removeAttribute(SESSION_ACCESS_TOKEN);
        LocalTokenStore.getSingleton().clear();
        return "Token aus Session und Dateispeicher entfernt.";
    }

}