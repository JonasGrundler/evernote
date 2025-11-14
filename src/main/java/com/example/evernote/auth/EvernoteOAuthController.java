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

    /*
    private NoteStoreClient getNoteStore(HttpSession session) throws Exception {
        String token = getToken(session);
        EvernoteAuth userAuth = new EvernoteAuth(com.evernote.auth.EvernoteService.PRODUCTION, token);
        ClientFactory factory = new ClientFactory(userAuth);
        return factory.createNoteStoreClient();
    }*/

    /*
    private static String enmlFromPlain(String text) {
        String esc = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">" + "<en-note>" + esc + "</en-note>";
    }*/

    /*
    @GetMapping(value = "/notes", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String listNotes(
            @RequestParam(name = "source", defaultValue = "mobile.iphone") String source,
            @RequestParam(name = "title", defaultValue = "Foto") String title,
            @RequestParam(name = "year", defaultValue = "0") String year,
            HttpSession session) throws Exception {
        try {
            NoteStoreClient noteStore = getNoteStore(session);

            // prepare tags for emails

            if (year.equals("0")) {
                year = String.valueOf(java.time.Year.now().getValue());
            }
            System.out.println("parameters: " + "source=" + source + ", title=" + title + ", year=" + year);
            NoteFilter f = new NoteFilter();
            // Name immer in Anführungszeichen, ggf. " escapen:
            String nbName = "00 Eingang";
            String q = "notebook:\"" + nbName.replace("\"", "\\\"") + "\"";
            f.setWords(q);

            NotesMetadataResultSpec spec = new NotesMetadataResultSpec();
            spec.setIncludeTitle(true);
            spec.setIncludeUpdateSequenceNum(true);
            spec.setIncludeAttributes(true);

            NotesMetadataList meta = noteStore.findNotesMetadata(f, 0, 100, spec);
            // → liefert nur Notizen aus genau diesem Notizbuch (per Name gefiltert)

            StringBuilder sb = new StringBuilder("Letzte 100 Notizen:\n\n");
            List<NoteMetadata> l = meta.getNotes();
            for (NoteMetadata nm : l) {
                try {
                    System.out.println("attributes:" + nm.getAttributes().getAuthor() + " " + nm.getAttributes().toString());
                    sb.append(nm.getTitle());
                    boolean noteUpdated = false;
                    Note note = lns.load(nm.getGuid(), nm.getUpdateSequenceNum());
                    if (note == null) {
                        note = noteStore.getNote(nm.getGuid(), true, true, true, true);
                        lns.save(note);
                        System.out.println("note from internet:" + note.getTitle());
                    } else {
                        System.out.println("note from store:" + note.getTitle());
                    }

                    // tagging from machine learning
                    String predictedTags = null;
                    try {
                        internetHelper.parseAndWrite(note);
                        ZoneId zone = ZoneId.of("Europe/Berlin");
                        Instant instant = Instant.ofEpochMilli(note.getCreated());
                        ZonedDateTime zdt = instant.atZone(zone);

                        File csv = BuildCSV.writeSingleLine(internetHelper.getTargetDir().toFile(), note.getGuid(), String.valueOf(zdt.getYear()));
                        System.out.println("csvLine:" + csv.getAbsolutePath());
                        String line;
                        while ((line = infPyOut.readLine()) == null || ! line.trim().equals("waiting")) {
                            if (line != null) {
                                System.out.println("inference:" + line);
                            }
                        }
                        infPyIn.write(csv.getAbsolutePath());
                        infPyIn.newLine();
                        infPyIn.flush();
                        // read first...
                        predictedTags = infPyOut.readLine();
                        System.out.println("predictedTags:" + predictedTags);
                        while ((line = infPyOut.readLine()) == null || ! line.trim().equals("done"));
                    } catch (Exception e) {
                        e.printStackTrace(System.out);
                    }
                    if (predictedTags != null) {
                        String[] predictedTagsArray = predictedTags.split(",");
                        for (String t : predictedTagsArray) {
                            t = t.trim();
                            System.out.println("predict:'" + t + "'");
                            for (Tag tag : EMailToTags.getSingleton().getTags()) {
                                // done-tag ist für Steuer und wird für neue Dokumente natürlich nicht gesetzt
                                boolean isNumber = false;
                                try {
                                    Integer.parseInt(t);
                                    isNumber = true;
                                } catch (Exception e) {}
                                if (tag.getName().equals(t)) {
                                    if (
                                            ! tag.getName().equals("done") &&
                                            ! isNumber
                                    ) {
                                        if (note.getTagGuids() == null || !note.getTagGuids().contains(tag.getGuid())) {
                                            System.out.println("predict added:'" + t + "'");
                                            note.addToTagGuids(tag.getGuid());
                                            noteUpdated = true;
                                        }
                                    }
                                }
                            }
                        }
                    }


                    // title for paper documents
                    if (
                            nm.getAttributes() != null &&
                            source.equals(nm.getAttributes().getSource()) &&
                            (nm.getTitle() == null || nm.getTitle().equals(title) || nm.getTitle().equals(""))) {
                        System.out.println("----------------- reco -------------------");
                        try {
                            int rn = 0;
                            if (
                                    note.getResources() != null &&
                                    note.getResources().size() > 0
                            ) {

                                //for (Resource r : note.getResources())
                                Resource r = note.getResources().get(0);
                                {
                                    //System.out.println("note.resource.type" + r.getMime() + ":" + note.getContentLength());

                                    rn++;
                                    BufferedImage top = null;
                                    try {
                                        BufferedImage img = ImageIO.read(new ByteArrayInputStream(r.getData().getBody()));
                                        if (img != null && img.getWidth() > 1000 && img.getHeight() > 1000) {
                                            top = img.getSubimage(img.getWidth() / 3 * 2, 0, img.getWidth() / 3, (img.getHeight() / 30));
                                        }

                                    } catch (Exception e) {
                                    }
                                    if (top != null) {
                                        // this is used
                                        try {
                                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                            ImageIO.write(top, "jpg", baos);          // Format wählen
                                            TrOcrClient trocr = new TrOcrClient();
                                            String json = trocr.ocr(baos.toByteArray());
                                            System.out.println("trocr:" + json);
                                            ObjectMapper om = new ObjectMapper();
                                            String text = om.readTree(json).path("text").asText();
                                            text = text.replaceAll("\\s+", "");
                                            text = text.replace(',', '.');
                                            if (text.startsWith(year + ".")) {
                                                while (text.endsWith(".")) {
                                                    text = text.substring(0, text.length() - 1);
                                                }
                                                if (Character.isDigit(text.charAt(text.length() - 1)) || text.charAt(text.length() - 1) == ')') {
                                                    if (text.length() > 8) {
                                                        // change title
                                                        sb.append(" change title to " + text + " ");
                                                        note.setTitle(text);
                                                        noteUpdated = true;
                                                    }
                                                }
                                            }
                                            System.out.println("text:" + text);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (
                            nm.getAttributes() != null &&
                                    nm.getAttributes().getSource() != null &&
                                    nm.getAttributes().getSource().equals("mail.smtp") &&
                                    nm.getAttributes().getAuthor() != null
                    ) {
                        System.out.println("tags for " + nm.getTitle());
                        List<String> missingGuids = new ArrayList<>();
                        for (String key : EMailToTags.getSingleton().getEMails()) {
                            if (nm.getAttributes().getAuthor().endsWith(key)) {
                                for (String tagname : EMailToTags.getSingleton().getTags(key)) {
                                    String guid = EMailToTags.getSingleton().getGUID(tagname);
                                    if (nm.getTagGuids() == null || !nm.getTagGuids().contains(guid)) {
                                        System.out.println("1 adding tags for " + nm.getTitle() + ":" + tagname);
                                        missingGuids.add(guid);
                                    }
                                }
                            }
                        }
                        if (missingGuids.size() > 0) {
                            //System.out.println("2 adding tags for " + nm.getTitle() + ":" + missingGuids.size());
                            //System.out.println("3 adding tags for " + nm.getTitle() + ":" + missingGuids);
                            for (String guid : missingGuids) {
                                System.out.println("adding:" + guid);
                                note.addToTagGuids(guid);
                                sb.append(" adding tag:");
                                for (Tag tag : tags) {
                                    if (tag.getGuid().equals(guid)) {
                                        sb.append(tag);
                                    }
                                }
                            }
                            noteUpdated = true;
                        }
                    }
                    if (noteUpdated) {
                        System.out.println("update:" + note.getTitle());
                        noteStore.updateNote(note);

                    }
                } catch (Exception e) {e.printStackTrace(System.out);}
                sb.append("\n");
                //meta.getNotes().forEach(n -> sb.append(n.getTitle()).append(" | GUID=").append(n.getGuid()).append('\n'));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            if (e instanceof EDAMUserException) {
                EDAMUserException eue = (EDAMUserException) e;
                if (eue.getErrorCode() == EDAMErrorCode.AUTH_EXPIRED) {
                    tokenStore.clear();
                    return "Auth expired. Token wurde gelöscht. Bitte neu autorisieren: /evernote/oauth/start";
                }
            }
            throw e;
        }
    }
    */


    /*
    @GetMapping(value = "/notes/search", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String searchNotes(@RequestParam("q") String query, HttpSession session) throws Exception {
        NoteStoreClient noteStore = getNoteStore(session);
        NoteFilter filter = new NoteFilter();
        filter.setWords(query);
        filter.setOrder(NoteSortOrder.UPDATED.getValue());
        filter.setAscending(false);
        NotesMetadataResultSpec spec = new NotesMetadataResultSpec();
        spec.setIncludeTitle(true);
        NotesMetadataList meta = noteStore.findNotesMetadata(filter, 0, 20, spec);
        StringBuilder sb = new StringBuilder("Suche: \"").append(query).append("\"\n\n");
        meta.getNotes().forEach(n -> sb.append(n.getTitle()).append(" | GUID=").append(n.getGuid()).append('\n'));
        return sb.toString();
    }

    @GetMapping(value = "/notes/delete", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String deleteNote(@RequestParam("guid") String guid, HttpSession session) throws Exception {
        NoteStoreClient noteStore = getNoteStore(session);
        int usn = noteStore.deleteNote(guid);
        return "Gelöscht GUID=" + guid + ", USN=" + usn;
    }

    @GetMapping(value = "/notes/create", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String createNote(@RequestParam("text") String text, HttpSession session) throws Exception {
        NoteStoreClient noteStore = getNoteStore(session);
        Note note = new Note();
        note.setTitle("API Note");
        note.setContent(enmlFromPlain(text));
        Note created = noteStore.createNote(note);
        return "Erstellt GUID=" + created.getGuid();
    }*/

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