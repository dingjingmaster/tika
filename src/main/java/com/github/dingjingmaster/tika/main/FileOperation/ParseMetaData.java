package com.github.dingjingmaster.tika.main.FileOperation;


import com.github.dingjingmaster.tika.main.FileOperation.resource.TikaResource;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.DocumentSelector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.ToXMLContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.*;

public class ParseMetaData {
    public static Metadata parseMetaData(String filePath, String metaFile) {
        Metadata metadata = new Metadata();

        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filePath);

        final Logger logger = LoggerFactory.getLogger(ParseMetaData.class);
        try (InputStream inputStream = new FileInputStream(filePath)) {

            ParseContext context = new ParseContext();
            Parser parser = TikaResource.createParser();

            // 无须解析嵌入的文档
            System.out.println("1");
            context.set(DocumentSelector.class, metadata1 -> false);

            BufferedWriter writer = new BufferedWriter(new FileWriter(metaFile));
            ToXMLContentHandler writeHandler = new CleanBreaksOutputHandler(writer);

            TikaResource.parse(parser, logger, filePath, inputStream, writeHandler, metadata, context);

        }
        catch (IOException e) {
            logger.error("Error parsing metadata from file: {}", filePath, e);
        }
        return metadata;
    }
}