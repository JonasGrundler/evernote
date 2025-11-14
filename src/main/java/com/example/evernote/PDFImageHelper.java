package com.example.evernote;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class PDFImageHelper extends PDFStreamEngine {

    private static final int IMG_MIN_WIDTH = 500;
    private static final int IMG_MIN_HEIGHT = 100;

    private int images;

    private PDFImageHelper() {
        this.images = 0;
    }

    private static final PDFImageHelper singleton;

    static {
        PDFImageHelper ls = null;
        try {
            ls = new PDFImageHelper();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        singleton = ls;
    }

    public static PDFImageHelper getSingleton() {
        return singleton;
    }

    @Override
    protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
        String op = operator.getName();

        if ("Do".equals(op)) {
            COSName objectName = (COSName) operands.get(0);
            PDXObject xobject = getResources().getXObject(objectName);

            if (xobject instanceof PDImageXObject image) {

                if (image.getImage().getWidth() > IMG_MIN_WIDTH && image.getImage().getHeight() > IMG_MIN_HEIGHT) {
                    // Dateiname bauen
                    File file = new File(LocalStore.getSingleton().getImages_tmp().toString(), images + ".png");
                    images++;

                    // als PNG speichern (geht sicher)
                    ImageIO.write(image.getImage(), "png", file);
                }
            } else if (xobject instanceof PDFormXObject form) {
                // falls das Bild in einem Form/XObject versteckt ist: rekursiv
                showForm(form);
            }
        } else {
            super.processOperator(operator, operands);
        }
    }

    public int processPDF(byte[] bytes, int images) {
        this.images = images;
        // images
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            for (PDPage page : doc.getPages()) {
                processPage(page);
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return images;
    }

}
