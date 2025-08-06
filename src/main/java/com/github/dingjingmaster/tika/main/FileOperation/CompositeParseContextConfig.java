package com.github.dingjingmaster.tika.main.FileOperation;

import java.util.List;

import org.apache.tika.config.ServiceLoader;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;


public class CompositeParseContextConfig implements ParseContextConfig {


    final List<ParseContextConfig> configs;

    public CompositeParseContextConfig() {
        configs = new ServiceLoader(CompositeParseContextConfig.class.getClassLoader()).loadServiceProviders(ParseContextConfig.class);
    }

    @Override
    public void configure(Metadata metadata, ParseContext context) {
        for (ParseContextConfig config : configs) {
            config.configure(metadata, context);
        }
    }
}
