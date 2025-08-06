package com.github.dingjingmaster.tika.main.FileOperation;

import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.formula.functions.T;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.config.TikaTaskTimeout;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.ParentContentHandler;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.CompositeParser;
import org.apache.tika.parser.DefaultParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.ocr.TesseractOCRParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.EmbeddedContentHandler;
import org.apache.tika.sax.TeeContentHandler;
import org.apache.tika.sax.XHTMLContentHandler;
import org.apache.tika.utils.StringUtils;
import org.apache.tika.utils.XMLReaderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.tika.sax.XHTMLContentHandler.XHTML;


public class TikaOCRParser extends TesseractOCRParser {
    private final String tesseractBase = "/usr/share/tessdata/";
    private final String tesseractCmd = "/usr/bin/tesseract";
    private final String magicCmd = "/usr/bin/magick";
    private final TikaImagePreprocessor imagePreprocessor = new TikaImagePreprocessor(magicCmd);
    private final Logger logger = LoggerFactory.getLogger(TikaOCRParser.class);
    TikaOCRParser() {
        super();
    }

    @Override
    public Set<MediaType> getSupportedTypes (ParseContext ctx) {
        return Set.of(
                MediaType.image("png")
        );
    }

    @Override
    public boolean hasTesseract() {
        if (!Files.exists(Paths.get(tesseractCmd), LinkOption.NOFOLLOW_LINKS)) {
            logger.warn("tesseract is not exists!");
            return false;
        }

        logger.info("Found tesseract: {}", tesseractCmd);

        return true;
    }

    @Override
    public String getTessdataPath() {
        return tesseractBase;
    }

    private String getTesseractCmd() {
        return tesseractCmd;
    }

    public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext parseContext) {
        try (TemporaryResources tmp = new TemporaryResources()) {
            TikaInputStream tikaStream = TikaInputStream.get(stream, tmp, metadata);
            tikaStream.getPath();
            File tmpOCROutputFile = tmp.createTemporaryFile();
            ContentHandler baseHandler = this.getContentHandler(handler, metadata, parseContext);
            XHTMLContentHandler xhtml = new XHTMLContentHandler(baseHandler, metadata);
            xhtml.startDocument();
            this.parse(tikaStream, tmpOCROutputFile, xhtml, metadata, parseContext);
            xhtml.endDocument();
        }
        catch (Exception e) {
            logger.warn("OCR parse error: {}", e.toString());
        }
    }

    private ContentHandler getContentHandler(ContentHandler handler, Metadata metadata, ParseContext parseContext) {
        ParentContentHandler parentContentHandler = (ParentContentHandler)parseContext.get(ParentContentHandler.class);
        if (parentContentHandler == null) {
            return handler;
        }
        else {
            String embeddedType = metadata.get(TikaCoreProperties.EMBEDDED_RESOURCE_TYPE);
            return (ContentHandler)(!TikaCoreProperties.EmbeddedResourceType.INLINE.name().equals(embeddedType) ? handler : new TeeContentHandler(new ContentHandler[]{new EmbeddedContentHandler(new BodyContentHandler(parentContentHandler.getContentHandler())), handler}));
        }
    }

    private void parse(TikaInputStream tikaInputStream, File tmpOCROutputFile, ContentHandler xhtml, Metadata metadata, ParseContext parseContext)
            throws IOException, SAXException, TikaException {

        try {
            Path input = tikaInputStream.getPath();
            doOCR(input.toFile(), tmpOCROutputFile, parseContext);
            if (tmpOCROutputFile.exists()) {
                try (InputStream is = new FileInputStream(tmpOCROutputFile)) {
                    extractOutput(is, xhtml);
                }
            }

            try (TemporaryResources tmp = new TemporaryResources()) {
                File metaInfo = tmp.createTemporaryFile();
                this.imagePreprocessor.process(input, metaInfo.toPath(), metadata);
                InputStream is = new FileInputStream(metaInfo);
                extractOSD(is, metadata);
                extractHOCROutput(is, parseContext, xhtml);
                Files.delete(metaInfo.toPath());
            } catch (Exception e) {
                logger.warn("magic error: {}", e.toString());
            }
            finally {
            }
        }
        finally {
        }
    }

    private void doOCR(File input, File output, ParseContext parseContext) throws IOException, TikaException {
        String[] var10002 = new String[5];
        var10002[0] = getTesseractCmd();
        var10002[1] = input.getPath();
        var10002[2] = output.getPath();
        var10002[3] = "--psm";
        var10002[4] = "1";
        ArrayList<String> cmd = new ArrayList(Arrays.asList(var10002));
        cmd.add("-l");
        cmd.add("osd+eng+chi_sim+chi_tra");

        String[] var16 = new String[]{"-c", null, null};
        var16[1] = "preserve_interword_spaces=0";
        var16[2] = "txt";
        cmd.addAll(Arrays.asList(var16));

        logger.info("Tesseract command: {}", String.join(" ", cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        this.setEnv(pb);
        Process process = null;
        String id = null;
        long timeoutMillis = TikaTaskTimeout.getTimeoutMillis(parseContext, (long)(10 * 60 * 1000));

        try {
            process = pb.start();
            id = this.register(process);
            this.runOCRProcess(process, timeoutMillis);
        }
        finally {
            if (process != null) {
                process.destroyForcibly();
            }
            if (id != null) {
                this.release(id);
            }
        }
        File txt = new File(output.getPath() + ".txt");
        if (txt.exists()) {
            Files.move(txt.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void setEnv(ProcessBuilder pb) {
        Map<String, String> env = pb.environment();

        if (!StringUtils.isBlank(getTessdataPath())) {
            env.put("TESSDATA_PREFIX", getTessdataPath());
        }
    }
    private void runOCRProcess(Process process, long timeoutMillis) throws IOException, TikaException {
        process.getOutputStream().close();
        InputStream out = process.getInputStream();
        InputStream err = process.getErrorStream();
        StringBuilder outBuilder = new StringBuilder();
        StringBuilder errBuilder = new StringBuilder();
        Thread outThread = this.logStream(out, outBuilder);
        Thread errThread = this.logStream(err, errBuilder);
        outThread.start();
        errThread.start();
        int exitValue = Integer.MIN_VALUE;

        try {
            boolean finished = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS);
            if (!finished) {
                throw new TikaException("TesseractOCRParser timeout");
            }

            exitValue = process.exitValue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TikaException("TesseractOCRParser interrupted", e);
        } catch (IllegalThreadStateException var14) {
            throw new TikaException("TesseractOCRParser timeout");
        }

        if (exitValue > 0) {
            try {
                errThread.join(1000L);
            }
            catch (InterruptedException var12) {
                logger.warn("proc error: {}", var12.toString());
            }

            throw new TikaException("TesseractOCRParser bad exit value " + exitValue + " err msg: " + errBuilder.toString());
        }
    }

    private Thread logStream(final InputStream stream, final StringBuilder out) {
        return new Thread(() -> {
            Reader reader = new InputStreamReader(stream, UTF_8);
            char[] buffer = new char[1024];
            try {
                for (int n = reader.read(buffer); n != -1; n = reader.read(buffer)) {
                    out.append(buffer, 0, n);
                }
            }
            catch (IOException e) {
                //swallow
            }
            finally {
                IOUtils.closeQuietly(stream);
            }
        });
    }
    private void extractOutput(InputStream stream, ContentHandler xhtml)
            throws SAXException, IOException {
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "class", "class", "CDATA", "ocr");
        xhtml.startElement(XHTML, "div", "div", attrs);
        try (Reader reader = new InputStreamReader(stream, UTF_8)) {
            char[] buffer = new char[1024];
            for (int n = reader.read(buffer); n != -1; n = reader.read(buffer)) {
                if (n > 0) {
                    xhtml.characters(buffer, 0, n);
                }
            }
        }
        xhtml.endElement(XHTML, "div", "div");
    }

    private void extractOSD(InputStream is, Metadata metadata) throws IOException {
        Matcher matcher = Pattern.compile("^([^:]+):\\s+(.*)").matcher("");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, UTF_8))) {
            String line = reader.readLine();
            while (line != null) {
                if (matcher.reset(line).find()) {
                    String k = matcher.group(1);
                    String v = matcher.group(2);

                    k = k.trim();
                    v = v.trim();

                    switch (k) {
                        case "Page number":
                            metadata.set(PSM0_PAGE_NUMBER, Integer.parseInt(v));
                            break;
                        case "Orientation in degrees":
                            metadata.set(PSM0_ORIENTATION, Integer.parseInt(v));
                            break;
                        case "Rotate":
                            metadata.set(PSM0_ROTATE, Integer.parseInt(v));
                            break;
                        case "Orientation confidence":
                            metadata.set(PSM0_ORIENTATION_CONFIDENCE, Double.parseDouble(v));
                            break;
                        case "Script":
                            metadata.set(PSM0_SCRIPT, v);
                            break;
                        case "Script confidence":
                            metadata.set(PSM0_SCRIPT_CONFIDENCE, Double.parseDouble(v));
                            break;
                        case "Mime type":
                            metadata.set("Content-Type", v);
                            break;
                        case "Colorspace":
                            metadata.set("Color-Space", v);
                            break;
                        case "Depth":
                            metadata.set("Depth", v);
                            break;
                        case "Software":
                            metadata.set("Software", v);
                            break;
                        case "Filename":
                            metadata.set("FileName", v);
                            break;
                        default:
//                            logger.warn("I regret I don't know how to parse {} with value {}", k, v);
                            break;
                    }
                }
                line = reader.readLine();
            }
        }
    }

    private static class HOCRPassThroughHandler extends DefaultHandler {
        public static final Set<String> IGNORE =
                unmodifiableSet("html", "head", "title", "meta", "body");
        private final ContentHandler xhtml;

        public HOCRPassThroughHandler(ContentHandler xhtml) {
            this.xhtml = xhtml;
        }

        private static Set<String> unmodifiableSet(String... elements) {
            return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(elements)));
        }

        /**
         * Starts the given element. Table cells and list items are automatically
         * indented by emitting a tab character as ignorable whitespace.
         */
        @Override
        public void startElement(String uri, String local, String name, Attributes attributes)
                throws SAXException {
            if (!IGNORE.contains(name)) {
                xhtml.startElement(uri, local, name, attributes);
            }
        }

        /**
         * Ends the given element. Block elements are automatically followed
         * by a newline character.
         */
        @Override
        public void endElement(String uri, String local, String name) throws SAXException {
            if (!IGNORE.contains(name)) {
                xhtml.endElement(uri, local, name);
            }
        }

        /**
         * @see <a href="https://issues.apache.org/jira/browse/TIKA-210">TIKA-210</a>
         */
        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            xhtml.characters(ch, start, length);
        }
    }

    private void extractHOCROutput(InputStream is, ParseContext parseContext, ContentHandler xhtml)
            throws TikaException, IOException, SAXException {
        if (parseContext == null) {
            parseContext = new ParseContext();
        }

        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "class", "class", "CDATA", "ocr");
        xhtml.startElement(XHTML, "div", "div", attrs);
        XMLReaderUtils.parseSAX(is, new HOCRPassThroughHandler(xhtml), parseContext);
        xhtml.endElement(XHTML, "div", "div");
    }
}

