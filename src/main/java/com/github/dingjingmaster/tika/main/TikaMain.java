package com.github.dingjingmaster.tika.main;

import org.apache.tika.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

/**
 * Tika 主要模块:
 *  解析器(Parser): 负责解析特定格式的文档内容
 *  检测器(Detector): 用于确定文档的类型和元数据
 *  标记器(Tokenizer): 将文本分解为标记(如：词、句子等)
 *  语言检测器(Language Detector): 用于确定文本的语言
 *  元数据提取器(Metadata Extractor): 从文档中抽取元数据，如：标题、作者、创建时间等
 */
public final class TikaMain {
    /**
     *
     */
    private TikaMain() {
        System.setSecurityManager(null);
        System.setProperty("org.apache.tika.debug", "false");
        System.setProperty("org.apache.tika.parser.ocr.debug", "false");
        System.setProperty("tika.mimetypes", "org/apache/tika/mime/tika-mimetypes.xml");
    }

    public void listAllParser() {
        System.out.println("All parser: ");
        ServiceLoader<Parser> loader = ServiceLoader.load(Parser.class);
        for (Parser p : loader) {
            System.out.println("    " + p.getClass().getName());
        }
    }

    /**
     * @param args
     */
    public static void main(final String[] args) {
        TikaMain tikaMain = new TikaMain();

        tikaMain.listAllParser();
    }
}

