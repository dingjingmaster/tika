package com.github.dingjingmaster.tika.main.FileOperation;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.RecursiveParserWrapper;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.ocr.TesseractOCRParser;
import org.apache.tika.sax.BasicContentHandlerFactory;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.RecursiveParserWrapperHandler;
import org.apache.tika.sax.ToXMLContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class AutoParser {
    private final Logger logger = LoggerFactory.getLogger(AutoParser.class);
    public AutoParser() {
        System.setSecurityManager(null);
        System.setProperty("org.apache.tika.debug", "false");
        System.setProperty("org.apache.tika.parser.ocr.debug", "false");
        System.setProperty("tika.mimetypes", "org/apache/tika/mime/tika-mimetypes.xml");
    }

    public boolean parserFile(String filePath, String tmpDir) {
        boolean ret = true;

        String ctxFile = null;
        String metaFile = null;

        if (!tmpDir.isEmpty()) {
            File df = new File(tmpDir);
            df.mkdirs();
            if (df.exists()) {
                ctxFile = tmpDir + "/ctx.txt";
                metaFile = tmpDir + "/meta.txt";
            }
        }
        Parser autoParser = null;
        TikaConfig tikaConfig = null;
        RecursiveParserWrapper parser = null;

        try {
            tikaConfig = new TikaConfig(getClass().getClassLoader().getResourceAsStream("tika-config.xml"));
            // 自动解析器
            autoParser = new TikaAutoParser(tikaConfig);

            parser = new RecursiveParserWrapper(autoParser);
        }
        catch (Exception e) {
            logger.warn("TikaConfig error: {}", e.toString());
        }

        if (parser == null) {
            return false;
        }

        // 元数据对象
        Metadata md = new Metadata();

        // 带上下文相关信息的ParseContext实例
        ParseContext ctx = new ParseContext();

        TesseractOCRConfig tessConfig = new TesseractOCRConfig();
        tessConfig.setLanguage("eng+chi_sim");
        tessConfig.setTimeoutSeconds(60 * 10);
        tessConfig.setEnableImagePreprocessing(true);

        ctx.set(Parser.class, parser);
        ctx.set(TesseractOCRConfig.class, tessConfig);

        try (InputStream fi = new BufferedInputStream(new FileInputStream(filePath))) {
            if (null != ctxFile) {
                BufferedWriter writer = new BufferedWriter(new FileWriter(ctxFile));
//                ToXMLContentHandler writeHandler = new CleanBreaksOutputHandler(writer);
                RecursiveParserWrapperHandler writeHandler = new RecursiveParserWrapperHandler(new BasicContentHandlerFactory(BasicContentHandlerFactory.HANDLER_TYPE.TEXT, -1));

                parser.parse(fi, writeHandler, md, ctx);
                writer.close();
            }
            else {
                BodyContentHandler memHandler = new BodyContentHandler(-1);
                parser.parse(fi, memHandler, md, ctx);
                System.out.println("File content: " + memHandler);
            }

            if (null != metaFile) {
                try (FileOutputStream fw = new FileOutputStream(metaFile)) {
                    String[] names = md.names();
                    for (String name : names) {
                        String lineBuf = name + "{]" + md.get(name) + "\n";
                        fw.write(lineBuf.getBytes(StandardCharsets.UTF_8));
                    }
                } catch (Exception e) {
                    ret = false;
                    logger.warn("Write meta info error: {}", e.toString());
                }
            }
        } catch (Exception e) {
            ret = false;
            logger.warn("Read file {} error: {}", filePath, e.toString());
        }

        return ret;
    }

    public static void main (String[] args) {
//        String file = "/home/dingjing/tk.csv";
//        String file = "/home/dingjing/andsec_6.0.8_amd64.deb";
        String file = "/home/dingjing/IDA Pro 9.1.zip";
//        String file = "/home/dingjing/aa.zip";
//        String file = "/home/dingjing/aa.docx";
//        String file = "/home/dingjing/Pictures/2025.png";
//        String file = "/home/dingjing/Scan_1170800.log";
//        String file = "/home/dingjing/3thrd.config";
//        String file = "/home/dingjing/Pictures/vim.png";
        AutoParser ap = new AutoParser();

        try {
            if (!ap.parserFile(file, "/tmp/")) {
                System.out.println("parser file: '" + file + "' failed!");
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
