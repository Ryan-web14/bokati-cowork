package com.sni.bokaticowork.core.generator.id;


import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;

public class GeneratorOfId implements IdentifierGenerator {

    //Custom epoch in bits
    private static long CUSTOM_EPOCH = 49852800000L; //this 30-07-2025

    //Bit allocation
    //12 bits
    private static int SEQUENCE_BITS = 12;
    //10 bits
    private static int MACHINE_ID_BITS = 10;

    //MAXIMUM VALUE
    private static long MAX_MACHINE_ID = (1L << MACHINE_ID_BITS) - 1; // 4096
    private static long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1; //1093 values possible

    //Bit shift
    private static int MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static int TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;

    private long lastTimestamp = -1L;
    private long sequence = 0;
    private long machineId;

    public GeneratorOfId(){
        this.machineId = getMachineId();
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

        long finalId =  (timestamp << TIMESTAMP_SHIFT) |
                (machineId << MACHINE_ID_SHIFT) | sequence;


        verifyIdComponents( finalId);

        System.out.println("Generated ID: " + finalId);

        return finalId;
    }

    private long waitForNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis() - CUSTOM_EPOCH;
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis() - CUSTOM_EPOCH;
        }
        return timestamp;
    }

    private long getMachineId() {

        try {
            String hostName = System.getProperty("user.name", "unknown");
            int hashCode = Math.abs(hostName.hashCode());
            return hashCode % (MAX_MACHINE_ID + 1);
        } catch (Exception e) {
            // Fallback to random if system properties are not available
            return ThreadLocalRandom.current().nextLong(0, MAX_MACHINE_ID + 1);
        }
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


    private void verifyIdComponents(long id) {
        System.out.println("\n--- Verification (Extracting Components) ---");

        long extractedTimestamp = extractTimestamp(id);
        long extractedMachineId = extractMachineId(id);
        long extractedSequence = extractSequence(id);

        System.out.println("Extracted timestamp: " + (extractedTimestamp - CUSTOM_EPOCH) + " (relative)");
        System.out.println("Extracted timestamp: " + extractedTimestamp + " (absolute)");
        System.out.println("Extracted timestamp: " + new java.util.Date(extractedTimestamp) + " (date)");
        System.out.println("Extracted machine ID: " + extractedMachineId);
        System.out.println("Extracted sequence: " + extractedSequence);

        // Verify they match original values
        boolean timestampMatch = (extractedTimestamp - CUSTOM_EPOCH) == lastTimestamp;
        boolean machineIdMatch = extractedMachineId == machineId;
        boolean sequenceMatch = extractedSequence == sequence;

        System.out.println("\n--- Verification Results ---");
        System.out.println("Timestamp matches: " + timestampMatch);
        System.out.println("Machine ID matches: " + machineIdMatch);
        System.out.println("Sequence matches: " + sequenceMatch);
        System.out.println("Overall verification: " + (timestampMatch && machineIdMatch && sequenceMatch ? "PASS" : "FAIL"));
        System.out.println("========================");
    }


}


