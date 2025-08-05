package com.github.dingjingmaster.tika.main.FileOperation;

import org.apache.tika.sax.ToXMLContentHandler;
import org.apache.tika.sax.WriteOutContentHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

public class CleanBreaksOutputHandler extends ToXMLContentHandler {
    private final BufferedWriter writer;

    public CleanBreaksOutputHandler (BufferedWriter writer) {
        super();
        this.writer = writer;
    }

    @Override
    public void characters (char[] ch, int start, int length) throws SAXException {
        if (length <= 0) { return; }
        String text = new String(ch, start, length);
        text = text.trim();
        super.characters(text.toCharArray(), start, text.length());
    }

    @Override
    public void endDocument() throws SAXException {
        Document doc = Jsoup.parse(this.toString());
        try {
            String title = doc.title();
            title = title.trim();
            if (!title.isEmpty()) {
                this.writer.write(title);
                this.writer.newLine();
            }
            String body = doc.body().text();
            body = body.trim();
            if (!body.isEmpty()) {
                this.writer.write(body);
                this.writer.newLine();
            }
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }
}
