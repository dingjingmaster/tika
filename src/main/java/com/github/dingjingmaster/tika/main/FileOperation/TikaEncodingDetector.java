package com.github.dingjingmaster.tika.main.FileOperation;

import org.apache.tika.detect.EncodingDetector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.txt.CharsetDetector;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * 多种文本编码格式解析
 */
public class TikaEncodingDetector implements EncodingDetector {
    private final CharsetDetector detector = new CharsetDetector();

    @Override
    public Charset detect(InputStream inputStream, Metadata metadata) throws IOException {
        detector.setText(inputStream);
        String name = detector.detect().getName();
        inputStream.reset();
        if (null != name) {
            return Charset.forName(name);
        }
        return null;
    }
}
