package com.github.dingjingmaster.tika.main.FileOperation;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ocr.TesseractOCRParser;
import org.apache.tika.parser.ocr.tess4j.ImageDeskew;
import org.apache.tika.utils.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class TikaImagePreprocessor implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(TikaImagePreprocessor.class);
    private static final double MINIMUM_DESKEW_THRESHOLD = 1.0D;

    private final String fullImageMagickPath;

    TikaImagePreprocessor(String fullImageMagickPath) {
        this.fullImageMagickPath = fullImageMagickPath;
    }


    //this assumes that image magick is available
    void process(Path sourceFile, Path targFile, Metadata metadata) throws IOException {
        double angle = getAngle(sourceFile, metadata);

        ArrayList<String> cmd = new ArrayList<>();
        cmd.add(fullImageMagickPath);
        cmd.add("identify");
        cmd.add("-verbose");
        cmd.add(sourceFile.toString());

        BufferedReader bufIn = null;
        String cmdStr = String.join(" ", cmd);

        try (FileOutputStream outputFile = new FileOutputStream(targFile.toFile())) {
            Process process = Runtime.getRuntime().exec(cmdStr, null, new File("/tmp"));
            process.waitFor(600, TimeUnit.SECONDS);

            bufIn = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            while (true) {
                String res = bufIn.readLine();
                if (res.isEmpty()) {
                    break;
                }
                outputFile.write(res.getBytes());
                outputFile.write(System.lineSeparator().getBytes());
            }
        }
        catch (Exception e) {
            logger.warn("magick error: {}", e.toString());
        }
        finally {
            if (bufIn != null) {
                bufIn.close();
            }
        }

        metadata.add(TesseractOCRParser.IMAGE_MAGICK, "true");
    }

    /**
     * Get the current skew angle of the image.  Positive = clockwise; Negative = counter-clockwise
     */
    private double getAngle(Path sourceFile, Metadata metadata) throws IOException {
        BufferedImage bi = ImageIO.read(sourceFile.toFile());
        ImageDeskew id = new ImageDeskew(bi);
        double angle = id.getSkewAngle();

        if (angle < MINIMUM_DESKEW_THRESHOLD && angle > -MINIMUM_DESKEW_THRESHOLD) {
            logger.debug("Changing angle " + angle + " to 0.0");
            angle = 0d;
        }
        else {
            metadata.add(TesseractOCRParser.IMAGE_ROTATION, String.format(Locale.getDefault(), "%.3f", angle));
        }

        return angle;
    }
}
