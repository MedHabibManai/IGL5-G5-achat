package tn.esprit.rh.achat.services;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import tn.esprit.rh.achat.entities.Produit;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class ProduitServiceImplTest {

    @Autowired
    IProduitService produitService;

    @Test
    public void testAddProduit() {
        log.info("Starting testAddProduit...");
        
        // Create a new product
        Produit produit = new Produit();
        produit.setCodeProduit("PROD001");
        produit.setLibelleProduit("Laptop Dell XPS 15");
        produit.setPrix(1299.99f);
        produit.setDateCreation(new Date());
        produit.setDateDerniereModification(new Date());

        // Add the product
        Produit savedProduit = produitService.addProduit(produit);
        
        log.info("Product added: " + savedProduit);
        
        // Assertions
        assertNotNull(savedProduit);
        assertNotNull(savedProduit.getIdProduit());
        assertEquals("PROD001", savedProduit.getCodeProduit());
        assertEquals("Laptop Dell XPS 15", savedProduit.getLibelleProduit());
        assertEquals(1299.99f, savedProduit.getPrix(), 0.01);
        
        // Cleanup
        produitService.deleteProduit(savedProduit.getIdProduit());
        log.info("testAddProduit completed successfully");
    }

    @Test
    public void testRetrieveProduit() {
        log.info("Starting testRetrieveProduit...");
        
        // First, create a product
        Produit produit = new Produit();
        produit.setCodeProduit("PROD002");
        produit.setLibelleProduit("iPhone 13 Pro");
        produit.setPrix(999.99f);
        produit.setDateCreation(new Date());
        
        Produit savedProduit = produitService.addProduit(produit);
        Long produitId = savedProduit.getIdProduit();
        
        // Retrieve the product
        Produit retrievedProduit = produitService.retrieveProduit(produitId);
        
        log.info("Product retrieved: " + retrievedProduit);
        
        // Assertions
        assertNotNull(retrievedProduit);
        assertEquals(produitId, retrievedProduit.getIdProduit());
        assertEquals("PROD002", retrievedProduit.getCodeProduit());
        assertEquals("iPhone 13 Pro", retrievedProduit.getLibelleProduit());
        assertEquals(999.99f, retrievedProduit.getPrix(), 0.01);
        
        // Cleanup
        produitService.deleteProduit(produitId);
        log.info("testRetrieveProduit completed successfully");
    }

    @Test
    public void testUpdateProduit() {
        log.info("Starting testUpdateProduit...");
        
        // Create a product
        Produit produit = new Produit();
        produit.setCodeProduit("PROD003");
        produit.setLibelleProduit("Samsung Galaxy S21");
        produit.setPrix(799.99f);
        produit.setDateCreation(new Date());
        
        Produit savedProduit = produitService.addProduit(produit);
        Long produitId = savedProduit.getIdProduit();
        
        // Update the product
        savedProduit.setLibelleProduit("Samsung Galaxy S22");
        savedProduit.setPrix(899.99f);
        savedProduit.setDateDerniereModification(new Date());
        
        Produit updatedProduit = produitService.updateProduit(savedProduit);
        
        log.info("Product updated: " + updatedProduit);
        
        // Assertions
        assertNotNull(updatedProduit);
        assertEquals(produitId, updatedProduit.getIdProduit());
        assertEquals("Samsung Galaxy S22", updatedProduit.getLibelleProduit());
        assertEquals(899.99f, updatedProduit.getPrix(), 0.01);
        assertNotNull(updatedProduit.getDateDerniereModification());
        
        // Cleanup
        produitService.deleteProduit(produitId);
        log.info("testUpdateProduit completed successfully");
    }

    @Test
    public void testDeleteProduit() {
        log.info("Starting testDeleteProduit...");
        
        // Create a product
        Produit produit = new Produit();
        produit.setCodeProduit("PROD004");
        produit.setLibelleProduit("MacBook Pro 16");
        produit.setPrix(2499.99f);
        produit.setDateCreation(new Date());
        
        Produit savedProduit = produitService.addProduit(produit);
        Long produitId = savedProduit.getIdProduit();
        
        log.info("Product created with ID: " + produitId);
        
        // Delete the product
        produitService.deleteProduit(produitId);
        
        // Verify deletion
        Produit deletedProduit = produitService.retrieveProduit(produitId);
        
        log.info("Product after deletion: " + deletedProduit);
        
        // Assertion
        assertNull(deletedProduit);
        log.info("testDeleteProduit completed successfully");
    }

    @Test
    public void testRetrieveAllProduits() {
        log.info("Starting testRetrieveAllProduits...");
        
        // Create multiple products
        Produit produit1 = new Produit();
        produit1.setCodeProduit("PROD005");
        produit1.setLibelleProduit("iPad Air");
        produit1.setPrix(599.99f);
        produit1.setDateCreation(new Date());
        
        Produit produit2 = new Produit();
        produit2.setCodeProduit("PROD006");
        produit2.setLibelleProduit("Apple Watch Series 7");
        produit2.setPrix(399.99f);
        produit2.setDateCreation(new Date());
        
        Produit savedProduit1 = produitService.addProduit(produit1);
        Produit savedProduit2 = produitService.addProduit(produit2);
        
        // Retrieve all products
        List<Produit> produits = produitService.retrieveAllProduits();
        
        log.info("Total products retrieved: " + produits.size());
        
        // Assertions
        assertNotNull(produits);
        assertTrue(produits.size() >= 2);
        
        // Verify our products are in the list
        boolean found1 = false;
        boolean found2 = false;
        for (Produit p : produits) {
            if (p.getIdProduit().equals(savedProduit1.getIdProduit())) {
                found1 = true;
            }
            if (p.getIdProduit().equals(savedProduit2.getIdProduit())) {
                found2 = true;
            }
        }
        
        assertTrue("Product 1 should be in the list", found1);
        assertTrue("Product 2 should be in the list", found2);
        
        // Cleanup
        produitService.deleteProduit(savedProduit1.getIdProduit());
        produitService.deleteProduit(savedProduit2.getIdProduit());
        log.info("testRetrieveAllProduits completed successfully");
    }

    @Test
    public void testAddProduitWithNullValues() {
        log.info("Starting testAddProduitWithNullValues...");
        
        // Create a product with minimal information
        Produit produit = new Produit();
        produit.setCodeProduit("PROD007");
        produit.setLibelleProduit("Test Product");
        produit.setPrix(0.0f);
        
        // Add the product
        Produit savedProduit = produitService.addProduit(produit);
        
        log.info("Product with null values added: " + savedProduit);
        
        // Assertions
        assertNotNull(savedProduit);
        assertNotNull(savedProduit.getIdProduit());
        assertEquals("PROD007", savedProduit.getCodeProduit());
        
        // Cleanup
        produitService.deleteProduit(savedProduit.getIdProduit());
        log.info("testAddProduitWithNullValues completed successfully");
    }

    @Test
    public void testUpdateProduitPrice() {
        log.info("Starting testUpdateProduitPrice...");
        
        // Create a product
        Produit produit = new Produit();
        produit.setCodeProduit("PROD008");
        produit.setLibelleProduit("Sony PlayStation 5");
        produit.setPrix(499.99f);
        produit.setDateCreation(new Date());
        
        Produit savedProduit = produitService.addProduit(produit);
        float originalPrice = savedProduit.getPrix();
        
        // Update only the price
        savedProduit.setPrix(549.99f);
        savedProduit.setDateDerniereModification(new Date());
        
        Produit updatedProduit = produitService.updateProduit(savedProduit);
        
        log.info("Original price: " + originalPrice + ", New price: " + updatedProduit.getPrix());
        
        // Assertions
        assertNotEquals(originalPrice, updatedProduit.getPrix(), 0.01);
        assertEquals(549.99f, updatedProduit.getPrix(), 0.01);
        
        // Cleanup
        produitService.deleteProduit(savedProduit.getIdProduit());
        log.info("testUpdateProduitPrice completed successfully");
    }
}
