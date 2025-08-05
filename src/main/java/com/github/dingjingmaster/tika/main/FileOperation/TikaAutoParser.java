package com.github.dingjingmaster.tika.main.FileOperation;

import org.apache.tika.parser.csv.TextAndCSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.tika.detect.EncodingDetector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.txt.CharsetDetector;

import java.io.IOException;
import java.io.InputStream;

public class TikaAutoParser extends AutoDetectParser {
    private final Logger logger = LoggerFactory.getLogger(TikaAutoParser.class);

    TikaAutoParser() {
        super();
    }

    @Override
    public void parse(InputStream stream, org.xml.sax.ContentHandler handler, Metadata metaData, ParseContext ctx)
            throws IOException, org.xml.sax.SAXException, org.apache.tika.exception.TikaException {
        Parser parser = getParser(stream, handler, metaData, ctx);
        parser.parse(stream, handler, metaData, ctx);
    }

    private Parser getParser(InputStream stream, org.xml.sax.ContentHandler handler, Metadata metadata, ParseContext ctx)
            throws IOException {
        MediaType type = getDetector().detect(stream, metadata);
        Parser parser = getParsers().get(type);
        if (null != parser) {
            logger.info("MediaType: {}, parser: {}", type, parser.getClass().getName());
            return parser;
        }

        // 检测 parser
        CharsetDetector detector = new CharsetDetector();
        detector.setText(stream);
        String charsetName = detector.detect().getName();
        stream.reset();
        if (null != charsetName) {
            metadata.set(Metadata.CONTENT_ENCODING, charsetName);
            MediaType gussType = MediaType.TEXT_PLAIN;
            parser = new TextAndCSVParser();
        }
        if (null != parser) {
            logger.info("MediaType: {}, charset: {}, parser: {}", type, charsetName, parser.getClass().getName());
            return parser;
        }

        // 最后实在没有找到合适的 parser
        parser = getFallback();
        logger.info("MediaType: {}, parser: {}", type, parser.getClass().getName());

        return parser;
    }
}
