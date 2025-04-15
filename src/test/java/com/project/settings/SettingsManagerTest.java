package com.project.settings;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.project.ui.settings.SettingsManager;
import com.project.ui.settings.ValidationSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
/**
 * Unit tests for {@link SettingsManager}.
 * Validates the retrieval and storage of settings, including API key, model name, temperature, and token amount.
 *
 * @author Sara Moussa.
 */
class SettingsManagerTest {

    /**
     * Manages the storage, retrieval, and validation of user-specific settings
     * such as API key, model name, temperature, and token amount.
     */
    private SettingsManager settingsManager;

    /**
     * Mock object for the Project class, used in testing scenarios.
     */
    @Mock
    private Project mockProject;

    /**
     * Initializes the test environment before each test execution.
     */
    @BeforeEach
    void setUp() {
        settingsManager = new SettingsManager();
    }


    /**
     * Verifies that the cached API key is correctly returned when available.
     */
    @Test
    void shouldReturnCachedApiKeyWhenAvailable() {
        try {
            var field = SettingsManager.class.getDeclaredField("cachedApiKey");
            field.setAccessible(true);
            field.get(settingsManager).getClass().getMethod("set", Object.class).invoke(field.get(settingsManager), "test-key");

            assertEquals("test-key", settingsManager.fetchApiKey());
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        }
    }

    /**
     * Tests storing and retrieving a model name.
     */
    @Test
    void shouldStoreAndRetrieveModelName() {
        String modelName = "gpt-4-turbo";
        settingsManager.setModelName(modelName);
        assertEquals(modelName, settingsManager.getModelName());
    }

    /**
     * Verifies that the temperature can be stored and retrieved correctly.
     */
    @Test
    void shouldStoreAndRetrieveTemperature() {
        String temp = "0.7";
        settingsManager.setTemperature(temp);
        assertEquals(temp, settingsManager.getTemperature());
    }

    /**
     * Verifies that a token amount can be correctly stored and then retrieved.
     */
    @Test
    void shouldStoreAndRetrieveTokenAmount() {
        String tokens = "4000";
        settingsManager.setTokenAmount(tokens);
        assertEquals(tokens, settingsManager.getTokenAmount());
    }


    /**
     * Tests the validation of a correctly formatted temperature value.
     */
    @Test
    void shouldValidateCorrectTemperature() {
        settingsManager.setTemperature("0.7");

        ValidationSettings result = settingsManager.validateTemperature(mockProject);

        assertTrue(result.isValid());
        assertEquals(0.7, result.getValue());
    }


    /**
     * Verifies that validation correctly identifies a non-integer token amount as invalid.
     */
    @Test
    void shouldReportInvalidWhenTokenAmountNotInteger() {
        settingsManager.setTokenAmount("not-an-integer");

        try (MockedStatic<ApplicationManager> mockedAppManager = mockStatic(ApplicationManager.class)) {
            com.intellij.openapi.application.Application mockApplication = mock(com.intellij.openapi.application.Application.class);
            mockedAppManager.when(ApplicationManager::getApplication).thenReturn(mockApplication);
            doNothing().when(mockApplication).invokeLater(any(Runnable.class));

            ValidationSettings result = settingsManager.validateTokenAmount(mockProject);

            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("Invalid token amount"));
        }
    }
}