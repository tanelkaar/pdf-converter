package ee.j6ukur.pdfconverter.converter;

import com.spire.pdf.PdfDocument;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PdfConverterTest {

    @Test
    void getOmnivaConverter() {
        PdfConverter converter = PdfConverter.PdfConverterBuilder.aPdfConverter(new File("src/test/resources/omniva.pdf")).build();
        converter.close();
        Assertions.assertTrue(converter instanceof OmnivaPdfConverter);
    }

    @Test
    void getSmartpostConverter() {
        PdfConverter converter = PdfConverter.PdfConverterBuilder.aPdfConverter(new File("src/test/resources/itella.pdf")).build();
        converter.close();
        Assertions.assertTrue(converter instanceof SmartpostPdfConverter);
    }

    @Test
    void getDpdConverter() {
        PdfConverter converter = PdfConverter.PdfConverterBuilder.aPdfConverter(new File("src/test/resources/dpd.pdf")).build();
        converter.close();
        Assertions.assertTrue(converter instanceof DpdPdfConverter);
    }

    @Test
    void getOmnivaBarcode() {
        PdfConverter converter = PdfConverter.PdfConverterBuilder.aPdfConverter(new File("src/test/resources/omniva.pdf")).build();
        String barcode = converter.getBarcodeValue();
        converter.close();
        Assertions.assertEquals("CE482916786EE", barcode);
    }

    @Test
    void getDpdBarcode()  {
        PdfConverter converter = PdfConverter.PdfConverterBuilder.aPdfConverter(new File("src/test/resources/dpd.pdf")).build();
        String barcode = converter.getBarcodeValue();
        converter.close();
        Assertions.assertEquals("%001012005607114435847337233", barcode);
    }

    @Test
    void getSmartpostBarcode()  {
        PdfConverter converter = PdfConverter.PdfConverterBuilder.aPdfConverter(new File("src/test/resources/itella.pdf")).build();
        String barcode = converter.getBarcodeValue();
        converter.close();
        Assertions.assertEquals("8838100037479457", barcode);
    }

    @Test
    void npeWithDpd() throws IOException {
        Assertions.assertThrows(NullPointerException.class, () -> {
            PdfDocument doc;
            try(
                    InputStream is = new FileInputStream(new File("src/test/resources/dpd.pdf"))
            ) {
                doc = new PdfDocument();
                doc.loadFromStream(is);
            }
            doc.saveAsImage(0);
        });
    }
}
