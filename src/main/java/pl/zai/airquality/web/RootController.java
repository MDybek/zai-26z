package pl.zai.airquality.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String index() {
        return """
                <!DOCTYPE html>
                <html lang="pl">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <meta name="description" content="ZAI 26Z Air Quality IoT Series API">
                    <title>ZAI 26Z - Air Quality API</title>
                    <style>
                        body { font-family: system-ui, -apple-system, sans-serif; margin: 2rem; background: #f8fafc; color: #1e293b; }
                        .card { background: white; padding: 2rem; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); max-width: 600px; margin: 0 auto; }
                        h1 { color: #0f172a; margin-top: 0.5rem; font-size: 1.5rem; }
                        p { line-height: 1.5; color: #475569; }
                        ul { padding-left: 1.25rem; }
                        li { margin-bottom: 0.5rem; }
                        code { background: #f1f5f9; padding: 0.2rem 0.4rem; border-radius: 4px; font-size: 0.9em; color: #0f172a; }
                        .badge { display: inline-block; background: #16a34a; color: white; padding: 0.25rem 0.75rem; border-radius: 9999px; font-size: 0.85rem; font-weight: 600; }
                    </style>
                </head>
                <body>
                    <main class="card">
                        <span class="badge">API Status: OK</span>
                        <h1>ZAI 26Z - Air Quality Measurement Series API</h1>
                        <p>REST API backend for IoT air-quality measurement series compliant with OpenAPI 3.1 specification (RFC 9457 Problem Details).</p>
                        <h2>Dostępne punkty końcowe:</h2>
                        <ul>
                            <li><code>GET /api/health</code> - Sprawdzenie dostępności</li>
                            <li><code>GET /api/series</code> - Serie pomiarowe</li>
                            <li><code>GET /api/measurements</code> - Pomiary (filtry series, from, to, sort)</li>
                            <li><code>POST /api/auth/login</code> - Uwierzytelnianie administratora</li>
                        </ul>
                    </main>
                </body>
                </html>
                """;
    }
}
