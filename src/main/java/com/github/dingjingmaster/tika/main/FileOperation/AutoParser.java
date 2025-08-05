package com.github.dingjingmaster.tika.main.FileOperation;

//import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.apache.poi.ss.formula.functions.T;
import org.apache.tika.detect.EncodingDetector;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
//import org.apache.tika.parser.RecursiveParserWrapper;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.ocr.TesseractOCRParser;
import org.apache.tika.parser.txt.CharsetDetector;
import org.apache.tika.parser.txt.UniversalEncodingDetector;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ToXMLContentHandler;
import org.apache.tika.sax.WriteOutContentHandler;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import com.github.dingjingmaster.tika.main.FileOperation.PaddleOCRParser;

public class AutoParser {
    public AutoParser() {
        System.setSecurityManager(null);
        System.setProperty("org.apache.tika.debug", "false");
        System.setProperty("tika.mimetypes", "org/apache/tika/mime/tika-mimetypes.xml");
    }

    public boolean parserFile(String filePath, String tmpDir) throws Exception {
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

        // 自动解析器
        Parser parser = new TikaAutoParser();

        // 元数据对象
        Metadata md = new Metadata();

        // 带上下文相关信息的ParseContext实例
        ParseContext ctx = new ParseContext();

        ctx.set(Parser.class, parser);
        ctx.set(TesseractOCRParser.class, new PaddleOCRParser());
//        ctx.set(EncodingDetector.class, new TikaEncodingDetector());
//        ctx.set(EmbeddedDocumentExtractor.class, new TikaEmbeddedDocumentExtractor());

        try (InputStream fi = new BufferedInputStream(new FileInputStream(filePath))) {
            if (null != ctxFile) {
                BufferedWriter writer = new BufferedWriter(new FileWriter(ctxFile));
                ToXMLContentHandler writeHandler = new CleanBreaksOutputHandler(writer);
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
                    System.out.println(e.toString());
                }
            }
        } catch (Exception e) {
            ret = false;
            System.out.println(e.toString());
        }

        return ret;
    }

    public static void main (String[] args) {
//        String file = "/home/dingjing/tk.csv";
//        String file = "/home/dingjing/andsec_3.2.14_amd64.deb";
//        String file = "/home/dingjing/TrayApp.zip";
//        String file = "/home/dingjing/aa.zip";
//        String file = "/home/dingjing/aa.docx";
//        String file = "/home/dingjing/Pictures/2025.png";
        String file = "/home/dingjing/Scan_1170800.log";
//        String file = "/home/dingjing/3thrd.config";
//        String file = "/home/dingjing/Pictures/vim.png";
        AutoParser ap = new AutoParser();

        try {
            if (!ap.parserFile(file, "/tmp/")) {
                System.out.println("parser file: '" + file + "' failed!");
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
}
