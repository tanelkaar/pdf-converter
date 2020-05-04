package ee.j6ukur.pdfconverter.controller;

import ee.j6ukur.pdfconverter.converter.PdfConverter;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Controller
public class ConvertController {

    @Get(uri = "/convert", produces = "application/pdf")
    public byte[] convert(@QueryValue("url") String url,
                          @QueryValue(value = "row", defaultValue = "1") Integer row,
                          @QueryValue(value = "col", defaultValue = "1") Integer col,
                          @QueryValue(value = "lines", defaultValue = "false") boolean lines,
                          @QueryValue(value = "all", defaultValue = "false") boolean all) throws IOException, InterruptedException {

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PdfConverter.PdfConverterBuilder.aPdfConverter().withUrl(url).withOut(os).withRow(row).withCol(col).withAll(all).withLines(lines).build().render();
        return os.toByteArray();
    }
}
