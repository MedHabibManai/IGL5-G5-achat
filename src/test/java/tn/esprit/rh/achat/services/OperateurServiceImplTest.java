package tn.esprit.rh.achat.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.rh.achat.entities.Operateur;
import tn.esprit.rh.achat.repositories.OperateurRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OperateurServiceImpl
 * Tests all CRUD operations for Operateur management
 */
@ExtendWith(MockitoExtension.class)
class OperateurServiceImplTest {

    @Mock
    private OperateurRepository operateurRepository;

    @InjectMocks
    private OperateurServiceImpl operateurService;

    private Operateur operateur1;
    private Operateur operateur2;

    @BeforeEach
    void setUp() {
        // Initialize test data
        operateur1 = new Operateur();
        operateur1.setIdOperateur(1L);
        operateur1.setNom("Slouma");
        operateur1.setPrenom("Rayen");
        operateur1.setPassword("password123");

        operateur2 = new Operateur();
        operateur2.setIdOperateur(2L);
        operateur2.setNom("Manai");
        operateur2.setPrenom("Habib");
        operateur2.setPassword("password456");
    }

    @Test
    void testRetrieveAllOperateurs_Success() {
        // Arrange
        List<Operateur> operateurs = new ArrayList<>();
        operateurs.add(operateur1);
        operateurs.add(operateur2);
        when(operateurRepository.findAll()).thenReturn(operateurs);

        // Act
        List<Operateur> result = operateurService.retrieveAllOperateurs();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Slouma", result.get(0).getNom());
        assertEquals("Manai", result.get(1).getNom());
        verify(operateurRepository, times(1)).findAll();
    }

    @Test
    void testRetrieveAllOperateurs_EmptyList() {
        // Arrange
        when(operateurRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        List<Operateur> result = operateurService.retrieveAllOperateurs();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(operateurRepository, times(1)).findAll();
    }

    @Test
    void testAddOperateur_Success() {
        // Arrange
        when(operateurRepository.save(any(Operateur.class))).thenReturn(operateur1);

        // Act
        Operateur result = operateurService.addOperateur(operateur1);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getIdOperateur());
        assertEquals("Slouma", result.getNom());
        assertEquals("Rayen", result.getPrenom());
        verify(operateurRepository, times(1)).save(operateur1);
    }

    @Test
    void testRetrieveOperateur_Success() {
        // Arrange
        when(operateurRepository.findById(1L)).thenReturn(Optional.of(operateur1));

        // Act
        Operateur result = operateurService.retrieveOperateur(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getIdOperateur());
        assertEquals("Slouma", result.getNom());
        assertEquals("Rayen", result.getPrenom());
        verify(operateurRepository, times(1)).findById(1L);
    }

    @Test
    void testRetrieveOperateur_NotFound() {
        // Arrange
        when(operateurRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Operateur result = operateurService.retrieveOperateur(999L);

        // Assert
        assertNull(result);
        verify(operateurRepository, times(1)).findById(999L);
    }

    @Test
    void testUpdateOperateur_Success() {
        // Arrange
        Operateur updatedOperateur = new Operateur();
        updatedOperateur.setIdOperateur(1L);
        updatedOperateur.setNom("Slouma Updated");
        updatedOperateur.setPrenom("Rayen Updated");
        updatedOperateur.setPassword("newpassword");

        when(operateurRepository.save(any(Operateur.class))).thenReturn(updatedOperateur);

        // Act
        Operateur result = operateurService.updateOperateur(updatedOperateur);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getIdOperateur());
        assertEquals("Slouma Updated", result.getNom());
        assertEquals("Rayen Updated", result.getPrenom());
        assertEquals("newpassword", result.getPassword());
        verify(operateurRepository, times(1)).save(updatedOperateur);
    }

    @Test
    void testDeleteOperateur_Success() {
        // Arrange
        doNothing().when(operateurRepository).deleteById(1L);

        // Act
        operateurService.deleteOperateur(1L);

        // Assert
        verify(operateurRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteOperateur_NonExistentId() {
        // Arrange
        doNothing().when(operateurRepository).deleteById(999L);

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> operateurService.deleteOperateur(999L));
        verify(operateurRepository, times(1)).deleteById(999L);
    }
}

