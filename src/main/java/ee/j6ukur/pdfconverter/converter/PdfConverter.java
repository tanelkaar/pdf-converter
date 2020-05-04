package ee.j6ukur.pdfconverter.converter;

import com.spire.pdf.PdfDocument;
import com.spire.pdf.PdfPageBase;
import com.spire.pdf.graphics.PdfImage;
import com.spire.pdf.graphics.PdfPen;
import com.spire.pdf.graphics.PdfRGBColor;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.imageio.ImageIO;
import org.apache.commons.io.IOUtils;

public class PdfConverter {

    public static final float PDF_RESIZE_FACTOR = 0.5f;
    public static final float BARCODE_RESIZE_X_FACTOR = 0.96f;
    public static final float BARCODE_RESIZE_Y_FACTOR = 0.55f;
    public static final int PADDING = 10;
    public static final int PAGE_MARGIN = 15;

    public static final int MAX_ROWS = 4;
    public static final int MAX_COLUMNS = 2;

    private String url;

    public PdfConverter(String url, OutputStream out) {
        this.url = url;
        this.out = out;
    }

    private OutputStream out;
    private int row;
    private int col;
    private boolean lines;
    private boolean all;

    private PdfImage pdf;
    private PdfImage barcode;

    private PdfDocument pdfDocument;

    public static void main(String[] args) throws IOException, InterruptedException {
        String url = args[0];
        OutputStream out = new FileOutputStream(new File("tmp.pdf"));
        int row = 1;
        int col = 2;
        PdfConverterBuilder.aPdfConverter().withUrl(url).withOut(out).withRow(row).withCol(col).build().render();
        out.close();
    }

    public void render() throws IOException, InterruptedException {
        getPdfAndBarcodeAsImages(getPdfInputStream(url));
        PdfPageBase page = createNewPdf();

//        writeImage(out, rotatedImage);
        if (all) {
            addAll();
        } else {
            addPdfAndBarcode(row, col);
        }
        pdfDocument.saveToStream(out);
        pdfDocument.close();
    }

    private void addAll() {
        for (int r = 1; r < MAX_ROWS + 1; r++) {
            for (int c = 1; c < MAX_COLUMNS + 1; c++) {
                addPdfAndBarcode(r, c);
            }
        }

    }

    private void addPdfAndBarcode(int row, int column) {
        PdfPageBase page = pdfDocument.getPages().get(0);
        float pdfWidth = pdf.getWidth() * PDF_RESIZE_FACTOR;
        float pdfHeight = pdf.getHeight() * PDF_RESIZE_FACTOR;

        float barcodeWidth = barcode.getWidth() * BARCODE_RESIZE_X_FACTOR;
        float barcodeHeight = barcode.getHeight() * BARCODE_RESIZE_Y_FACTOR;

        double x = (column - 1) * (page.getSize().getWidth() - 2 * PAGE_MARGIN) / MAX_COLUMNS + PADDING;
        double y = (row - 1) * (page.getSize().getHeight() - 2 * PAGE_MARGIN) / MAX_ROWS + PADDING;
        page.getCanvas().drawImage(pdf, x, y, pdfWidth, pdfHeight);

        double barcodeX = x + 30 * PDF_RESIZE_FACTOR;
        double barcodeY = y + 10 * PDF_RESIZE_FACTOR;
        page.getCanvas().drawImage(barcode, barcodeX, barcodeY, barcodeWidth, barcodeHeight);
    }

    private PdfPageBase createNewPdf() {
        pdfDocument = new PdfDocument();
        pdfDocument.getPageSettings().setMargins(PAGE_MARGIN, PAGE_MARGIN, PAGE_MARGIN, PAGE_MARGIN);
        PdfPageBase page = pdfDocument.appendPage();
        if (lines) {
            drawLines(page);
        }
        return page;
    }

    private static void drawLines(PdfPageBase page) {
        PdfPen pen = new PdfPen(new PdfRGBColor(Color.black), 0.1);
        // vertical lines
        for (int i = 1; i < MAX_COLUMNS; i++) {
            double x = (page.getSize().getWidth() - 2 * PAGE_MARGIN) / MAX_COLUMNS * i;
            page.getCanvas().drawLine(pen, x, 0, x, page.getSize().getHeight());
        }

        // horizontal lines
        for (int i = 1; i < MAX_ROWS; i++) {
            double y = (page.getSize().getHeight() - 2 * PAGE_MARGIN) / MAX_ROWS * i;
            page.getCanvas().drawLine(pen, 0, y, page.getSize().getWidth(), y);
        }
    }

    private static BufferedImage crop(BufferedImage src) {
        return src.getSubimage(40, 42, 335, 500);
    }

    private static void writeImage(OutputStream out, BufferedImage image) throws IOException {
        ImageIO.write(image, "png", out);
    }

    private static BufferedImage rotateClockwise90(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();

        BufferedImage dest = new BufferedImage(height, width, src.getType());

        Graphics2D graphics2D = dest.createGraphics();
        graphics2D.translate((height - width) / 2, (height - width) / 2);
        graphics2D.rotate(Math.PI / 2, height / 2, width / 2);
        graphics2D.drawRenderedImage(src, null);

        return dest;
    }

    private void getPdfAndBarcodeAsImages(InputStream pdfIs) throws IOException {
        try (pdfIs) {
            PdfDocument doc = new PdfDocument();
            doc.loadFromStream(pdfIs);
            barcode = PdfImage.fromImage(rotateClockwise90(doc.getPages().get(0).getImagesInfo()[1].getImage()));
            pdf = PdfImage.fromImage(rotateClockwise90(crop(doc.saveAsImage(0))));
            doc.close();
        }
    }

    private static InputStream getPdfInputStream(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(10l))
                .GET()
                .uri(URI.create(url))
                .build();
        HttpResponse<InputStream> resp = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if(resp.statusCode() != 200) {
            String content = IOUtils.toString(resp.body(), "UTF-8");
            resp.body().close();
            throw new IllegalStateException("PDF query resulted in error! Status: " + resp.statusCode() + ". Headers: " + resp.headers() + ". Content: " + content);
        }
        return resp.body();
    }

    public static final class PdfConverterBuilder {
        private String url;
        private OutputStream out;
        private int row;
        private int col;
        private boolean lines;
        private boolean all;

        private PdfConverterBuilder() {
        }

        public static PdfConverterBuilder aPdfConverter() {
            return new PdfConverterBuilder();
        }

        public PdfConverterBuilder withUrl(String url) {
            this.url = url;
            return this;
        }

        public PdfConverterBuilder withOut(OutputStream out) {
            this.out = out;
            return this;
        }

        public PdfConverterBuilder withRow(int row) {
            this.row = row;
            return this;
        }

        public PdfConverterBuilder withCol(int col) {
            this.col = col;
            return this;
        }

        public PdfConverterBuilder withLines(boolean lines) {
            this.lines = lines;
            return this;
        }

        public PdfConverterBuilder withAll(boolean all) {
            this.all = all;
            return this;
        }

        public PdfConverter build() {
            PdfConverter pdfConverter = new PdfConverter(url, out);
            pdfConverter.row = this.row;
            pdfConverter.lines = this.lines;
            pdfConverter.all = this.all;
            pdfConverter.col = this.col;
            return pdfConverter;
        }
    }
}
