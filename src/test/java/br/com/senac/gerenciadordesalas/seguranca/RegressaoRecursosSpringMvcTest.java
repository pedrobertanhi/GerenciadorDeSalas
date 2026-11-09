package br.com.senac.gerenciadordesalas.seguranca;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Detecta a introducao de recursos que tornam aplicaveis as CVEs
 * CVE-2026-47884 (XsltView) e CVE-2026-47890 (SSE com fragmentos).
 * Este teste nao substitui a atualizacao de bibliotecas vulneraveis.
 */
class RegressaoRecursosSpringMvcTest {

    @Test
    void naoIntroduzirXsltViewOuSseComFragmentosSemRevisaoDeSeguranca() throws IOException {
        Path sources = Path.of("src", "main", "java");
        try (Stream<Path> files = Files.walk(sources)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertFalse(source.contains("XsltView") || source.contains("XsltViewResolver"),
                        () -> "Uso de XSLT requer revisao de CVE-2026-47884: " + file);
                assertFalse(source.contains("SseEmitter") || source.contains("TEXT_EVENT_STREAM_VALUE")
                                || source.contains("text/event-stream") || source.contains("ServerSentEvent"),
                        () -> "Uso de SSE requer revisao de CVE-2026-47890: " + file);
            }
        }
    }
}
