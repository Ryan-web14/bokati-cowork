package com.sni.bokaticowork.features.booking.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.config.BookingCheckInProperties;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import com.sni.bokaticowork.features.booking.service.support.BookingConfirmationDocumentService;
import com.sni.bokaticowork.features.booking.service.support.ScannerGrantCodec;
import com.sni.bokaticowork.features.booking.service.support.ScannerTerminalGate;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Le pointage ne se declenche plus par l'ouverture d'un lien.
 *
 * <p>Il se faisait sur un GET, avec pour seule autorite un cookie envoye a tout le domaine. Une
 * page quelconque visitee par le terminal de l'accueil pouvait donc enregistrer des arrivees avec
 * un {@code <img src="...">}, la protection CSRF etant desactivee globalement. Le GET affiche
 * desormais une confirmation, et seul un POST pointe.</p>
 */
class PublicBookingCheckInTest {

    private static final String KEY = "cle-de-poste-partagee-0123456789";
    private static final String TOKEN = "jeton-de-pointage";

    private BookingService bookingService;
    private BookingCheckInProperties properties;
    private ScannerGrantCodec codec;
    private PublicBookingController controller;

    @BeforeEach
    void setUp() {
        bookingService = mock(BookingService.class);
        properties = new BookingCheckInProperties();
        properties.setScannerKey(KEY);
        codec = new ScannerGrantCodec(properties);
        controller = new PublicBookingController(
                bookingService,
                mock(BookingConfirmationDocumentService.class),
                properties,
                new ScannerTerminalGate(properties, codec));
    }

    private MockHttpServletRequest authorizedTerminal() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("bokati_checkin_grant", codec.issue(Instant.now())));
        return request;
    }

    @Test
    @DisplayName("Ouvrir le lien du QR code ne pointe personne · c'est la faille corrigee")
    void openingTheScanLinkDoesNotCheckAnyoneIn() {
        ModelAndView mav = controller.scanCheckIn(TOKEN, authorizedTerminal());

        verifyNoInteractions(bookingService);
        assertThat(mav.getViewName()).isEqualTo("booking/checkin-confirm");
        assertThat(mav.getModel()).containsEntry("checkInToken", TOKEN);
    }

    @Test
    @DisplayName("La page de confirmation pointe vers la route POST, pas vers le lien ouvert")
    void confirmationPageTargetsThePostRoute() {
        ModelAndView mav = controller.scanCheckIn(TOKEN, authorizedTerminal());

        assertThat(mav.getModel())
                .containsEntry("confirmUrl", ApiPath.V1 + "/public/bookings/check-in/scan");
    }

    @Test
    @DisplayName("Un poste non autorise se voit demander la cle, et rien n'est pointe")
    void unauthorizedTerminalIsAskedForTheKey() {
        ModelAndView mav = controller.scanCheckIn(TOKEN, new MockHttpServletRequest());

        verifyNoInteractions(bookingService);
        assertThat(mav.getViewName()).isEqualTo("booking/checkin-admin-code");
        assertThat(mav.getModel())
                .containsEntry("verifyUrl", ApiPath.V1 + "/public/bookings/check-in/scanner-verify");
    }

    @Test
    @DisplayName("Le POST confirme pointe")
    void postConfirmsAndChecksIn() {
        controller.confirmScanCheckIn(TOKEN, authorizedTerminal());

        verify(bookingService, times(1)).checkInByToken(eq(TOKEN), any());
    }

    @Test
    @DisplayName("Le POST sans autorisation de poste ne pointe pas")
    void postWithoutAuthorizationDoesNotCheckIn() {
        ModelAndView mav = controller.confirmScanCheckIn(TOKEN, new MockHttpServletRequest());

        verifyNoInteractions(bookingService);
        assertThat(mav.getViewName()).isEqualTo("booking/checkin-admin-code");
    }

    @Test
    @DisplayName("Un cookie de l'ancienne version ne pointe plus · il portait la cle")
    void legacyCookieNoLongerChecksIn() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("bokati_scanner", KEY));

        controller.confirmScanCheckIn(TOKEN, request);

        verifyNoInteractions(bookingService);
    }

    @Test
    @DisplayName("La bonne cle autorise le poste et pointe dans la meme requete")
    void correctKeyAuthorizesAndChecksIn() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.scannerVerify(TOKEN, KEY, response);

        verify(bookingService, times(1)).checkInByToken(eq(TOKEN), any());
        assertThat(response.getHeaders("Set-Cookie"))
                .anyMatch(header -> header.startsWith("bokati_checkin_grant="));
    }

    @Test
    @DisplayName("Une cle erronee n'autorise rien et ne pointe rien")
    void wrongKeyAuthorizesNothing() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ModelAndView mav = controller.scannerVerify(TOKEN, "mauvaise-cle", response);

        verify(bookingService, never()).checkInByToken(any(), any());
        assertThat(mav.getViewName()).isEqualTo("booking/checkin-admin-code");
        assertThat(mav.getModel()).containsKey("error");
        assertThat(response.getHeaders("Set-Cookie"))
                .noneMatch(header -> header.startsWith("bokati_checkin_grant="));
    }

    @Test
    @DisplayName("L'activation d'un poste pose l'autorisation et dit quand elle expire")
    void setupAuthorizesAndAnnouncesTheExpiry() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ModelAndView mav = controller.scannerSetup(KEY, response);

        assertThat(mav.getModel()).containsEntry("setupSuccess", true);
        assertThat(mav.getModel()).containsEntry("grantValidityDays", 30L);
        assertThat(response.getHeaders("Set-Cookie"))
                .anyMatch(header -> header.startsWith("bokati_checkin_grant="));
    }

    @Test
    @DisplayName("Les deux formulaires visent des routes qui existent")
    void formActionsPointAtRealRoutes() {
        // Regression : les gabarits visaient /v1/public/bookings/... alors que le prefixe est
        // /sni/api/v1 · les deux formulaires du poste de pointage tombaient en 404, donc aucun
        // poste ne pouvait etre active.
        String verifyUrl = (String) controller.scanCheckIn(TOKEN, new MockHttpServletRequest())
                .getModel().get("verifyUrl");
        String setupUrl = (String) controller.scannerSetupPage().getModel().get("setupUrl");

        assertThat(verifyUrl).startsWith(ApiPath.V1 + "/public/bookings/");
        assertThat(setupUrl).startsWith(ApiPath.V1 + "/public/bookings/");
    }
}
