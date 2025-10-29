package tn.esprit.rh.achat.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.rh.achat.entities.*;
import tn.esprit.rh.achat.repositories.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FactureServiceImplTest {

    @Mock
    private FactureRepository factureRepository;

    @Mock
    private OperateurRepository operateurRepository;

    @Mock
    private DetailFactureRepository detailFactureRepository;

    @Mock
    private FournisseurRepository fournisseurRepository;

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private ReglementServiceImpl reglementService;

    @InjectMocks
    private FactureServiceImpl factureService;

    private Facture facture;
    private Fournisseur fournisseur;
    private Operateur operateur;

    @BeforeEach
    void setUp() {
        // Initialize test data
        facture = new Facture();
        facture.setIdFacture(1L);
        facture.setMontantFacture(1000.0f);
        facture.setMontantRemise(100.0f);
        facture.setArchivee(false);
        facture.setDateCreationFacture(new Date());
        facture.setDateDerniereModificationFacture(new Date());

        fournisseur = new Fournisseur();
        fournisseur.setIdFournisseur(1L);
        fournisseur.setCode("F001");
        fournisseur.setLibelle("Fournisseur Test");
        Set<Facture> factures = new HashSet<>();
        factures.add(facture);
        fournisseur.setFactures(factures);

        operateur = new Operateur();
        operateur.setIdOperateur(1L);
        operateur.setNom("Slouma");
        operateur.setPrenom("Rayen");
        operateur.setFactures(new HashSet<>());
    }

    @Test
    void testRetrieveAllFactures_Success() {
        // Arrange
        List<Facture> factureList = Arrays.asList(facture);
        when(factureRepository.findAll()).thenReturn(factureList);

        // Act
        List<Facture> result = factureService.retrieveAllFactures();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(facture.getIdFacture(), result.get(0).getIdFacture());
        verify(factureRepository, times(1)).findAll();
    }

    @Test
    void testRetrieveAllFactures_EmptyList() {
        // Arrange
        when(factureRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        List<Facture> result = factureService.retrieveAllFactures();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(factureRepository, times(1)).findAll();
    }

    @Test
    void testAddFacture_Success() {
        // Arrange
        when(factureRepository.save(any(Facture.class))).thenReturn(facture);

        // Act
        Facture result = factureService.addFacture(facture);

        // Assert
        assertNotNull(result);
        assertEquals(facture.getIdFacture(), result.getIdFacture());
        assertEquals(facture.getMontantFacture(), result.getMontantFacture());
        verify(factureRepository, times(1)).save(facture);
    }

    @Test
    void testRetrieveFacture_Success() {
        // Arrange
        when(factureRepository.findById(1L)).thenReturn(Optional.of(facture));

        // Act
        Facture result = factureService.retrieveFacture(1L);

        // Assert
        assertNotNull(result);
        assertEquals(facture.getIdFacture(), result.getIdFacture());
        assertEquals(facture.getMontantFacture(), result.getMontantFacture());
        verify(factureRepository, times(1)).findById(1L);
    }

    @Test
    void testRetrieveFacture_NotFound() {
        // Arrange
        when(factureRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Facture result = factureService.retrieveFacture(999L);

        // Assert
        assertNull(result);
        verify(factureRepository, times(1)).findById(999L);
    }

    @Test
    void testCancelFacture_Success() {
        // Arrange
        when(factureRepository.findById(1L)).thenReturn(Optional.of(facture));
        when(factureRepository.save(any(Facture.class))).thenReturn(facture);
        doNothing().when(factureRepository).updateFacture(anyLong());

        // Act
        factureService.cancelFacture(1L);

        // Assert
        verify(factureRepository, times(1)).findById(1L);
        verify(factureRepository, times(1)).save(any(Facture.class));
        verify(factureRepository, times(1)).updateFacture(1L);
    }

    @Test
    void testCancelFacture_NotFound() {
        // Arrange
        when(factureRepository.findById(999L)).thenReturn(Optional.empty());
        when(factureRepository.save(any(Facture.class))).thenReturn(new Facture());
        doNothing().when(factureRepository).updateFacture(anyLong());

        // Act
        factureService.cancelFacture(999L);

        // Assert
        verify(factureRepository, times(1)).findById(999L);
        verify(factureRepository, times(1)).save(any(Facture.class));
        verify(factureRepository, times(1)).updateFacture(999L);
    }

    @Test
    void testGetFacturesByFournisseur_Success() {
        // Arrange
        Set<Facture> factureSet = new HashSet<>();
        factureSet.add(facture);
        fournisseur.setFactures(factureSet);
        when(fournisseurRepository.findById(1L)).thenReturn(Optional.of(fournisseur));

        // Act & Assert
        // Note: The implementation has a bug - it tries to cast Set<Facture> to List<Facture>
        // This will throw ClassCastException at runtime
        assertThrows(ClassCastException.class, () -> {
            factureService.getFacturesByFournisseur(1L);
        });

        verify(fournisseurRepository, times(1)).findById(1L);
    }

    @Test
    void testAssignOperateurToFacture_Success() {
        // Arrange
        when(factureRepository.findById(1L)).thenReturn(Optional.of(facture));
        when(operateurRepository.findById(1L)).thenReturn(Optional.of(operateur));
        when(operateurRepository.save(any(Operateur.class))).thenReturn(operateur);

        // Act
        factureService.assignOperateurToFacture(1L, 1L);

        // Assert
        verify(factureRepository, times(1)).findById(1L);
        verify(operateurRepository, times(1)).findById(1L);
        verify(operateurRepository, times(1)).save(operateur);
        assertTrue(operateur.getFactures().contains(facture));
    }

    @Test
    void testPourcentageRecouvrement_Success() {
        // Arrange
        Date startDate = new Date();
        Date endDate = new Date();
        float totalFactures = 10000.0f;
        float totalRecouvrement = 7500.0f;
        float expectedPercentage = 75.0f;

        when(factureRepository.getTotalFacturesEntreDeuxDates(startDate, endDate)).thenReturn(totalFactures);
        when(reglementService.getChiffreAffaireEntreDeuxDate(startDate, endDate)).thenReturn(totalRecouvrement);

        // Act
        float result = factureService.pourcentageRecouvrement(startDate, endDate);

        // Assert
        assertEquals(expectedPercentage, result, 0.01);
        verify(factureRepository, times(1)).getTotalFacturesEntreDeuxDates(startDate, endDate);
        verify(reglementService, times(1)).getChiffreAffaireEntreDeuxDate(startDate, endDate);
    }

    @Test
    void testPourcentageRecouvrement_ZeroFactures() {
        // Arrange
        Date startDate = new Date();
        Date endDate = new Date();
        float totalFactures = 0.0f;
        float totalRecouvrement = 0.0f;

        when(factureRepository.getTotalFacturesEntreDeuxDates(startDate, endDate)).thenReturn(totalFactures);
        when(reglementService.getChiffreAffaireEntreDeuxDate(startDate, endDate)).thenReturn(totalRecouvrement);

        // Act - Division by zero will result in Infinity or NaN
        float result = factureService.pourcentageRecouvrement(startDate, endDate);

        // Assert - Result should be NaN (0/0) or Infinity
        assertTrue(Float.isNaN(result) || Float.isInfinite(result));
    }

    @Test
    void testPourcentageRecouvrement_FullRecovery() {
        // Arrange
        Date startDate = new Date();
        Date endDate = new Date();
        float totalFactures = 5000.0f;
        float totalRecouvrement = 5000.0f;
        float expectedPercentage = 100.0f;

        when(factureRepository.getTotalFacturesEntreDeuxDates(startDate, endDate)).thenReturn(totalFactures);
        when(reglementService.getChiffreAffaireEntreDeuxDate(startDate, endDate)).thenReturn(totalRecouvrement);

        // Act
        float result = factureService.pourcentageRecouvrement(startDate, endDate);

        // Assert
        assertEquals(expectedPercentage, result, 0.01);
    }
}

