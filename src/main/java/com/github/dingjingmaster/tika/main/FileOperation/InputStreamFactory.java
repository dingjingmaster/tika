package com.github.dingjingmaster.tika.main.FileOperation;

import java.io.IOException;
import java.io.InputStream;

import org.apache.tika.metadata.Metadata;

/**
 * Interface to allow for custom/consistent creation of InputStream
 * <p>
 * This factory is used statically in TikaResource.  Make sure not
 * to hold instance state in implementations.
 */
public interface InputStreamFactory {

    InputStream getInputStream(InputStream is, Metadata metadata) throws IOException;

}
