package com.neviswealth.searchapi.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Captor
    private ArgumentCaptor<ClientEntity> clientCaptor;

    private ClientService clientService;

    @BeforeEach
    void setUp() {
        clientService = new ClientService(clientRepository);
    }

    @Test
    void blankFirstNameThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> clientService.create("   ", "Doe", "john@example.com", "desc", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("firstName must not be blank");
    }

    @Test
    void blankEmailThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> clientService.create("John", "Doe", "   ", "desc", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email must not be blank");
    }

    @Test
    void duplicateEmailThrowsDuplicateEmailException() {
        when(clientRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> clientService.create("John", "Doe", " John@Example.com ", "desc", null))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("Client with email already exists: john@example.com");
    }

    @Test
    void validInputSavesNormalizedEmailAndDefaultsSocialLinksToEmptyList() {
        when(clientRepository.existsByEmail("john@example.com")).thenReturn(false);

        ClientEntity created = clientService.create("John", "Doe", " John@Example.com ", "desc", null);

        verify(clientRepository).save(clientCaptor.capture());
        ClientEntity saved = clientCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("john@example.com");
        assertThat(saved.getSocialLinks()).isEmpty();
        assertThat(saved.getId()).isNotNull();

        assertThat(created).isSameAs(saved);
    }
}
