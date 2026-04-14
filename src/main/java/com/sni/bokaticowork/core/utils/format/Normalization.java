package com.sni.bokaticowork.core.utils.format;

import java.util.Random;

public class Normalization {

    // Will make the first letter of each word in the first name Capital
    public static String normalizeFirstname(String firstname) {
        if (firstname == null || firstname.isEmpty()) {
            return firstname;
        }

        String[] words = firstname.trim().split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.contains("-")) {
                String[] hyphenWords = word.split("-");
                StringBuilder hyphenResult = new StringBuilder();

                for (String hyphenWord : hyphenWords) {
                    if (!hyphenWord.isEmpty()) {
                        String firstLetter = hyphenWord.substring(0, 1).toUpperCase();
                        String restOfWord = hyphenWord.substring(1).toLowerCase();
                        hyphenResult.append(firstLetter).append(restOfWord);
                    }
                    hyphenResult.append("-");
                }

                // Remove the trailing hyphen
                if (!hyphenResult.isEmpty() && hyphenResult.charAt(hyphenResult.length() - 1) == '-') {
                    hyphenResult.setLength(hyphenResult.length() - 1);
                }
                result.append(hyphenResult);
            } else {
                if (!word.isEmpty()) {
                    String firstLetter = word.substring(0, 1).toUpperCase();
                    String restOfWord = word.substring(1).toLowerCase();
                    result.append(firstLetter).append(restOfWord);
                }
            }

            // Add space between words (but not after the last word)
            result.append(" ");
        }

        // Remove the trailing space
        if (!result.isEmpty() && result.charAt(result.length() - 1) == ' ') {
            result.setLength(result.length() - 1);
        }

        return result.toString();
    }

    public static String normalizeLastname(String lastname) {
        if(lastname.isEmpty()){
            return lastname;
        }
        return lastname.toUpperCase();
    }

    public static String getString(String firstname, String lastname, String prefix) {
        String cleanFirstname = firstname.replaceAll("\\s+","").toUpperCase();
        String cleanLastname = lastname.replaceAll("\\s+","").toUpperCase();
        StringBuilder firstnameString = new StringBuilder(cleanFirstname);
        StringBuilder lastnameString = new StringBuilder(cleanLastname);
        Random random = new Random();

        if(lastnameString.length() < 3){
            while(lastnameString.length() < 3){
                char randomChar = (char)('A' + random.nextInt(26));
                lastnameString.append(randomChar);
            }
        }
        if(cleanFirstname.length() < 3){
            while(firstnameString.length() < 3){
                char randomChar = (char)('A' + random.nextInt(26));
                firstnameString.append(randomChar);
            }
        }
        String subFirst = firstnameString.substring(0, 3);
        String subLast = lastnameString.substring(0, 3);
        return prefix + subFirst + subLast +
                String.format("%04d", new Random().nextInt(10000));
    }

    // Test method to verify the functionality
    public static void main(String[] args) {
        System.out.println("'" + normalizeFirstname("olivia marie") + "'"); // Should print: Olivia Marie
        System.out.println("'" + normalizeFirstname("jean-claude") + "'"); // Should print: Jean-Claude
        System.out.println("'" + normalizeFirstname("marie-claire anne") + "'"); // Should print: Marie-Claire Anne
        System.out.println("'" + normalizeFirstname("JOHN") + "'"); // Should print: John
        System.out.println("'" + normalizeFirstname("") + "'"); // Should print: (empty string)
        System.out.println("'" + normalizeFirstname("a b c") + "'"); // Should print: A B C
    }
}