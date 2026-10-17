package com.papaya.claims.web;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class WebInterfaceTest {
    @Test void staticWorkspaceAssetsProvideAccessibleResponsiveAndCredentialSafeInterface() throws Exception {
        String html = resource("static/index.html"); String js = resource("static/app.js"); String css = resource("static/styles.css");
        assertThat(html).contains("lang=\"en\"", "aria-live=\"polite\"", "id=\"login-form\"", "id=\"claim-detail\"");
        assertThat(html).doesNotContain("http://", "https://", "system password", "<script src=\"https://");
        assertThat(js).contains("textContent", "csrfHeader", "crypto.randomUUID", "credentials: 'same-origin'", "state.credentials = null");
        assertThat(js).doesNotContain("innerHTML", "password123", "demo-password", "system:");
        assertThat(css).contains("@media(max-width:760px)", ":focus-visible", "[hidden]");
    }
    private static String resource(String path) throws Exception {
        try (var stream = WebInterfaceTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("classpath asset " + path).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
