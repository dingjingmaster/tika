package com.github.dingjingmaster.tika.main.FileOperation.resource;

import static java.nio.charset.StandardCharsets.UTF_8;
//import static com.github.dingjingmaster.tika.main.FileOperation.resource.RecursiveMetadataResource.DEFAULT_HANDLER_TYPE;
//import static com.github.dingjingmaster.tika.main.FileOperation.resource.RecursiveMetadataResource.HANDLER_TYPE_PARAM;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import com.github.dingjingmaster.tika.main.FileOperation.*;
import com.github.dingjingmaster.tika.main.FileOperation.others.MultivaluedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.mime.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.config.TikaTaskTimeout;
import org.apache.tika.exception.EncryptedDocumentException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.DigestingParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.pipes.HandlerConfig;
import org.apache.tika.sax.BasicContentHandlerFactory;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ExpandedTitleContentHandler;
import org.apache.tika.sax.RichTextContentHandler;
import org.apache.tika.sax.boilerpipe.BoilerpipeContentHandler;

import org.apache.tika.utils.ExceptionUtils;

// 这是主要调用
//@Path("/tika")
public class TikaResource {

    // DJ-
    private String mFileName;
    private final Logger logger = LoggerFactory.getLogger(TikaResource.class);

    public static final String GREETING = "This is Tika Server (" + Tika.getString() + "). Please PUT\n";
    private static final String META_PREFIX = "meta_";
    private static final Logger LOG = LoggerFactory.getLogger(TikaResource.class);
    private static Pattern ALLOWABLE_HEADER_CHARS = Pattern.compile("(?i)^[-/_+\\.A-Z0-9 ]+$");
    private static TikaConfig TIKA_CONFIG;
    private static TikaServerConfig TIKA_SERVER_CONFIG;
    private static DigestingParser.Digester DIGESTER = null;
    private static InputStreamFactory INPUTSTREAM_FACTORY = null;
    private static ServerStatus SERVER_STATUS = null;

    private static ParseContextConfig PARSE_CONTEXT_CONFIG = new CompositeParseContextConfig();


    public static void init(TikaConfig config, DigestingParser.Digester digester, InputStreamFactory inputStreamFactory) {
        TIKA_CONFIG = config;
        DIGESTER = digester;
        INPUTSTREAM_FACTORY = inputStreamFactory;
    }


    //@SuppressWarnings("serial")
    public static Parser createParser() {
        final Parser parser = new AutoDetectParser(TIKA_CONFIG);
        if (DIGESTER != null) {
            boolean skipContainer = false;
            if (TIKA_CONFIG
                    .getAutoDetectParserConfig()
                    .getDigesterFactory() != null && TIKA_CONFIG
                    .getAutoDetectParserConfig()
                    .getDigesterFactory()
                    .isSkipContainerDocument()) {
                skipContainer = true;
            }
            return new DigestingParser(parser, DIGESTER, skipContainer);
        }
        return parser;
    }

    public static TikaConfig getConfig() {
        return TIKA_CONFIG;
    }


    public static void fillParseContext(MultivaluedMap<String, String> httpHeaders, Metadata metadata, ParseContext parseContext) {
        PARSE_CONTEXT_CONFIG.configure(metadata, parseContext);
    }

    public InputStream getInputStream(InputStream is, Metadata metadata) {
        try {
            return INPUTSTREAM_FACTORY.getInputStream(is, metadata);
        } catch (IOException e) {
            logger.warn("getInputStream error: {}", e.toString());
        }
        return null;
    }

    /**
     * Utility method to set a property on a class via reflection.
     *
     * @param object the <code>Object</code> to set the property on.
     * @param key    the key of the HTTP Header.
     * @param val    the value of HTTP header.
     * @param prefix the name of the HTTP Header prefix used to find property.
     */
    public void processHeaderConfig(Object object, String key, String val, String prefix) {
        try {
            String property = StringUtils.removeStartIgnoreCase(key, prefix);
            Field field = null;
            try {
                field = object
                        .getClass()
                        .getDeclaredField(StringUtils.uncapitalize(property));
            } catch (NoSuchFieldException e) {
                // try to match field case-insensitive way
                for (Field aField : object
                        .getClass()
                        .getDeclaredFields()) {
                    if (aField
                            .getName()
                            .equalsIgnoreCase(property)) {
                        field = aField;
                        break;
                    }
                }
            }
            String setter = field != null ? field.getName() : property;
            setter = "set" + setter
                    .substring(0, 1)
                    .toUpperCase(Locale.US) + setter.substring(1);
            //default assume string class
            //if there's a more specific type, e.g. double, int, boolean
            //try that.
            Class clazz = String.class;
            if (field != null) {
                if (field.getType() == int.class || field.getType() == Integer.class) {
                    clazz = int.class;
                } else if (field.getType() == double.class) {
                    clazz = double.class;
                } else if (field.getType() == Double.class) {
                    clazz = Double.class;
                } else if (field.getType() == float.class) {
                    clazz = float.class;
                } else if (field.getType() == Float.class) {
                    clazz = Float.class;
                } else if (field.getType() == boolean.class) {
                    clazz = boolean.class;
                } else if (field.getType() == Boolean.class) {
                    clazz = Boolean.class;
                } else if (field.getType() == long.class) {
                    clazz = long.class;
                } else if (field.getType() == Long.class) {
                    clazz = Long.class;
                }
            }

            Method m = tryToGetMethod(object, setter, clazz);
            //if you couldn't find more specific setter, back off
            //to string setter and try that.
            if (m == null && clazz != String.class) {
                m = tryToGetMethod(object, setter, String.class);
            }

            if (m != null) {
                if (clazz == String.class) {
                    checkTrustWorthy(setter, val);
                    m.invoke(object, val);
                } else if (clazz == int.class || clazz == Integer.class) {
                    m.invoke(object, Integer.parseInt(val));
                } else if (clazz == double.class || clazz == Double.class) {
                    m.invoke(object, Double.parseDouble(val));
                } else if (clazz == boolean.class || clazz == Boolean.class) {
                    m.invoke(object, Boolean.parseBoolean(val));
                } else if (clazz == float.class || clazz == Float.class) {
                    m.invoke(object, Float.parseFloat(val));
                } else if (clazz == long.class || clazz == Long.class) {
                    m.invoke(object, Long.parseLong(val));
                } else {
                    throw new IllegalArgumentException("setter must be String, int, float, double or boolean...for now");
                }
            } else {
                throw new NoSuchMethodException("Couldn't find: " + setter);
            }

        } catch (Throwable ex) {
            // TIKA-3345
            String error = (!(ex.getCause() instanceof IllegalArgumentException)) ? String.format(Locale.ROOT, "%s is an invalid %s header", key, prefix) :
                    String.format(Locale.ROOT, "%s is an invalid %s header value", val, key);
//            throw new WebApplicationException(error, Response.Status.BAD_REQUEST);
            logger.warn("error: {}", ex.toString());
        }
    }

    private static void checkTrustWorthy(String setter, String val) {
        if (setter == null || val == null) {
            throw new IllegalArgumentException("setter and val must not be null");
        }
        if (setter
                .toLowerCase(Locale.US)
                .contains("trusted")) {
            throw new IllegalArgumentException("Can't call a trusted method via tika-server headers");
        }
        Matcher m = ALLOWABLE_HEADER_CHARS.matcher(val);
        if (!m.find()) {
            throw new IllegalArgumentException("Header val: " + val + " contains illegal characters. " + "Must contain: TikaResource.ALLOWABLE_HEADER_CHARS");
        }
    }

    /**
     * Tries to get method. Silently swallows NoMethodException and returns
     * <code>null</code> if not found.
     *
     * @param object the object to get method from.
     * @param method the name of the method to get.
     * @param clazz  the parameter type of the method to get.
     * @return the found method instance
     */
    private static Method tryToGetMethod(Object object, String method, Class clazz) {
        try {
            return object
                    .getClass()
                    .getMethod(method, clazz);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

//    @SuppressWarnings("serial")
    public void fillMetadata(Parser parser, Metadata metadata) {
        String fileName = mFileName;
        if (fileName != null) {
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
        }

        MediaType mediaType = null;
    }

    public static void parse(Parser parser, Logger logger, String path, InputStream inputStream, ContentHandler handler, Metadata metadata, ParseContext parseContext) {
        String fileName = metadata.get(TikaCoreProperties.RESOURCE_NAME_KEY);
        long timeoutMillis = getTaskTimeout(parseContext);

        long taskId = SERVER_STATUS.start(ServerStatus.TASK.PARSE, fileName, timeoutMillis);
        try {
            parser.parse(inputStream, handler, metadata, parseContext);
        }
        catch (SAXException e) {
            logger.warn("Parse error: {}", e.toString());
        }
        catch (EncryptedDocumentException e) {
            logger.warn("{}: Encrypted document ({})", path, fileName, e);
        }
        catch (Exception e) {
            if (!WriteLimitReachedException.isWriteLimitReached(e)) {
                logger.warn("{}: Text extraction failed ({})", path, fileName, e);
            }
        }
        catch (OutOfMemoryError e) {
            logger.warn("{}: OOM ({})", path, fileName, e);
            SERVER_STATUS.setStatus(ServerStatus.STATUS.ERROR);
        }
        finally {
            SERVER_STATUS.complete(taskId);
            try {
                inputStream.close();
            }
            catch (Exception e) {
                logger.warn("input stream close error: {}", e.toString());
            }
        }
    }

    protected static long getTaskTimeout(ParseContext parseContext) {

        TikaTaskTimeout tikaTaskTimeout = parseContext.get(TikaTaskTimeout.class);
        long timeoutMillis = TIKA_SERVER_CONFIG.getTaskTimeoutMillis();

        if (tikaTaskTimeout != null) {
            if (tikaTaskTimeout.getTimeoutMillis() > TIKA_SERVER_CONFIG.getTaskTimeoutMillis()) {
                throw new IllegalArgumentException(
                        "Can't request a timeout ( " + tikaTaskTimeout.getTimeoutMillis() + "ms) greater than the taskTimeoutMillis set in the server config (" +
                                TIKA_SERVER_CONFIG.getTaskTimeoutMillis() + "ms)");
            }
            timeoutMillis = tikaTaskTimeout.getTimeoutMillis();
//            if (timeoutMillis < TIKA_SERVER_CONFIG.getMinimumTimeoutMillis()) {
//                throw new WebApplicationException(new IllegalArgumentException(
//                        "taskTimeoutMillis must be > " + "minimumTimeoutMillis, currently set to (" + TIKA_SERVER_CONFIG.getMinimumTimeoutMillis() + "ms)"),
//                        Response.Status.BAD_REQUEST);
//            }
        }
        return timeoutMillis;
    }

    public static void checkIsOperating() {
        //check that server is not in shutdown mode
    }

    public static void logRequest(Logger logger, String endpoint, Metadata metadata) {

        if (metadata.get(org.apache.tika.metadata.HttpHeaders.CONTENT_TYPE) == null) {
            logger.info("{} (autodetecting type)", endpoint);
        } else {
            logger.info("{} ({})", endpoint, metadata.get(org.apache.tika.metadata.HttpHeaders.CONTENT_TYPE));
        }
    }

    public static boolean getThrowOnWriteLimitReached(MultivaluedMap<String, String> httpHeaders) {
        if (httpHeaders.containsKey("throwOnWriteLimitReached")) {
            String val = httpHeaders.getFirst("throwOnWriteLimitReached");
            if ("true".equalsIgnoreCase(val)) {
                return true;
            } else if ("false".equalsIgnoreCase(val)) {
                return false;
            } else {
                throw new IllegalArgumentException("'throwOnWriteLimitReached' must be either 'true' or 'false'");
            }
        }
        return HandlerConfig.DEFAULT_HANDLER_CONFIG.isThrowOnWriteLimitReached();
    }

//    @GET
//    @Produces("text/plain")
    public String getMessage() {
        checkIsOperating();
        return GREETING;
    }

//    @POST
//    @Consumes("multipart/form-data")
//    @Produces("text/plain")
//    @Path("form")
//    public StreamingOutput getTextFromMultipart(Attachment att, @Context HttpHeaders httpHeaders, @Context final UriInfo info) {
//        return produceText(att.getObject(InputStream.class), new Metadata(), preparePostHeaderMap(att, httpHeaders), info);
//    }

    //this is equivalent to text-main in tika-app
//    @PUT
//    @Consumes("*/*")
//    @Produces("text/plain")
//    @Path("main")
//    public StreamingOutput getTextMain(final InputStream is, @Context HttpHeaders httpHeaders, @Context final UriInfo info) {
//        return produceTextMain(is, httpHeaders.getRequestHeaders(), info);
//    }

    //this is equivalent to text-main (Boilerpipe handler) in tika-app
//    @POST
//    @Consumes("multipart/form-data")
//    @Produces("text/plain")
//    @Path("form/main")
//    public StreamingOutput getTextMainFromMultipart(final Attachment att, @Context HttpHeaders httpHeaders, @Context final UriInfo info) {
//        return produceTextMain(att.getObject(InputStream.class), preparePostHeaderMap(att, httpHeaders), info);
//    }

//    public StreamingOutput produceTextMain(final InputStream is, MultivaluedMap<String, String> httpHeaders, final UriInfo info) {
//        final Parser parser = createParser();
//        final Metadata metadata = new Metadata();
//        final ParseContext context = new ParseContext();
//
//        fillMetadata(parser, metadata, httpHeaders);
//        fillParseContext(httpHeaders, metadata, context);
//
//        logRequest(LOG, "/tika", metadata);
//
//        return outputStream -> {
//            Writer writer = new OutputStreamWriter(outputStream, UTF_8);
//
//            ContentHandler handler = new BoilerpipeContentHandler(writer);
//
//            parse(parser, LOG, info.getPath(), is, handler, metadata, context);
//        };
//    }

//    @PUT
//    @Consumes("*/*")
//    @Produces("text/plain")
//    public StreamingOutput getText(final InputStream is, @Context HttpHeaders httpHeaders, @Context final UriInfo info) {
//        final Metadata metadata = new Metadata();
//        return produceText(getInputStream(is, metadata, httpHeaders, info), metadata, httpHeaders.getRequestHeaders(), info);
//    }
//
//    public StreamingOutput produceText(final InputStream is, final Metadata metadata, MultivaluedMap<String, String> httpHeaders, final UriInfo info) {
//        final Parser parser = createParser();
//        final ParseContext context = new ParseContext();
//
//        fillMetadata(parser, metadata, httpHeaders);
//        fillParseContext(httpHeaders, metadata, context);
//
//        logRequest(LOG, "/tika", metadata);
//
//        return outputStream -> {
//            Writer writer = new OutputStreamWriter(outputStream, UTF_8);
//
//            BodyContentHandler body = new BodyContentHandler(new RichTextContentHandler(writer));
//
//            parse(parser, LOG, info.getPath(), is, body, metadata, context);
//        };
//    }

//    @POST
//    @Consumes("multipart/form-data")
//    @Produces("text/html")
//    @Path("form")
//    public StreamingOutput getHTMLFromMultipart(Attachment att, @Context HttpHeaders httpHeaders, @Context final UriInfo info) {
//        return produceOutput(att.getObject(InputStream.class), new Metadata(), preparePostHeaderMap(att, httpHeaders), info, "html");
//    }

//    @PUT
//    @Consumes("*/*")
//    @Produces("text/html")
//    public StreamingOutput getHTML(final InputStream is, @Context HttpHeaders httpHeaders, @Context final UriInfo info) {
//        Metadata metadata = new Metadata();
//        return produceOutput(getInputStream(is, metadata, httpHeaders, info), metadata, httpHeaders.getRequestHeaders(), info, "html");
//    }

//    @POST
//    @Consumes("multipart/form-data")
//    @Produces("text/xml")
//    @Path("form")
//    public StreamingOutput getXMLFromMultipart(Attachment att, @Context HttpHeaders httpHeaders, @Context final UriInfo info) {
//        return produceOutput(att.getObject(InputStream.class), new Metadata(), preparePostHeaderMap(att, httpHeaders), info, "xml");
//    }

//    @PUT
//    @Consumes("*/*")
//    @Produces("text/xml")
//    public StreamingOutput getXML(final InputStream is, @Context HttpHeaders httpHeaders, @Context final UriInfo info) {
//        Metadata metadata = new Metadata();
//        return produceOutput(getInputStream(is, metadata, httpHeaders, info), metadata, httpHeaders.getRequestHeaders(), info, "xml");
//    }

//    @POST
//    @Consumes("multipart/form-data")
//    @Produces("application/json")
//    @Path("form{" + HANDLER_TYPE_PARAM + " : (\\w+)?}")
//    public Metadata getJsonFromMultipart(Attachment att, @Context HttpHeaders httpHeaders, @Context final UriInfo info, @PathParam(HANDLER_TYPE_PARAM) String handlerTypeName)
//            throws IOException, TikaException {
//        Metadata metadata = new Metadata();
//        parseToMetadata(getInputStream(att.getObject(InputStream.class), metadata, httpHeaders, info), metadata, preparePostHeaderMap(att, httpHeaders), info, handlerTypeName);
//        TikaResource
//                .getConfig()
//                .getMetadataFilter()
//                .filter(metadata);
//        return metadata;
//    }

//    @PUT
//    @Consumes("*/*")
//    @Produces("application/json")
//    @Path("{" + HANDLER_TYPE_PARAM + " : (\\w+)?}")
//    public Metadata getJson(final InputStream is, @Context HttpHeaders httpHeaders, @Context final UriInfo info, @PathParam(HANDLER_TYPE_PARAM) String handlerTypeName)
//            throws IOException, TikaException {
//        Metadata metadata = new Metadata();
//        parseToMetadata(getInputStream(is, metadata, httpHeaders, info), metadata, httpHeaders.getRequestHeaders(), info, handlerTypeName);
//        TikaResource
//                .getConfig()
//                .getMetadataFilter()
//                .filter(metadata);
//        return metadata;
//    }

//    private void parseToMetadata(InputStream inputStream, Metadata metadata, MultivaluedMap<String, String> httpHeaders, UriInfo info, String handlerTypeName) throws IOException {
//        final Parser parser = createParser();
//        final ParseContext context = new ParseContext();
//
//        fillMetadata(parser, metadata, httpHeaders);
//        fillParseContext(httpHeaders, metadata, context);
//
//        logRequest(LOG, "/tika", metadata);
//        int writeLimit = -1;
//        boolean throwOnWriteLimitReached = getThrowOnWriteLimitReached(httpHeaders);
//        if (httpHeaders.containsKey("writeLimit")) {
//            writeLimit = Integer.parseInt(httpHeaders.getFirst("writeLimit"));
//        }
//
//        BasicContentHandlerFactory.HANDLER_TYPE type = BasicContentHandlerFactory.parseHandlerType(handlerTypeName, DEFAULT_HANDLER_TYPE);
//        BasicContentHandlerFactory fact = new BasicContentHandlerFactory(type, writeLimit, throwOnWriteLimitReached, context);
//        ContentHandler contentHandler = fact.getNewContentHandler();
//
//        try {
//            parse(parser, LOG, info.getPath(), inputStream, contentHandler, metadata, context);
//        } catch (TikaServerParseException e) {
//            Throwable cause = e.getCause();
//            boolean writeLimitReached = false;
//            if (WriteLimitReachedException.isWriteLimitReached(cause)) {
//                metadata.set(TikaCoreProperties.WRITE_LIMIT_REACHED, "true");
//                writeLimitReached = true;
//            }
//            if (TIKA_SERVER_CONFIG.isReturnStackTrace()) {
//                if (cause != null) {
//                    metadata.add(TikaCoreProperties.CONTAINER_EXCEPTION, ExceptionUtils.getStackTrace(cause));
//                } else {
//                    metadata.add(TikaCoreProperties.CONTAINER_EXCEPTION, ExceptionUtils.getStackTrace(e));
//                }
//            } else if (!writeLimitReached) {
//                throw e;
//            }
//        } catch (OutOfMemoryError e) {
//            if (TIKA_SERVER_CONFIG.isReturnStackTrace()) {
//                metadata.add(TikaCoreProperties.CONTAINER_EXCEPTION, ExceptionUtils.getStackTrace(e));
//            } else {
//                throw e;
//            }
//        } finally {
//            metadata.add(TikaCoreProperties.TIKA_CONTENT, contentHandler.toString());
//        }
//    }
//
//    private StreamingOutput produceOutput(final InputStream is, Metadata metadata, final MultivaluedMap<String, String> httpHeaders, final UriInfo info, final String format) {
//        final Parser parser = createParser();
//        final ParseContext context = new ParseContext();
//
//        fillMetadata(parser, metadata, httpHeaders);
//        fillParseContext(httpHeaders, metadata, context);
//
//
//        logRequest(LOG, "/tika", metadata);
//
//        return outputStream -> {
//            Writer writer = new OutputStreamWriter(outputStream, UTF_8);
//            ContentHandler content;
//
//            try {
//                SAXTransformerFactory factory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
//                TransformerHandler handler = factory.newTransformerHandler();
//                handler
//                        .getTransformer()
//                        .setOutputProperty(OutputKeys.METHOD, format);
//                handler
//                        .getTransformer()
//                        .setOutputProperty(OutputKeys.INDENT, "yes");
//                handler
//                        .getTransformer()
//                        .setOutputProperty(OutputKeys.ENCODING, UTF_8.name());
//                handler
//                        .getTransformer()
//                        .setOutputProperty(OutputKeys.VERSION, "1.1");
//                handler.setResult(new StreamResult(writer));
//                content = new ExpandedTitleContentHandler(handler);
//            } catch (TransformerConfigurationException e) {
//                throw new WebApplicationException(e);
//            }
//
//            parse(parser, LOG, info.getPath(), is, content, metadata, context);
//        };
//    }
//
//    /**
//     * Prepares a multivalued map, combining attachment headers and request headers.
//     * Gives priority to attachment headers.
//     *
//     * @param att         the attachment.
//     * @param httpHeaders the http headers, fetched from context.
//     * @return the case insensitive MetadataMap containing combined headers.
//     */
//    private MetadataMap<String, String> preparePostHeaderMap(Attachment att, HttpHeaders httpHeaders) {
//        if (att == null && httpHeaders == null) {
//            return null;
//        }
//        MetadataMap<String, String> finalHeaders = new MetadataMap<>(false, true);
//        if (httpHeaders != null && httpHeaders.getRequestHeaders() != null) {
//            finalHeaders.putAll(httpHeaders.getRequestHeaders());
//        }
//        if (att != null && att.getHeaders() != null) {
//            finalHeaders.putAll(att.getHeaders());
//        }
//        return finalHeaders;
//    }
}
