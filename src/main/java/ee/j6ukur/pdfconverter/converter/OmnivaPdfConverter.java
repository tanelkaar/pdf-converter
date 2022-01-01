package ee.j6ukur.pdfconverter.converter;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class OmnivaPdfConverter extends PdfConverter {

    @Override
    protected String getBarcodeValue() {
        String value = getPage().extractText(new Rectangle2D.Double(124d, 385d, 70d, 10d)).trim();
        if(value.length() != 13) {
            throw new IllegalStateException("Got invalid barcode from omniva pdf: '" + value + "'");
        }
        return value;
    }

    @Override
    protected Rectangle getCropRectangle() {
        return new Rectangle(1870, 140, 1500, 1030);
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
