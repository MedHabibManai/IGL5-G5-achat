package tn.esprit.rh.achat.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.rh.achat.entities.Facture;
import tn.esprit.rh.achat.services.IFactureService;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FactureRestController.class)
class FactureRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IFactureService factureService;

    @Autowired
    private ObjectMapper objectMapper;

    private Facture facture;
    private List<Facture> factureList;

    @BeforeEach
    void setUp() {
        facture = new Facture();
        facture.setIdFacture(1L);
        facture.setMontantFacture(1000.0f);
        facture.setMontantRemise(100.0f);
        facture.setArchivee(false);
        facture.setDateCreationFacture(new Date());
        facture.setDateDerniereModificationFacture(new Date());

        Facture facture2 = new Facture();
        facture2.setIdFacture(2L);
        facture2.setMontantFacture(2000.0f);
        facture2.setMontantRemise(200.0f);
        facture2.setArchivee(false);

        factureList = Arrays.asList(facture, facture2);
    }

    @Test
    void testGetFactures_Success() throws Exception {
        // Arrange
        when(factureService.retrieveAllFactures()).thenReturn(factureList);

        // Act & Assert
        mockMvc.perform(get("/facture/retrieve-all-factures"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].idFacture").value(1))
                .andExpect(jsonPath("$[0].montantFacture").value(1000.0))
                .andExpect(jsonPath("$[1].idFacture").value(2))
                .andExpect(jsonPath("$[1].montantFacture").value(2000.0));

        verify(factureService, times(1)).retrieveAllFactures();
    }

    @Test
    void testGetFactures_EmptyList() throws Exception {
        // Arrange
        when(factureService.retrieveAllFactures()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/facture/retrieve-all-factures"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(factureService, times(1)).retrieveAllFactures();
    }

    @Test
    void testRetrieveFacture_Success() throws Exception {
        // Arrange
        when(factureService.retrieveFacture(1L)).thenReturn(facture);

        // Act & Assert
        mockMvc.perform(get("/facture/retrieve-facture/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.idFacture").value(1))
                .andExpect(jsonPath("$.montantFacture").value(1000.0))
                .andExpect(jsonPath("$.montantRemise").value(100.0))
                .andExpect(jsonPath("$.archivee").value(false));

        verify(factureService, times(1)).retrieveFacture(1L);
    }

    @Test
    void testRetrieveFacture_NotFound() throws Exception {
        // Arrange
        when(factureService.retrieveFacture(999L)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/facture/retrieve-facture/999"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(factureService, times(1)).retrieveFacture(999L);
    }

    @Test
    void testAddFacture_Success() throws Exception {
        // Arrange
        when(factureService.addFacture(any(Facture.class))).thenReturn(facture);

        // Act & Assert
        mockMvc.perform(post("/facture/add-facture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(facture)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.idFacture").value(1))
                .andExpect(jsonPath("$.montantFacture").value(1000.0));

        verify(factureService, times(1)).addFacture(any(Facture.class));
    }

    @Test
    void testCancelFacture_Success() throws Exception {
        // Arrange
        doNothing().when(factureService).cancelFacture(1L);

        // Act & Assert
        mockMvc.perform(put("/facture/cancel-facture/1"))
                .andExpect(status().isOk());

        verify(factureService, times(1)).cancelFacture(1L);
    }

    @Test
    void testGetFactureByFournisseur_Success() throws Exception {
        // Arrange
        when(factureService.getFacturesByFournisseur(1L)).thenReturn(factureList);

        // Act & Assert
        mockMvc.perform(get("/facture/getFactureByFournisseur/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].idFacture").value(1))
                .andExpect(jsonPath("$[1].idFacture").value(2));

        verify(factureService, times(1)).getFacturesByFournisseur(1L);
    }

    @Test
    void testGetFactureByFournisseur_EmptyList() throws Exception {
        // Arrange
        when(factureService.getFacturesByFournisseur(999L)).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/facture/getFactureByFournisseur/999"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(factureService, times(1)).getFacturesByFournisseur(999L);
    }

    @Test
    void testAssignOperateurToFacture_Success() throws Exception {
        // Arrange
        doNothing().when(factureService).assignOperateurToFacture(1L, 1L);

        // Act & Assert
        mockMvc.perform(put("/facture/assignOperateurToFacture/1/1"))
                .andExpect(status().isOk());

        verify(factureService, times(1)).assignOperateurToFacture(1L, 1L);
    }

    @Test
    void testPourcentageRecouvrement_Success() throws Exception {
        // Arrange
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date startDate = sdf.parse("2024-01-01");
        Date endDate = sdf.parse("2024-12-31");
        float expectedPercentage = 75.5f;

        when(factureService.pourcentageRecouvrement(any(Date.class), any(Date.class)))
                .thenReturn(expectedPercentage);

        // Act & Assert
        mockMvc.perform(get("/facture/pourcentageRecouvrement/2024-01-01/2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(String.valueOf(expectedPercentage)));

        verify(factureService, times(1)).pourcentageRecouvrement(any(Date.class), any(Date.class));
    }

    @Test
    void testPourcentageRecouvrement_Exception() throws Exception {
        // Arrange
        when(factureService.pourcentageRecouvrement(any(Date.class), any(Date.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/facture/pourcentageRecouvrement/2024-01-01/2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(content().string("0.0"));

        verify(factureService, times(1)).pourcentageRecouvrement(any(Date.class), any(Date.class));
    }

    @Test
    void testPourcentageRecouvrement_InvalidDateFormat() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/facture/pourcentageRecouvrement/invalid-date/2024-12-31"))
                .andExpect(status().isBadRequest());

        verify(factureService, never()).pourcentageRecouvrement(any(Date.class), any(Date.class));
    }
}

