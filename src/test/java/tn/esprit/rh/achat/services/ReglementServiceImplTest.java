package tn.esprit.rh.achat.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.rh.achat.entities.Facture;
import tn.esprit.rh.achat.entities.Reglement;
import tn.esprit.rh.achat.repositories.FactureRepository;
import tn.esprit.rh.achat.repositories.ReglementRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReglementServiceImpl
 * Tests all operations for Reglement management including custom queries
 */
@ExtendWith(MockitoExtension.class)
class ReglementServiceImplTest {

    @Mock
    private ReglementRepository reglementRepository;

    @Mock
    private FactureRepository factureRepository;

    @InjectMocks
    private ReglementServiceImpl reglementService;

    private Reglement reglement1;
    private Reglement reglement2;
    private Facture facture;

    @BeforeEach
    void setUp() {
        // Initialize test facture
        facture = new Facture();
        facture.setIdFacture(1L);
        facture.setMontantFacture(1000f);
        facture.setArchivee(false);

        // Initialize test reglements
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.JANUARY, 15);

        reglement1 = new Reglement();
        reglement1.setIdReglement(1L);
        reglement1.setMontantPaye(500f);
        reglement1.setMontantRestant(500f);
        reglement1.setPayee(false);
        reglement1.setDateReglement(cal.getTime());
        reglement1.setFacture(facture);

        cal.set(2024, Calendar.FEBRUARY, 15);
        reglement2 = new Reglement();
        reglement2.setIdReglement(2L);
        reglement2.setMontantPaye(500f);
        reglement2.setMontantRestant(0f);
        reglement2.setPayee(true);
        reglement2.setDateReglement(cal.getTime());
        reglement2.setFacture(facture);
    }

    @Test
    void testRetrieveAllReglements_Success() {
        // Arrange
        List<Reglement> reglements = new ArrayList<>();
        reglements.add(reglement1);
        reglements.add(reglement2);
        when(reglementRepository.findAll()).thenReturn(reglements);

        // Act
        List<Reglement> result = reglementService.retrieveAllReglements();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(500f, result.get(0).getMontantPaye());
        assertEquals(500f, result.get(1).getMontantPaye());
        verify(reglementRepository, times(1)).findAll();
    }

    @Test
    void testRetrieveAllReglements_EmptyList() {
        // Arrange
        when(reglementRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        List<Reglement> result = reglementService.retrieveAllReglements();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(reglementRepository, times(1)).findAll();
    }

    @Test
    void testAddReglement_Success() {
        // Arrange
        when(reglementRepository.save(any(Reglement.class))).thenReturn(reglement1);

        // Act
        Reglement result = reglementService.addReglement(reglement1);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getIdReglement());
        assertEquals(500f, result.getMontantPaye());
        assertEquals(500f, result.getMontantRestant());
        assertFalse(result.getPayee());
        verify(reglementRepository, times(1)).save(reglement1);
    }

    @Test
    void testRetrieveReglement_Success() {
        // Arrange
        when(reglementRepository.findById(1L)).thenReturn(Optional.of(reglement1));

        // Act
        Reglement result = reglementService.retrieveReglement(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getIdReglement());
        assertEquals(500f, result.getMontantPaye());
        verify(reglementRepository, times(1)).findById(1L);
    }

    @Test
    void testRetrieveReglement_NotFound() {
        // Arrange
        when(reglementRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Reglement result = reglementService.retrieveReglement(999L);

        // Assert
        assertNull(result);
        verify(reglementRepository, times(1)).findById(999L);
    }

    @Test
    void testRetrieveReglementByFacture_Success() {
        // Arrange
        List<Reglement> reglements = new ArrayList<>();
        reglements.add(reglement1);
        reglements.add(reglement2);
        when(reglementRepository.retrieveReglementByFacture(1L)).thenReturn(reglements);

        // Act
        List<Reglement> result = reglementService.retrieveReglementByFacture(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getFacture().getIdFacture());
        assertEquals(1L, result.get(1).getFacture().getIdFacture());
        verify(reglementRepository, times(1)).retrieveReglementByFacture(1L);
    }

    @Test
    void testRetrieveReglementByFacture_NoReglements() {
        // Arrange
        when(reglementRepository.retrieveReglementByFacture(999L)).thenReturn(new ArrayList<>());

        // Act
        List<Reglement> result = reglementService.retrieveReglementByFacture(999L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(reglementRepository, times(1)).retrieveReglementByFacture(999L);
    }

    @Test
    void testGetChiffreAffaireEntreDeuxDate_Success() {
        // Arrange
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.JANUARY, 1);
        Date startDate = cal.getTime();
        cal.set(2024, Calendar.DECEMBER, 31);
        Date endDate = cal.getTime();

        when(reglementRepository.getChiffreAffaireEntreDeuxDate(startDate, endDate)).thenReturn(1000f);

        // Act
        float result = reglementService.getChiffreAffaireEntreDeuxDate(startDate, endDate);

        // Assert
        assertEquals(1000f, result, 0.01f);
        verify(reglementRepository, times(1)).getChiffreAffaireEntreDeuxDate(startDate, endDate);
    }

    @Test
    void testGetChiffreAffaireEntreDeuxDate_NoReglements() {
        // Arrange
        Calendar cal = Calendar.getInstance();
        cal.set(2023, Calendar.JANUARY, 1);
        Date startDate = cal.getTime();
        cal.set(2023, Calendar.DECEMBER, 31);
        Date endDate = cal.getTime();

        when(reglementRepository.getChiffreAffaireEntreDeuxDate(startDate, endDate)).thenReturn(0f);

        // Act
        float result = reglementService.getChiffreAffaireEntreDeuxDate(startDate, endDate);

        // Assert
        assertEquals(0f, result, 0.01f);
        verify(reglementRepository, times(1)).getChiffreAffaireEntreDeuxDate(startDate, endDate);
    }
}

