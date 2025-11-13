package tn.esprit.rh.achat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tn.esprit.rh.achat.entities.*;
import tn.esprit.rh.achat.repositories.*;

import java.util.Date;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private FournisseurRepository fournisseurRepository;

    @Autowired
    private OperateurRepository operateurRepository;

    @Autowired
    private DetailFournisseurRepository detailFournisseurRepository;

    @Override
    public void run(String... args) throws Exception {
        // Only initialize if database is empty
        if (produitRepository.count() == 0) {
            log.info("Initializing database with sample data...");
            initializeData();
            log.info("Database initialization complete!");
        } else {
            log.info("Database already contains data, skipping initialization.");
        }
    }

    private void initializeData() {
        // Create Stocks
        Stock stock1 = new Stock();
        stock1.setLibelleStock("Stock Principal");
        stock1.setQte(1000);
        stock1.setQteMin(100);
        stockRepository.save(stock1);

        Stock stock2 = new Stock();
        stock2.setLibelleStock("Stock Secondaire");
        stock2.setQte(500);
        stock2.setQteMin(50);
        stockRepository.save(stock2);

        // Create Produits
        Produit produit1 = new Produit();
        produit1.setCodeProduit("PROD001");
        produit1.setLibelleProduit("Laptop Dell XPS 15");
        produit1.setPrix(3500.00f);
        produit1.setDateCreation(new Date());
        produit1.setDateDerniereModification(new Date());
        produit1.setStock(stock1);
        produitRepository.save(produit1);

        Produit produit2 = new Produit();
        produit2.setCodeProduit("PROD002");
        produit2.setLibelleProduit("iPhone 14 Pro");
        produit2.setPrix(4200.00f);
        produit2.setDateCreation(new Date());
        produit2.setDateDerniereModification(new Date());
        produit2.setStock(stock1);
        produitRepository.save(produit2);

        Produit produit3 = new Produit();
        produit3.setCodeProduit("PROD003");
        produit3.setLibelleProduit("Samsung Galaxy S23");
        produit3.setPrix(3800.00f);
        produit3.setDateCreation(new Date());
        produit3.setDateDerniereModification(new Date());
        produit3.setStock(stock2);
        produitRepository.save(produit3);

        Produit produit4 = new Produit();
        produit4.setCodeProduit("PROD004");
        produit4.setLibelleProduit("MacBook Pro M2");
        produit4.setPrix(5500.00f);
        produit4.setDateCreation(new Date());
        produit4.setDateDerniereModification(new Date());
        produit4.setStock(stock1);
        produitRepository.save(produit4);

        Produit produit5 = new Produit();
        produit5.setCodeProduit("PROD005");
        produit5.setLibelleProduit("iPad Air");
        produit5.setPrix(2200.00f);
        produit5.setDateCreation(new Date());
        produit5.setDateDerniereModification(new Date());
        produit5.setStock(stock2);
        produitRepository.save(produit5);

        // Create Detail Fournisseurs
        DetailFournisseur detail1 = new DetailFournisseur();
        detail1.setAdresse("123 Avenue Habib Bourguiba, Tunis");
        detail1.setEmail("contact@techsupply.tn");
        detail1.setMatricule("MAT001");
        detail1.setDateDebutCollaboration(new Date());
        detailFournisseurRepository.save(detail1);

        DetailFournisseur detail2 = new DetailFournisseur();
        detail2.setAdresse("456 Rue de la République, Sfax");
        detail2.setEmail("info@electronicsplus.tn");
        detail2.setMatricule("MAT002");
        detail2.setDateDebutCollaboration(new Date());
        detailFournisseurRepository.save(detail2);

        DetailFournisseur detail3 = new DetailFournisseur();
        detail3.setAdresse("789 Boulevard 7 Novembre, Sousse");
        detail3.setEmail("sales@mobilehub.tn");
        detail3.setMatricule("MAT003");
        detail3.setDateDebutCollaboration(new Date());
        detailFournisseurRepository.save(detail3);

        // Create Fournisseurs
        Fournisseur fournisseur1 = new Fournisseur();
        fournisseur1.setCode("FOUR001");
        fournisseur1.setLibelle("Tech Supply Tunisia");
        fournisseur1.setCategorieFournisseur(CategorieFournisseur.ORDINAIRE);
        fournisseur1.setDetailFournisseur(detail1);
        fournisseurRepository.save(fournisseur1);

        Fournisseur fournisseur2 = new Fournisseur();
        fournisseur2.setCode("FOUR002");
        fournisseur2.setLibelle("Electronics Plus");
        fournisseur2.setCategorieFournisseur(CategorieFournisseur.CONVENTIONNE);
        fournisseur2.setDetailFournisseur(detail2);
        fournisseurRepository.save(fournisseur2);

        Fournisseur fournisseur3 = new Fournisseur();
        fournisseur3.setCode("FOUR003");
        fournisseur3.setLibelle("Mobile Hub");
        fournisseur3.setCategorieFournisseur(CategorieFournisseur.ORDINAIRE);
        fournisseur3.setDetailFournisseur(detail3);
        fournisseurRepository.save(fournisseur3);

        // Create Operateurs
        Operateur operateur1 = new Operateur();
        operateur1.setNom("Ben Ali");
        operateur1.setPrenom("Ahmed");
        operateur1.setPassword("password123");
        operateurRepository.save(operateur1);

        Operateur operateur2 = new Operateur();
        operateur2.setNom("Trabelsi");
        operateur2.setPrenom("Fatma");
        operateur2.setPassword("password456");
        operateurRepository.save(operateur2);

        Operateur operateur3 = new Operateur();
        operateur3.setNom("Hamdi");
        operateur3.setPrenom("Mohamed");
        operateur3.setPassword("password789");
        operateurRepository.save(operateur3);

        log.info("Created {} products", produitRepository.count());
        log.info("Created {} stocks", stockRepository.count());
        log.info("Created {} fournisseurs", fournisseurRepository.count());
        log.info("Created {} operateurs", operateurRepository.count());
    }
}

