import re

with open('src/main/java/com/neosow/infra/service/impl/QuotationServiceImpl.java', 'r') as f:
    content = f.read()

old_payload = """            String pdfUrl = String.format("https://api.neosowinfra.com/api/v1/quotations/%s/pdf", id);
            
            // Sending as a document (PDF) instead of text
            java.util.Map<String, Object> payload = java.util.Map.of(
                    "messaging_product", "whatsapp",
                    "to", cleanMobile,
                    "type", "document",
                    "document", java.util.Map.of(
                        "link", pdfUrl,
                        "filename", "NeoSow_Quotation_" + id + ".pdf",
                        "caption", messageBody
                    )
            );"""

new_payload = """            // Sending as an approved Meta WhatsApp Template
            java.util.List<java.util.Map<String, String>> parameters = java.util.List.of(
                    java.util.Map.of("type", "text", "text", customer.getName() != null ? customer.getName() : "Customer"),
                    java.util.Map.of("type", "text", "text", quotation.getId().toString()),
                    java.util.Map.of("type", "text", "text", grandTotal.setScale(2, java.math.RoundingMode.HALF_UP).toString()),
                    java.util.Map.of("type", "text", "text", quotation.getId().toString())
            );

            java.util.Map<String, Object> payload = java.util.Map.of(
                    "messaging_product", "whatsapp",
                    "to", cleanMobile,
                    "type", "template",
                    "template", java.util.Map.of(
                            "name", "neosow_quotation",
                            "language", java.util.Map.of("code", "en"),
                            "components", java.util.List.of(
                                    java.util.Map.of(
                                            "type", "body",
                                            "parameters", parameters
                                    )
                            )
                    )
            );"""

if old_payload in content:
    content = content.replace(old_payload, new_payload)
    with open('src/main/java/com/neosow/infra/service/impl/QuotationServiceImpl.java', 'w') as f:
        f.write(content)
    print("Replaced payload.")
else:
    print("Could not find payload!")
