package ee.j6ukur.pdfconverter.converter;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class SmartpostPdfConverter extends PdfConverter {

    @Override
    protected String getBarcodeValue() {
        String value = getPage().extractText(new Rectangle2D.Double(119d, 345d, 90d, 15d)).trim();
        if(value.length() != 16) {
            throw new IllegalStateException("Got invalid barcode from itella pdf: '" + value + "'");
        }
        return value;
    }

    @Override
    protected Rectangle getCropRectangle() {
        return new Rectangle(1790, 105, 1600, 1080);
    }

    @Override
    protected Rectangle getBarCodeCoordinates() {
        return new Rectangle(50, 6, 62, 178);
    }

    @Override
    protected PdfConverter.BarCodeAlgorithm getBarcodeAlogorithm() {
        return PdfConverter.BarCodeAlgorithm.C;
    }
}
