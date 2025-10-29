package tn.esprit.rh.achat.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.rh.achat.entities.Operateur;
import tn.esprit.rh.achat.services.IOperateurService;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for OperateurController
 * Tests all REST endpoints for Operateur management
 */
@WebMvcTest(OperateurController.class)
class OperateurControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IOperateurService operateurService;

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
    void testGetOperateurs_Success() throws Exception {
        // Arrange
        List<Operateur> operateurs = new ArrayList<>();
        operateurs.add(operateur1);
        operateurs.add(operateur2);
        when(operateurService.retrieveAllOperateurs()).thenReturn(operateurs);

        // Act & Assert
        mockMvc.perform(get("/operateur/retrieve-all-operateurs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].idOperateur", is(1)))
                .andExpect(jsonPath("$[0].nom", is("Slouma")))
                .andExpect(jsonPath("$[0].prenom", is("Rayen")))
                .andExpect(jsonPath("$[1].idOperateur", is(2)))
                .andExpect(jsonPath("$[1].nom", is("Manai")));

        verify(operateurService, times(1)).retrieveAllOperateurs();
    }

    @Test
    void testGetOperateurs_EmptyList() throws Exception {
        // Arrange
        when(operateurService.retrieveAllOperateurs()).thenReturn(new ArrayList<>());

        // Act & Assert
        mockMvc.perform(get("/operateur/retrieve-all-operateurs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(operateurService, times(1)).retrieveAllOperateurs();
    }

    @Test
    void testRetrieveOperateur_Success() throws Exception {
        // Arrange
        when(operateurService.retrieveOperateur(1L)).thenReturn(operateur1);

        // Act & Assert
        mockMvc.perform(get("/operateur/retrieve-operateur/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.idOperateur", is(1)))
                .andExpect(jsonPath("$.nom", is("Slouma")))
                .andExpect(jsonPath("$.prenom", is("Rayen")));

        verify(operateurService, times(1)).retrieveOperateur(1L);
    }

    @Test
    void testRetrieveOperateur_NotFound() throws Exception {
        // Arrange
        when(operateurService.retrieveOperateur(999L)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/operateur/retrieve-operateur/999"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(operateurService, times(1)).retrieveOperateur(999L);
    }

    @Test
    void testAddOperateur_Success() throws Exception {
        // Arrange
        Operateur newOperateur = new Operateur();
        newOperateur.setNom("Test");
        newOperateur.setPrenom("User");
        newOperateur.setPassword("testpass");

        Operateur savedOperateur = new Operateur();
        savedOperateur.setIdOperateur(3L);
        savedOperateur.setNom("Test");
        savedOperateur.setPrenom("User");
        savedOperateur.setPassword("testpass");

        when(operateurService.addOperateur(any(Operateur.class))).thenReturn(savedOperateur);

        // Act & Assert
        mockMvc.perform(post("/operateur/add-operateur")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newOperateur)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.idOperateur", is(3)))
                .andExpect(jsonPath("$.nom", is("Test")))
                .andExpect(jsonPath("$.prenom", is("User")));

        verify(operateurService, times(1)).addOperateur(any(Operateur.class));
    }

    @Test
    void testModifyOperateur_Success() throws Exception {
        // Arrange
        Operateur updatedOperateur = new Operateur();
        updatedOperateur.setIdOperateur(1L);
        updatedOperateur.setNom("Updated Name");
        updatedOperateur.setPrenom("Updated Prenom");
        updatedOperateur.setPassword("newpass");

        when(operateurService.updateOperateur(any(Operateur.class))).thenReturn(updatedOperateur);

        // Act & Assert
        mockMvc.perform(put("/operateur/modify-operateur")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedOperateur)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.idOperateur", is(1)))
                .andExpect(jsonPath("$.nom", is("Updated Name")))
                .andExpect(jsonPath("$.prenom", is("Updated Prenom")));

        verify(operateurService, times(1)).updateOperateur(any(Operateur.class));
    }

    @Test
    void testRemoveOperateur_Success() throws Exception {
        // Arrange
        doNothing().when(operateurService).deleteOperateur(1L);

        // Act & Assert
        mockMvc.perform(delete("/operateur/remove-operateur/1"))
                .andExpect(status().isOk());

        verify(operateurService, times(1)).deleteOperateur(1L);
    }

    @Test
    void testRemoveOperateur_NonExistentId() throws Exception {
        // Arrange
        doNothing().when(operateurService).deleteOperateur(999L);

        // Act & Assert
        mockMvc.perform(delete("/operateur/remove-operateur/999"))
                .andExpect(status().isOk());

        verify(operateurService, times(1)).deleteOperateur(999L);
    }
}

