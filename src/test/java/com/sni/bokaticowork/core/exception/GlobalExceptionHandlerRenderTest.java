package com.sni.bokaticowork.core.exception;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Une erreur metier se rend en JSON par le gestionnaire global, sans passer par /error.
 */
class GlobalExceptionHandlerRenderTest {

    @RestController
    static class Failing {
        @GetMapping("/missing")
        String missing() {
            throw new ResourceNotFoundException("Booking not found");
        }

        @GetMapping("/denied")
        String denied() {
            throw new org.springframework.security.authorization.AuthorizationDeniedException("Access Denied");
        }
    }

    @Test
    @DisplayName("Refus d'une methode protegee : 403, pas 500")
    void methodDenialIsForbidden() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new Failing())
                .setControllerAdvice(new GlobalExceptionHandler(new ErrorResponse()))
                .build();

        mvc.perform(get("/denied").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Introuvable : 404 et corps JSON du gestionnaire")
    void notFoundIsRendered() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new Failing())
                .setControllerAdvice(new GlobalExceptionHandler(new ErrorResponse()))
                .build();

        mvc.perform(get("/missing").accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
