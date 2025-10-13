package com.example.evernote;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@SpringBootApplication
@RestController
public class EvernoteOAuthSampleApplication {
    public static void main(String[] args) { SpringApplication.run(EvernoteOAuthSampleApplication.class, args); }
    @GetMapping("/")
    public String home() {
        return "<html><body><h2>Evernote OAuth Sample</h2>"
             + "<p><a href='/evernote/oauth/start'>Mit Evernote verbinden</a></p>"
             + "<p><a href='/auth/status'>Auth-Status</a></p>"
             + "<p><a href='/notes'>Notizen auflisten</a></p>"
             + "</body></html>";
    }
}