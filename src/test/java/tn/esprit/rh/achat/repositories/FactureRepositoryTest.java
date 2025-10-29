package tn.esprit.rh.achat.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import tn.esprit.rh.achat.entities.Facture;
import tn.esprit.rh.achat.entities.Fournisseur;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class FactureRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FactureRepository factureRepository;

    private Fournisseur fournisseur;
    private Facture facture1;
    private Facture facture2;
    private Facture facture3;

    @BeforeEach
    void setUp() {
        // Create and persist a Fournisseur
        fournisseur = new Fournisseur();
        fournisseur.setCode("F001");
        fournisseur.setLibelle("Test Fournisseur");
        entityManager.persist(fournisseur);

        // Create factures with different dates
        Calendar cal = Calendar.getInstance();
        
        // Facture 1 - Active, within date range
        facture1 = new Facture();
        facture1.setMontantFacture(1000.0f);
        facture1.setMontantRemise(100.0f);
        facture1.setArchivee(false);
        facture1.setFournisseur(fournisseur);
        cal.set(2024, Calendar.JANUARY, 15);
        facture1.setDateCreationFacture(cal.getTime());
        entityManager.persist(facture1);

        // Facture 2 - Active, within date range
        facture2 = new Facture();
        facture2.setMontantFacture(2000.0f);
        facture2.setMontantRemise(200.0f);
        facture2.setArchivee(false);
        facture2.setFournisseur(fournisseur);
        cal.set(2024, Calendar.FEBRUARY, 15);
        facture2.setDateCreationFacture(cal.getTime());
        entityManager.persist(facture2);

        // Facture 3 - Archived, within date range
        facture3 = new Facture();
        facture3.setMontantFacture(3000.0f);
        facture3.setMontantRemise(300.0f);
        facture3.setArchivee(true);
        facture3.setFournisseur(fournisseur);
        cal.set(2024, Calendar.MARCH, 15);
        facture3.setDateCreationFacture(cal.getTime());
        entityManager.persist(facture3);

        entityManager.flush();
    }

    @Test
    void testGetFactureByFournisseur_Success() {
        // Act
        List<Facture> result = factureRepository.getFactureByFournisseur(fournisseur);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size()); // Only non-archived factures
        assertTrue(result.stream().noneMatch(Facture::getArchivee));
        assertTrue(result.stream().allMatch(f -> f.getFournisseur().equals(fournisseur)));
    }

    @Test
    void testGetFactureByFournisseur_NoActiveFactures() {
        // Arrange - Archive all factures
        facture1.setArchivee(true);
        facture2.setArchivee(true);
        entityManager.flush();

        // Act
        List<Facture> result = factureRepository.getFactureByFournisseur(fournisseur);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetTotalFacturesEntreDeuxDates_Success() {
        // Arrange
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.JANUARY, 1);
        Date startDate = cal.getTime();
        cal.set(2024, Calendar.FEBRUARY, 28);
        Date endDate = cal.getTime();

        // Act
        float total = factureRepository.getTotalFacturesEntreDeuxDates(startDate, endDate);

        // Assert
        // Should sum facture1 (1000) + facture2 (2000) = 3000
        // facture3 is archived so it should not be included
        assertEquals(3000.0f, total, 0.01f);
    }

    @Test
    void testGetTotalFacturesEntreDeuxDates_OnlyActiveFactures() {
        // Arrange
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.JANUARY, 1);
        Date startDate = cal.getTime();
        cal.set(2024, Calendar.DECEMBER, 31);
        Date endDate = cal.getTime();

        // Act
        float total = factureRepository.getTotalFacturesEntreDeuxDates(startDate, endDate);

        // Assert
        // Should sum only facture1 (1000) + facture2 (2000) = 3000
        // facture3 is archived (3000) so excluded
        assertEquals(3000.0f, total, 0.01f);
    }

    @Test
    void testGetTotalFacturesEntreDeuxDates_NoFacturesInRange() {
        // Arrange
        Calendar cal = Calendar.getInstance();
        cal.set(2023, Calendar.JANUARY, 1);
        Date startDate = cal.getTime();
        cal.set(2023, Calendar.DECEMBER, 31);
        Date endDate = cal.getTime();

        // Act & Assert
        // When there are no factures in the range, SUM returns null which causes
        // AopInvocationException when trying to convert to primitive float
        // This is a known limitation of using primitive return types with aggregate functions
        assertThrows(org.springframework.aop.AopInvocationException.class, () -> {
            factureRepository.getTotalFacturesEntreDeuxDates(startDate, endDate);
        });
    }

    @Test
    void testUpdateFacture_Success() {
        // Arrange
        Long factureId = facture1.getIdFacture();
        assertFalse(facture1.getArchivee());

        // Act
        factureRepository.updateFacture(factureId);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Facture updatedFacture = factureRepository.findById(factureId).orElse(null);
        assertNotNull(updatedFacture);
        assertTrue(updatedFacture.getArchivee());
    }

    @Test
    void testUpdateFacture_NonExistentId() {
        // Act
        factureRepository.updateFacture(999L);
        entityManager.flush();

        // Assert - Should not throw exception, just do nothing
        List<Facture> allFactures = factureRepository.findAll();
        assertEquals(3, allFactures.size());
    }

    @Test
    void testSaveFacture_Success() {
        // Arrange
        Facture newFacture = new Facture();
        newFacture.setMontantFacture(5000.0f);
        newFacture.setMontantRemise(500.0f);
        newFacture.setArchivee(false);
        newFacture.setDateCreationFacture(new Date());
        newFacture.setFournisseur(fournisseur);

        // Act
        Facture savedFacture = factureRepository.save(newFacture);

        // Assert
        assertNotNull(savedFacture);
        assertNotNull(savedFacture.getIdFacture());
        assertEquals(5000.0f, savedFacture.getMontantFacture());
    }

    @Test
    void testFindById_Success() {
        // Act
        Facture found = factureRepository.findById(facture1.getIdFacture()).orElse(null);

        // Assert
        assertNotNull(found);
        assertEquals(facture1.getIdFacture(), found.getIdFacture());
        assertEquals(facture1.getMontantFacture(), found.getMontantFacture());
    }

    @Test
    void testFindById_NotFound() {
        // Act
        Facture found = factureRepository.findById(999L).orElse(null);

        // Assert
        assertNull(found);
    }

    @Test
    void testFindAll_Success() {
        // Act
        List<Facture> allFactures = factureRepository.findAll();

        // Assert
        assertNotNull(allFactures);
        assertEquals(3, allFactures.size());
    }

    @Test
    void testDeleteFacture_Success() {
        // Arrange
        Long factureId = facture1.getIdFacture();

        // Act
        factureRepository.deleteById(factureId);
        entityManager.flush();

        // Assert
        Facture deletedFacture = factureRepository.findById(factureId).orElse(null);
        assertNull(deletedFacture);
    }
}

