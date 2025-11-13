package tn.esprit.rh.achat.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * Welcome Controller - Provides root endpoint with API documentation links
 */
@Controller
public class WelcomeController {

    @GetMapping("/")
    @ResponseBody
    public Map<String, Object> welcome() {
        Map<String, Object> response = new HashMap<>();
        response.put("application", "Achat E-Commerce Application");
        response.put("version", "1.0");
        response.put("status", "UP");
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("Swagger UI", "/swagger-ui/");
        endpoints.put("API Docs", "/v2/api-docs");
        endpoints.put("Health Check", "/actuator/health");
        endpoints.put("Produits", "/produit/retrieve-all-produits");
        endpoints.put("Stocks", "/stock/retrieve-all-stocks");
        endpoints.put("Fournisseurs", "/fournisseur/retrieve-all-fournisseurs");
        endpoints.put("Factures", "/facture/retrieve-all-factures");
        endpoints.put("Operateurs", "/operateur/retrieve-all-operateurs");
        endpoints.put("Reglements", "/reglement/retrieve-all-reglements");
        
        response.put("endpoints", endpoints);
        response.put("message", "Welcome to Achat API! Visit /swagger-ui/ for complete API documentation.");
        
        return response;
    }
}

