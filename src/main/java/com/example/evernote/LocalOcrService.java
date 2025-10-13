package com.example.evernote;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.util.ImageHelper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.io.ByteArrayInputStream;

public class LocalOcrService {

    private final String tessdataPath = "C:\\Users\\Jonas\\AppData\\Local\\Programs\\Tesseract-OCR\\tessdata";
    private final String language = "deu+eng";
    private final double topFraction = 0.1;

    public LocalOcrService() {
    }

    /** Haupt-API: OCR nur im oberen Bildanteil (z.B. topFraction=0.33 -> oberes Drittel). */
    public String ocrTop(BufferedImage img) throws Exception {
        if (img == null) return "";

        // optional: Orientierung / Größe optimieren (Upscale hilft Tesseract oft bei Handschrift)

        return runTesseract(img);
    }

     /** Tesseract ausführen. */
    private String runTesseract(BufferedImage img) throws Exception {
        ITesseract t = new Tesseract();
        if (tessdataPath != null && !tessdataPath.isBlank()) {
            t.setDatapath(tessdataPath);
        }
        t.setLanguage(language != null ? language : "deu+eng");

        // Für Handschrift experimentieren:
        //t.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_LSTM_ONLY);
        // Page segmentation mode: ausprobierenswert 6 (Block), 11 (Sparse), 13 (Raw line)
        t.setPageSegMode(ITessAPI.TessPageSegMode.PSM_SINGLE_BLOCK); // oder PSM_SINGLE_BLOCK=6, PSM_SPARSE_TEXT=11, PSM_RAW_LINE=13

        // DPI hochsetzen, damit Tesseract vernünftig skaliert:
        //t.setTessVariable("user_defined_dpi", "300");
        // Leerzeichen beibehalten (hilft bei Handschrift oft, musst ausprobieren):
        //t.setTessVariable("preserve_interword_spaces", "1");

        t.setVariable("tessedit_char_whitelist", "0123456789.()");
        t.setVariable("load_system_dawg", "F");
        t.setVariable("load_freq_dawg", "F");

        return t.doOCR(img);
    }
}
