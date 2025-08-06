package com.github.dingjingmaster.tika.main.FileOperation;

import org.apache.tika.sax.ToXMLContentHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;
import org.xml.sax.*;

import java.io.BufferedWriter;
import java.io.IOException;

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
            StringBuilder bodyStr = new StringBuilder();

            doc.body().traverse(new NodeVisitor() {
                @Override
                public void head(Node node, int depth) {
                    if (node instanceof TextNode) {
                        bodyStr.append(((TextNode)node).text());
                    }
                    else if (node.nodeName().equals("br")
                        || node.nodeName().equals("p")
                        || node.nodeName().equals("div")) {
                        bodyStr.append("\n");
                    }
                }
                @Override
                public void tail (Node node, int depth) {
                    bodyStr.append("\n");
                }
            });

            String body = bodyStr.toString();
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
