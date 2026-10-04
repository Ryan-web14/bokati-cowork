package com.sni.bokaticowork.core.generator.id;


import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generateur d'identifiants de type Snowflake · horodatage, identifiant de machine, sequence.
 *
 * <p>C'est la cle primaire de 258 entites, via {@code @IdGeneration}. L'unicite ne tient donc pas
 * a une contrainte de base mais a ce calcul, et elle repose entierement sur le fait que deux
 * instances qui tournent en meme temps portent des identifiants de machine differents.</p>
 *
 * <p>Ce n'etait pas le cas · l'identifiant de machine derivait de {@code user.name}, qui est le
 * meme sur tous les dynos Heroku. Les journaux de production le montraient directement :
 * {@code machine ID: 444} sur {@code web.1} <b>et</b> sur {@code web.2}. Chaque instance tenant son
 * compteur de sequence en memoire et le remettant a zero a chaque milliseconde, deux instances qui
 * inserent dans la meme table a la meme milliseconde produisaient <b>la meme cle primaire</b> ·
 * violation d'unicite, donc echec d'ecriture, en apparence aleatoire.</p>
 */
@Slf4j
public class GeneratorOfId implements IdentifierGenerator {

    //Custom epoch in bits
    private static long CUSTOM_EPOCH = 49852800000L; //this 30-07-2025

    //Bit allocation
    //12 bits
    private static int SEQUENCE_BITS = 12;
    //10 bits
    private static int MACHINE_ID_BITS = 10;

    //MAXIMUM VALUE
    private static long MAX_MACHINE_ID = (1L << MACHINE_ID_BITS) - 1; // 1023
    private static long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1; // 4095

    //Bit shift
    private static int MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static int TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;

    /** Reglage explicite · c'est lui qui rend l'unicite certaine plutot que probable. */
    static final String MACHINE_ID_VARIABLE = "ID_GENERATOR_MACHINE_ID";
    /** Sur Heroku, {@code web.1}, {@code web.2}, {@code worker.1} · une valeur par instance. */
    static final String DYNO_VARIABLE = "DYNO";

    /**
     * Resolu une seule fois pour le processus.
     *
     * <p>Hibernate instancie un generateur par champ annote · sans cela, les 258 instances
     * resoudraient et journaliseraient la meme valeur chacune de son cote.</p>
     */
    private static final Resolved MACHINE = resolveMachine(System.getenv(MACHINE_ID_VARIABLE),
            System.getenv(DYNO_VARIABLE));

    static {
        // La seule ligne que ce generateur journalise · elle est indispensable. Deux instances qui
        // annoncent le meme identifiant de machine se voient immediatement dans les journaux, ce
        // qui n'etait pas le cas quand toutes affichaient 444 sans que rien ne le signale.
        log.info("Generateur d'identifiants · identifiant de machine {} (source : {})",
                MACHINE.machineId(), MACHINE.source());
    }

    private long lastTimestamp = -1L;
    private long sequence = 0;
    private final long machineId;

    public GeneratorOfId() {
        this.machineId = MACHINE.machineId();
    }

    @Override
    public synchronized Serializable generate (SharedSessionContractImplementor session, Object object){
        return generateId();
    }

    public synchronized Long generateId() {

        long timestamp = System.currentTimeMillis() - CUSTOM_EPOCH;

        if(timestamp < 0){
            throw new IllegalStateException("Time is negative, clock moved backwards");
        }

        if(timestamp == lastTimestamp){
            sequence = (sequence + 1) & MAX_SEQUENCE;
            // If sequence overflows, wait for next millisecond
            if (sequence == 0) {
                timestamp = waitForNextMillis(timestamp);
            }
        } else {
            // Reset sequence for new millisecond
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // Rien n'est journalise ici · cette methode s'execute a chaque insertion. Elle imprimait
        // treize lignes sur la sortie standard a chaque appel, dont une auto-verification qui
        // comparait des valeurs calculees deux lignes plus haut et ne pouvait donc pas echouer.
        // Sur Heroku, le collecteur de journaux ecarte les lignes au-dela de son debit : ce bruit
        // faisait disparaitre les vraies erreurs.
        return (timestamp << TIMESTAMP_SHIFT) | (machineId << MACHINE_ID_SHIFT) | sequence;
    }

    private long waitForNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis() - CUSTOM_EPOCH;
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis() - CUSTOM_EPOCH;
        }
        return timestamp;
    }

    /**
     * D'ou vient l'identifiant de machine, par ordre de confiance.
     *
     * <p>Le reglage explicite d'abord · lui seul garantit l'unicite au lieu de la rendre probable,
     * et c'est ce qu'il faut renseigner des que l'on depasse quelques instances. A defaut, le nom
     * du dyno, qui differe par instance et reste stable d'un redemarrage a l'autre. En dernier
     * recours un tirage aleatoire · imparfait, mais une chance sur 1024 de collision vaut
     * infiniment mieux que la certitude d'en avoir une.</p>
     */
    static Resolved resolveMachine(String configured, String dyno) {
        Long explicit = parseMachineId(configured);
        if (explicit != null) {
            return new Resolved(explicit, MACHINE_ID_VARIABLE);
        }
        if (configured != null && !configured.isBlank()) {
            log.warn("{} vaut « {} », qui n'est pas un entier entre 0 et {} · valeur ignoree",
                    MACHINE_ID_VARIABLE, configured, MAX_MACHINE_ID);
        }
        if (dyno != null && !dyno.isBlank()) {
            // Le nom entier, et non son type ou son numero seul · « web.1 » et « worker.1 » ne
            // doivent pas se rejoindre.
            return new Resolved(spread(dyno.trim()), DYNO_VARIABLE);
        }
        return new Resolved(ThreadLocalRandom.current().nextLong(0, MAX_MACHINE_ID + 1), "aleatoire");
    }

    private static Long parseMachineId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            long value = Long.parseLong(raw.trim());
            return value >= 0 && value <= MAX_MACHINE_ID ? value : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Repartit une chaine sur toute la plage · {@code hashCode} seul se concentre trop. */
    private static long spread(String value) {
        long hash = 1125899906842597L;
        for (int i = 0; i < value.length(); i++) {
            hash = 31 * hash + value.charAt(i);
        }
        hash ^= (hash >>> 32);
        return Math.floorMod(hash, MAX_MACHINE_ID + 1);
    }

    public static long extractTimestamp(long id) {
        return (id >> TIMESTAMP_SHIFT) + CUSTOM_EPOCH;
    }

    public static long extractMachineId(long id) {
        return (id >> MACHINE_ID_SHIFT) & MAX_MACHINE_ID;
    }

    public static long extractSequence(long id) {
        return id & MAX_SEQUENCE;
    }

    /** L'identifiant retenu et sa provenance · la provenance rend une collision diagnosticable. */
    record Resolved(long machineId, String source) {
    }
}
