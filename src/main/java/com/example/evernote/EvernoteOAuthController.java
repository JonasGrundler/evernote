package com.example.evernote;

import com.evernote.auth.EvernoteAuth;
import com.evernote.clients.ClientFactory;
import com.evernote.clients.NoteStoreClient;
import com.evernote.edam.error.EDAMErrorCode;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.*;
import com.evernote.edam.notestore.NoteStore.Client;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.NoteSortOrder;
import com.evernote.edam.type.Resource;
import com.evernote.edam.type.Tag;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.oauth.OAuth10aService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import net.sourceforge.tess4j.util.ImageHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    private final LocalTokenStore tokenStore = new LocalTokenStore();

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
        tokenStore.save(accessToken.getToken());
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
        if (token == null || token.isBlank()) token = tokenStore.load();
        if (token == null || token.isBlank())
            throw new IllegalStateException("Kein Evernote-Access-Token vorhanden. Bitte zuerst /evernote/oauth/start aufrufen.");
        return token;
    }

    private NoteStoreClient getNoteStore(HttpSession session) throws Exception {
        String token = getToken(session);
        EvernoteAuth userAuth = new EvernoteAuth(com.evernote.auth.EvernoteService.PRODUCTION, token);
        ClientFactory factory = new ClientFactory(userAuth);
        return factory.createNoteStoreClient();
    }

    private static String enmlFromPlain(String text) {
        String esc = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">" + "<en-note>" + esc + "</en-note>";
    }

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
            List<Tag> tags = noteStore.listTags();
            EMailToTags eMailToTags = new EMailToTags();
            // validate...
            for (String eTag : eMailToTags.getAllTags()) {
                boolean found = false;
                for (Tag tag : tags) {
                    if (tag.getName().equals(eTag)) {
                        found = true;
                        eMailToTags.setGUID(tag.getName(), tag.getGuid());
                        continue;
                    }
                }
                if (! found) {
                    System.out.println("eTag " + eTag + " not found!");
                }
            }



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
            LocalNoteStore lns = new LocalNoteStore();
            for (NoteMetadata nm : l) {
                try {
                    System.out.println("attributes:" + nm.getAttributes().getAuthor() + " " + nm.getAttributes().toString());

                    // title for paper documents
                    if (
                            nm.getAttributes() != null &&
                                    source.equals(nm.getAttributes().getSource()) &&
                                    (nm.getTitle() == null || nm.getTitle().equals(title) || nm.getTitle().equals(""))) {
                        System.out.println("----------------- reco -------------------");
                        try {
                            Note note = lns.load(nm.getGuid(), nm.getUpdateSequenceNum());
                            if (note == null) {
                                note = noteStore.getNote(nm.getGuid(), true, true, true, true);
                                lns.save(note);
                                System.out.println("note from internet:" + note.getTitle());
                            } else {
                                System.out.println("note from store:" + note.getTitle());
                            }
                            //System.out.println("note.content" + note.getContent());
                            //System.out.println("info:" + note.isSetTitle() + ", " + note.getAttributes().getAuthor() + ", " + note.getAttributes().getSource());
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
                                            lns.writeJpg(img, rn, note.getGuid(), note.getUpdateSequenceNum());
                                            top = img.getSubimage(img.getWidth() / 3 * 2, 0, img.getWidth() / 3, (img.getHeight() / 30));
                                            lns.writeJpg(top, rn, note.getGuid() + "_compressed", note.getUpdateSequenceNum());
                                        }

                                    } catch (Exception e) {
                                    }
                                    if (top != null) {
                                        // not in use
                                    /*try {
                                        byte[] b = r.getRecognition().getBody();
                                        String s = new String(b);
                                        System.out.println("s:" + s);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }*/
                                        // not in use
                                    /*try {
                                        String ocr = doOCR(top);
                                        System.out.println("ocr:" + ocr);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }*/
                                        // this is used
                                        try {
                                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                            ImageIO.write(top, "jpg", baos);          // Format wählen
                                            TrOcrClient trocr = new TrOcrClient(trocrBaseUrl);
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
                                                        sb.append("*** change to " + text + " ");
                                                        note.setTitle(text);
                                                        noteStore.updateNote(note);
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
                        for (String key : eMailToTags.getEMails()) {
                            if (nm.getAttributes().getAuthor().endsWith(key)) {
                                for (String tagname : eMailToTags.getTags(key)) {
                                    String guid = eMailToTags.getGUID(tagname);
                                    if (nm.getTagGuids() == null || !nm.getTagGuids().contains(guid)) {
                                        System.out.println("1 adding tags for " + nm.getTitle() + ":" + tagname);
                                        missingGuids.add(guid);
                                    }
                                }
                            }
                        }
                        if (missingGuids.size() > 0) {
                            System.out.println("2 adding tags for " + nm.getTitle() + ":" + missingGuids.size());
                            Note note = lns.load(nm.getGuid(), nm.getUpdateSequenceNum());
                            if (note == null) {
                                note = noteStore.getNote(nm.getGuid(), true, true, true, true);
                                lns.save(note);
                                System.out.println("note from internet:" + note.getTitle());
                            } else {
                                System.out.println("note from store:" + note.getTitle());
                            }
                            if (note != null) {
                                System.out.println("3 adding tags for " + nm.getTitle() + ":" + missingGuids);
                                for (String guid : missingGuids) {
                                    System.out.println("adding:" + guid);
                                    note.addToTagGuids(guid);
                                }
                                noteStore.updateNote(note);
                            }
                        }
                        sb.append(nm.getTitle());
                        sb.append("\n");
                    }
                } catch (Exception e) {e.printStackTrace();}
                //meta.getNotes().forEach(n -> sb.append(n.getTitle()).append(" | GUID=").append(n.getGuid()).append('\n'));
            }
            return sb.toString();
        } catch (EDAMUserException e) {
            if (e.getErrorCode() == EDAMErrorCode.AUTH_EXPIRED) {
                tokenStore.clear();
                return "Auth expired. Token wurde gelöscht. Bitte neu autorisieren: /evernote/oauth/start";
            }
            throw e;
        }
    }

    private String doOCR(BufferedImage img) throws Exception {
        // 3) OCR ausführen (nur oben)
        LocalOcrService ocr = new LocalOcrService();

        String text = ocr.ocrTop(img);

        return text != null ? text : "";
    }


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
    }

    @GetMapping(value = "/auth/status", produces = "text/plain")
    @ResponseBody
    public String authStatus(HttpSession session) {
        String s = (String) session.getAttribute(SESSION_ACCESS_TOKEN);
        String p = (configuredAccessToken != null && !configuredAccessToken.isBlank()) ? "ja" : "nein";
        String e = (System.getenv("EVERNOTE_ACCESS_TOKEN") != null && !System.getenv("EVERNOTE_ACCESS_TOKEN").isBlank()) ? "ja" : "nein";
        String f = (new LocalTokenStore().load() != null) ? "ja (~/.evernote-oauth/access-token.txt)" : "nein";
        return "Session: " + (s != null && !s.isBlank() ? "ja" : "nein") + ", properties: " + p + ", env: " + e + ", file: " + f;
    }

    @GetMapping(value = "/auth/forget", produces = "text/plain")
    @ResponseBody
    public String forgetToken(HttpSession session) {
        session.removeAttribute(SESSION_ACCESS_TOKEN);
        new LocalTokenStore().clear();
        return "Token aus Session und Dateispeicher entfernt.";
    }

}