package com.github.dingjingmaster.tika.main.FileOperation;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;

/**
 * Implementations must be thread-safe!
 * <p>
 * This class translates http headers into objects/configurations set
 * via the ParseContext
 */
public interface ParseContextConfig {

    /**
     * Configures the parseContext with present headers.
     *
     * @param metadata the metadata.
     * @param context  the parse context to configure.
     */
    void configure(Metadata metadata, ParseContext context);
}
