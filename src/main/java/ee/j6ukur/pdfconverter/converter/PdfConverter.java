package ee.j6ukur.pdfconverter.converter;

import com.spire.pdf.PdfDocument;
import com.spire.pdf.PdfPageBase;
import com.spire.pdf.PdfPageRotateAngle;
import com.spire.pdf.graphics.PdfImage;
import com.spire.pdf.graphics.PdfImageType;
import com.spire.pdf.graphics.PdfMargins;
import com.spire.pdf.graphics.PdfPen;
import com.spire.pdf.graphics.PdfRGBColor;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.imageio.ImageIO;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeException;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;
import net.sourceforge.barbecue.output.OutputException;
import org.apache.commons.io.IOUtils;

public abstract class PdfConverter implements Closeable {

    public static final int PADDING = 10;
    public static final int PAGE_MARGIN = 0;

    public static final int MAX_ROWS = 4;
    public static final int MAX_COLUMNS = 2;
    public static final PdfPen PEN = new PdfPen(new PdfRGBColor(Color.black), 0.1);

    private int row;
    private int col;
    private boolean lines;
    private boolean all;
    protected PdfDocument originalPdfDocument;

    private PdfImage pdfImage;
    private PdfImage barcodeImage;

    private PdfDocument pdfDocument;

    public void render(OutputStream out) {
        // needs to be done before rotating the original pdf
        barcodeImage = PdfImage.fromImage(getBarcodeImage(getBarcodeValue()));

        // rotate original pdf, create image and crop
        BufferedImage pdfAsCroppedImage = reEncodeBufferedImage(getPdfAsCroppedImage());
        pdfImage = PdfImage.fromImage(pdfAsCroppedImage);

        pdfDocument = createNewPdf();

        if (all) {
            addAll();
        } else {
            addPdfAndBarcode(row, col);
        }
        pdfDocument.saveToStream(out);
        this.close();
    }

    private static BufferedImage reEncodeBufferedImage(BufferedImage bufferedImage) {
        ByteArrayOutputStream outArray = new ByteArrayOutputStream();
        writeImage(outArray, bufferedImage);
        try {
            return ImageIO.read(new ByteArrayInputStream(outArray.toByteArray()));
        } catch (IOException e) {
            throw new IllegalArgumentException("unable to re-encode image", e);
        }
    }

    public void image(OutputStream out) {
        BufferedImage image = getPdfAsCroppedImage();
        writeImage(out, image);
        this.close();
    }

    protected abstract String getBarcodeValue();

    private BufferedImage getPdfAsCroppedImage() {
        rotateOriginalPdf();
        return crop(
                originalPdfDocument.saveAsImage(0, PdfImageType.Bitmap, 300, 300)
        )
        ;
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
        int imageWidth = 275;
        int imageHeight = 190;


        double x = (column - 1) * (page.getSize().getWidth() - 2 * PAGE_MARGIN) / MAX_COLUMNS + PADDING;
        double y = (row - 1) * (page.getSize().getHeight() - 2 * PAGE_MARGIN) / MAX_ROWS + PADDING;
        Rectangle imageBorders = new Rectangle((int)x, (int)y, imageWidth, imageHeight);
        page.getCanvas().drawImage(pdfImage, imageBorders);
        if(lines) {
            page.getCanvas().drawRectangle(PEN, imageBorders);
        }

        Rectangle barCodeCoordinates = getBarCodeCoordinates();
        page.getCanvas().drawImage(barcodeImage, x + barCodeCoordinates.getX(),y + barCodeCoordinates.getY(), barCodeCoordinates.getWidth(), barCodeCoordinates.getHeight());
    }

    protected abstract Rectangle getBarCodeCoordinates();

    private BufferedImage getBarcodeImage(String value) {
        try {
            Barcode barcode = createNewBarcode(value);
            return rotateClockwise90(BarcodeImageHandler.getImage(barcode));
        } catch (OutputException | BarcodeException e) {
            throw new IllegalArgumentException("Unable to render barcode", e);
        }
    }

    protected enum BarCodeAlgorithm {
        A,// Code 128 A
        B,// Code 128 B
        C // Code 128 C
    }

    private Barcode createNewBarcode(String value) throws BarcodeException {
        Barcode barcode;
        switch (getBarcodeAlogorithm()) {
            case A:
                barcode = BarcodeFactory.createCode128A(value);
                break;
            case B:
                barcode = BarcodeFactory.createCode128B(value);
                break;
            case C:
                barcode = BarcodeFactory.createCode128C(value);
                break;
            default:
                throw new IllegalStateException("Invalid barcode type");
        }
        barcode.setDrawingText(false);
        barcode.setDrawingQuietSection(false);
        return barcode;
    }

    protected abstract BarCodeAlgorithm getBarcodeAlogorithm();

    private static BufferedImage rotateClockwise90(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();

        BufferedImage dest = new BufferedImage(height, width, src.getType());

        Graphics2D graphics2D = dest.createGraphics();
        graphics2D.translate((height - width) / 2, (height - width) / 2);
        graphics2D.rotate(Math.PI / 2, (double)height / 2, (double)width / 2);
        graphics2D.drawRenderedImage(src, null);

        return dest;
    }

    private PdfDocument createNewPdf() {
        PdfDocument document = new PdfDocument();
        document.getPageSettings().setMargins(new PdfMargins(PAGE_MARGIN));
        PdfPageBase page = document.appendPage();
        if (lines) {
            drawLines(page);
        }
        return document;
    }

    private static void drawLines(PdfPageBase page) {
        // vertical lines
        for (int i = 1; i < MAX_COLUMNS; i++) {
            double x = (page.getSize().getWidth() - 2 * PAGE_MARGIN) / MAX_COLUMNS * i;
            page.getCanvas().drawLine(PEN, x, 0, x, page.getSize().getHeight());
        }

        // horizontal lines
        for (int i = 1; i < MAX_ROWS; i++) {
            double y = (page.getSize().getHeight() - 2 * PAGE_MARGIN) / MAX_ROWS * i;
            page.getCanvas().drawLine(PEN, 0, y, page.getSize().getWidth(), y);
        }
    }

    private BufferedImage crop(BufferedImage src) {
        Rectangle rectangle = getCropRectangle();
        return src.getSubimage((int)rectangle.getX(), (int)rectangle.getY(), (int)rectangle.getWidth(), (int)rectangle.getHeight());
    }

    private static void writeImage(OutputStream out, BufferedImage image) {
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException e) {
            throw new IllegalArgumentException("unable to write image", e);
        }
    }


    private void rotateOriginalPdf() {
        getPage().setRotation(PdfPageRotateAngle.Rotate_Angle_90);
    }

    public void close() {
        if(originalPdfDocument != null) {
            originalPdfDocument.close();
        }
        if(pdfDocument != null) {
            pdfDocument.close();
        }
    }

    protected PdfPageBase getPage() {
        return originalPdfDocument.getPages().get(0);
    }

    protected abstract Rectangle getCropRectangle();

    public static final class PdfConverterBuilder {
        private String url;
        private File file;
        private int row = 1;
        private int col = 1;
        private boolean lines = false;
        private boolean all = false;

        public static PdfConverterBuilder aPdfConverter(String url) {
            return new PdfConverterBuilder(url);
        }

        private PdfConverterBuilder(String url) {
            this.url = url;
        }

        public static PdfConverterBuilder aPdfConverter(File file) {
            return new PdfConverterBuilder(file);
        }

        private PdfConverterBuilder(File file) {
            this.file = file;
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
            PdfDocument doc;
            try(
                    InputStream is = url != null ? getUrlInputStream(url) : getFileInputStream(file)
                ) {
                doc = new PdfDocument();
                doc.loadFromStream(is);
            } catch (IOException | InterruptedException e) {
                throw new IllegalArgumentException("Unable to read PDF", e);
            }

            PdfConverter pdfConverter = chooseImplementation(doc);
            pdfConverter.row = this.row;
            pdfConverter.lines = this.lines;
            pdfConverter.all = this.all;
            pdfConverter.col = this.col;
            pdfConverter.originalPdfDocument = doc;
            return pdfConverter;
        }

        private static InputStream getUrlInputStream(String url) throws IOException, InterruptedException {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .timeout(Duration.ofSeconds(10L))
                    .GET()
                    .uri(URI.create(url))
                    .build();
            HttpResponse<InputStream> resp = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if(resp.statusCode() != 200) {
                String content = IOUtils.toString(resp.body(), StandardCharsets.UTF_8);
                resp.body().close();
                throw new IllegalStateException("PDF query resulted in error! Status: " + resp.statusCode() + ". Headers: " + resp.headers() + ". Content: " + content);
            }
            return resp.body();
        }

        private static InputStream getFileInputStream(File file) {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException("file not found!", e);
            }
        }

        private static PdfConverter chooseImplementation(PdfDocument doc) {
            PdfPageBase page = doc.getPages().get(0);
            if(page.findText("omniva.ee").getFinds().length > 0) {
                return new OmnivaPdfConverter();
            }

            if(page.findText("smartpost.ee").getFinds().length > 0) {
                return new SmartpostPdfConverter();
            }

            if(page.findText("DPD").getFinds().length > 0) {
                return new DpdPdfConverter();
            }

            throw new IllegalArgumentException("Unknown label, unable to determine type");
        }
    }
}
