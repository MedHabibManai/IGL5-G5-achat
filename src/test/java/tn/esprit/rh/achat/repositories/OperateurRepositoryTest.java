package tn.esprit.rh.achat.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import tn.esprit.rh.achat.entities.Operateur;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for OperateurRepository
 * Tests basic CRUD operations using H2 in-memory database
 */
@DataJpaTest
class OperateurRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OperateurRepository operateurRepository;

    private Operateur operateur1;
    private Operateur operateur2;

    @BeforeEach
    void setUp() {
        // Clear any existing data
        operateurRepository.deleteAll();

        // Initialize test data
        operateur1 = new Operateur();
        operateur1.setNom("Slouma");
        operateur1.setPrenom("Rayen");
        operateur1.setPassword("password123");

        operateur2 = new Operateur();
        operateur2.setNom("Manai");
        operateur2.setPrenom("Habib");
        operateur2.setPassword("password456");

        // Persist test data
        entityManager.persist(operateur1);
        entityManager.persist(operateur2);
        entityManager.flush();
    }

    @Test
    void testSaveOperateur_Success() {
        // Arrange
        Operateur newOperateur = new Operateur();
        newOperateur.setNom("Test");
        newOperateur.setPrenom("User");
        newOperateur.setPassword("testpass");

        // Act
        Operateur saved = operateurRepository.save(newOperateur);

        // Assert
        assertNotNull(saved);
        assertNotNull(saved.getIdOperateur());
        assertEquals("Test", saved.getNom());
        assertEquals("User", saved.getPrenom());
        assertEquals("testpass", saved.getPassword());
    }

    @Test
    void testFindById_Success() {
        // Act
        Optional<Operateur> found = operateurRepository.findById(operateur1.getIdOperateur());

        // Assert
        assertTrue(found.isPresent());
        assertEquals("Slouma", found.get().getNom());
        assertEquals("Rayen", found.get().getPrenom());
    }

    @Test
    void testFindById_NotFound() {
        // Act
        Optional<Operateur> found = operateurRepository.findById(999L);

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void testFindAll_Success() {
        // Act
        List<Operateur> operateurs = (List<Operateur>) operateurRepository.findAll();

        // Assert
        assertNotNull(operateurs);
        assertEquals(2, operateurs.size());
    }

    @Test
    void testUpdateOperateur_Success() {
        // Arrange
        operateur1.setNom("Updated Name");
        operateur1.setPrenom("Updated Prenom");

        // Act
        Operateur updated = operateurRepository.save(operateur1);

        // Assert
        assertNotNull(updated);
        assertEquals("Updated Name", updated.getNom());
        assertEquals("Updated Prenom", updated.getPrenom());
    }

    @Test
    void testDeleteOperateur_Success() {
        // Arrange
        Long idToDelete = operateur1.getIdOperateur();

        // Act
        operateurRepository.deleteById(idToDelete);
        entityManager.flush();

        // Assert
        Optional<Operateur> deleted = operateurRepository.findById(idToDelete);
        assertFalse(deleted.isPresent());
    }

    @Test
    void testDeleteAll_Success() {
        // Act
        operateurRepository.deleteAll();
        entityManager.flush();

        // Assert
        List<Operateur> operateurs = (List<Operateur>) operateurRepository.findAll();
        assertTrue(operateurs.isEmpty());
    }

    @Test
    void testCount_Success() {
        // Act
        long count = operateurRepository.count();

        // Assert
        assertEquals(2, count);
    }

    @Test
    void testExistsById_True() {
        // Act
        boolean exists = operateurRepository.existsById(operateur1.getIdOperateur());

        // Assert
        assertTrue(exists);
    }

    @Test
    void testExistsById_False() {
        // Act
        boolean exists = operateurRepository.existsById(999L);

        // Assert
        assertFalse(exists);
    }
}

