package ee.j6ukur.pdfconverter.controller;

import ee.j6ukur.pdfconverter.converter.PdfConverter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
public class ConvertController {

    @RequestMapping(value = "/convert", method = RequestMethod.GET)
    public void convert(HttpServletResponse response,
                        @RequestParam("url") String url,
                        @RequestParam(value = "row", required = false, defaultValue = "1") Integer row,
                        @RequestParam(value = "col", required = false, defaultValue = "1") Integer col,
                        @RequestParam(value = "lines", required = false, defaultValue = "false") boolean lines,
                        @RequestParam(value = "all", required = false, defaultValue = "false") boolean all) throws IOException, InterruptedException {
        response.setContentType("application/pdf");
//        response.setContentType("image/png");
        PdfConverter.PdfConverterBuilder.aPdfConverter().withUrl(url).withOut(response.getOutputStream()).withRow(row).withCol(col).withAll(all).withLines(lines).build().render();
    }
}
