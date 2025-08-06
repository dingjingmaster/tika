package com.github.dingjingmaster.tika.main.FileOperation;

import java.awt.*;
import java.io.InputStream;
import java.util.Set;

import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ocr.TesseractOCRParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.XHTMLContentHandler;


public class PaddleOCRParser extends TesseractOCRParser {
    private static final Logger log = LoggerFactory.getLogger(PaddleOCRParser.class);

    PaddleOCRParser() {
        super();
    }

    @Override
    public Set<MediaType> getSupportedTypes(ParseContext context) {
        return supportedTypes;
    }

    @Override
    public void parse(InputStream stream, ContentHandler handler, Metadata metaData, ParseContext context) {
        System.out.println("[OCR]");
        try {
            log.info("[OCR] start parse");
            String ocrText = callMyOCR(stream);
            XHTMLContentHandler xhtml = new XHTMLContentHandler(handler, metaData);
            xhtml.startDocument();
            xhtml.element("p", ocrText);
            xhtml.endDocument();
        }
        catch (Exception e) {
            System.out.println("Parse OCR error!");
        }
    }

    private String callMyOCR(InputStream imageStream) {
        // OCR 结果
        System.out.println("--->KKK");
        return "kkkk";
    }

    private static final Set<MediaType> supportedTypes = Set.of(
            MediaType.image("png"),
            MediaType.image("jpg"),
            MediaType.image("bmp"),
            MediaType.image("jpeg")
    );
}
