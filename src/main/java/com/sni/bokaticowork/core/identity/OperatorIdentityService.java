package com.sni.bokaticowork.core.identity;

import com.sni.bokaticowork.core.baseClasses.model.Address;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Source unique de l'identite de l'exploitant pour les pieces imprimees.
 *
 * <p>Lit la fiche exploitant et la garde une minute. La fiche change rarement, mais une piece
 * se genere souvent (lots de factures, rapports) · la relire a chaque PDF couterait une requete
 * et une jointure d'adresse pour rien. Une modification de la fiche invalide le cache
 * immediatement ({@link OperatorIdentityChangeListener}) ; la duree d'une minute couvre les
 * chemins qui ne passent pas par l'entite (suppression logique en requete directe).</p>
 *
 * <p>Ne leve jamais · une base indisponible ou une fiche absente donne l'identite par defaut,
 * pour que l'emission d'une piece ne depende pas de la fiche.</p>
 */
@Slf4j
@Service
public class OperatorIdentityService {

    static final Duration TTL = Duration.ofMinutes(1);

    /** Incremente a chaque modification de la fiche · un cache plus ancien est relu. */
    private static final AtomicLong GENERATION = new AtomicLong();

    private final BusinessRepository businessRepository;
    private final Clock clock;

    private volatile Cached cached;

    @org.springframework.beans.factory.annotation.Autowired
    public OperatorIdentityService(BusinessRepository businessRepository) {
        this(businessRepository, Clock.systemUTC());
    }

    OperatorIdentityService(BusinessRepository businessRepository, Clock clock) {
        this.businessRepository = businessRepository;
        this.clock = clock;
    }

    /** Signale une modification de la fiche · la prochaine lecture repart de la base. */
    public static void invalidate() {
        GENERATION.incrementAndGet();
    }

    /** Identite courante, completee par les valeurs par defaut. */
    @Transactional(readOnly = true)
    public OperatorIdentity current() {
        Cached snapshot = cached;
        Instant now = clock.instant();
        long generation = GENERATION.get();
        if (snapshot != null && snapshot.generation() == generation && now.isBefore(snapshot.expiresAt())) {
            return snapshot.identity();
        }
        OperatorIdentity identity = load();
        cached = new Cached(identity, now.plus(TTL), generation);
        return identity;
    }

    private OperatorIdentity load() {
        try {
            return businessRepository.findFirstByOperatorTrueAndDeletedFalse()
                    .map(OperatorIdentityService::fromBusiness)
                    .map(OperatorIdentity::withDefaults)
                    .orElseGet(OperatorIdentity::defaults);
        } catch (RuntimeException ex) {
            log.warn("Fiche exploitant illisible · identite par defaut sur les pieces : {}", ex.getMessage());
            return OperatorIdentity.defaults();
        }
    }

    static OperatorIdentity fromBusiness(BusinessEntity business) {
        Address address = business.getAddress();
        return new OperatorIdentity(
                business.getName(),
                business.getLegalForm(),
                business.getNiuNumber(),
                business.getRccmNumber(),
                formatAddress(address),
                address != null ? address.getCity() : null,
                business.getPhone(),
                business.getEmail(),
                business.getLegalRepresentativeName(),
                business.getLegalRepresentativeTitle());
    }

    /** Meme mise en forme que les contrats · numero, rue, quartier, ville. */
    static String formatAddress(Address address) {
        if (address == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(address.getStreetNumber())) sb.append(address.getStreetNumber().trim()).append(" ");
        if (StringUtils.hasText(address.getStreetName())) sb.append(address.getStreetName().trim()).append(", ");
        if (StringUtils.hasText(address.getDistrict())) sb.append(address.getDistrict().trim()).append(", ");
        if (StringUtils.hasText(address.getCity())) sb.append(address.getCity().trim());
        String result = sb.toString().replaceAll(",\\s*$", "").trim();
        return result.isEmpty() ? null : result;
    }

    private record Cached(OperatorIdentity identity, Instant expiresAt, long generation) {
    }
}
