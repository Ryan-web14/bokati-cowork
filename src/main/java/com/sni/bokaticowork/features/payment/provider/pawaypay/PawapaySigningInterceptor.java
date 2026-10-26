package com.sni.bokaticowork.features.payment.provider.pawaypay;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Set;

/**
 * Pose la signature PawaPay sur chaque requete qui porte un corps, juste avant l'envoi.
 *
 * <p>Ici et pas plus tot · l'intercepteur voit les octets exacts du corps et l'URL reelle,
 * apres serialisation. Signer un objet avant sa serialisation donnerait une empreinte qui ne
 * correspond pas toujours a ce qui part, et PawaPay refuserait la requete.</p>
 *
 * <p>Les lectures de statut ({@code GET}) n'ont pas de corps et ne sont pas des appels financiers ·
 * elles partent sans signature, comme PawaPay le prevoit.</p>
 */
class PawapaySigningInterceptor implements ClientHttpRequestInterceptor {

    private static final Set<HttpMethod> SIGNED = Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH);

    private final PawapayRequestSigner signer;

    PawapaySigningInterceptor(PawapayRequestSigner signer) {
        this.signer = signer;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        if (SIGNED.contains(request.getMethod())) {
            MediaType contentType = request.getHeaders().getContentType();
            signer.sign(request.getMethod().name(), request.getURI(),
                            contentType == null ? null : contentType.toString(), body)
                    .forEach((name, value) -> request.getHeaders().set(name, value));
        }
        return execution.execute(request, body);
    }
}
