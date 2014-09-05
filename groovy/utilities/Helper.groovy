package utilities;

import groovy.text.*;

public class Helper {
    def static prepareScript(String script, Map binding) {
        def templateEngine = new SimpleTemplateEngine();
        def template = templateEngine.createTemplate(
            script.replaceAll(~/\$/, /\\\$/).replaceAll(~/@@/, /\$/));

        return template.make(binding).toString();
    }
}

