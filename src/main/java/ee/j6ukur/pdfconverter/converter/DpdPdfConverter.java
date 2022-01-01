package ee.j6ukur.pdfconverter.converter;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class DpdPdfConverter extends PdfConverter {

    @Override
    protected String getBarcodeValue() {
        String rawValue = getPage().extractText(new Rectangle2D.Double(105, 405, 125, 15));
        String trimmed = rawValue.trim().replace(" ", "");
        String value = "%" + trimmed.substring(0, trimmed.length() - 1);
        if(value.length() != 28) {
            throw new IllegalStateException("Got invalid barcode from dpd pdf: '" + value + "'");
        }
        return value;
    }

    @Override
    protected Rectangle getCropRectangle() {
        return new Rectangle(582, 42, 500, 335);
    }

    @Override
    protected Rectangle getBarCodeCoordinates() {
        return new Rectangle(12, 6, 56, 178);
    }

    @Override
    protected PdfConverter.BarCodeAlgorithm getBarcodeAlogorithm() {
        return PdfConverter.BarCodeAlgorithm.A;
    }
}
