package com.neosow.infra.service.impl;

import com.neosow.infra.service.PdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfServiceImpl implements PdfService {

    private final TemplateEngine templateEngine;

    @Override
    public byte[] generatePdf(String templateName, Map<String, Object> data) {
        log.info("Generating PDF using template: {}", templateName);
        try {
            // Setup Thymeleaf Context
            Context context = new Context();
            context.setVariables(data);

            // Render HTML string
            String htmlContent = templateEngine.process(templateName, context);

            // Generate PDF via Flying Saucer
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            
            // Render from HTML string
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
            renderer.finishPDF();

            log.info("PDF generated successfully");
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error rendering PDF template", e);
        }
    }
}
