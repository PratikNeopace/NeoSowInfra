package com.neosow.infra.service;

import java.util.Map;

public interface PdfService {
    byte[] generatePdf(String templateName, Map<String, Object> data);
}
