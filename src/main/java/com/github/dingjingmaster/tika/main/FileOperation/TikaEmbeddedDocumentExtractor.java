package com.github.dingjingmaster.tika.main.FileOperation;

import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.ContentHandler;

public class TikaEmbeddedDocumentExtractor implements EmbeddedDocumentExtractor {
    @Override
    public boolean shouldParseEmbedded(Metadata metadata) {
        // 是否继续递归处理
        return true;
    }

    @Override
    public void parseEmbedded(InputStream inputStream, org.xml.sax.ContentHandler contentHandler, Metadata metadata, boolean b) throws SAXException, IOException {
        System.out.println("===>");
    }
}
