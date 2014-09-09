package utilities;

import groovy.text.*;

public class Helper {

    /*
     * Prepare the script by replacing groovy-variables (marked with @@{})
     * while still allowing Jenkins variables (marked with $).
     */
    def static prepareScript(String script, Map binding) {
        def templateEngine = new SimpleTemplateEngine();
        def template = templateEngine.createTemplate(
            script.replaceAll(~/\$/, /\\\$/).replaceAll(~/@@/, /\$/));

        return template.make(binding).toString();
    }
}

