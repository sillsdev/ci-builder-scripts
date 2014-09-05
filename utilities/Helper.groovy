package utilities

import groovy.text.*

public class Helper {
    public static string PrepareScript(string script, Map binding) {
        def templateEngine = new SimpleTemplateEngine();
        def template = templateEngine.createTemplate(
            script.replaceAll(~/\$/, /\\\$/).replaceAll(~/@@/, /\$/));

        return template.make(values).toString();
    }
}

