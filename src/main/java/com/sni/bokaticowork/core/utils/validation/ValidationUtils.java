package com.sni.bokaticowork.core.utils.validation;

import java.util.regex.Pattern;

public class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$");
    private static final Pattern NIU_PATTERN = Pattern.compile("^[PE][0-9]{15}$");

    // Free-text fields such as descriptions: allow any letter, digit, punctuation,
    // symbol and whitespace (including line breaks). Only control characters other
    // than tab/newline/carriage-return are rejected.
    private static final Pattern DESCRIPTION_PATTERN =
            Pattern.compile("^[\\p{L}\\p{N}\\p{P}\\p{S}\\p{Zs}\\r\\n\\t]+$");

    public static boolean validateString(String str){
        if (str.isEmpty()){
            return false;
        }
        return str.matches("^[\\p{L}\\p{N} ,.'\\-()/:&+]+$");
    }

    /**
     * Validates a free-text field (e.g. a description). Accepts a much broader set of
     * characters than {@link #validateString(String)} · hyphens, slashes, exclamation
     * marks and other common punctuation/symbols · while still rejecting empty input
     * and control characters.
     */
    public static boolean validateDescription(String str){
        if (str == null || str.isEmpty()){
            return false;
        }
        return DESCRIPTION_PATTERN.matcher(str).matches();
    }

    /**
     * Le numero a-t-il une forme lisible · nationale ou internationale, espaces et plus compris.
     *
     * <p>Ne juge que la forme. L'indicatif manquant est complete ailleurs, par
     * {@link com.sni.bokaticowork.core.utils.phone.PhoneNumberService}, qui connait les pays.</p>
     */
    public static boolean validatePhoneNumber(String str){
        return com.sni.bokaticowork.core.utils.phone.PhoneNumbers.looksValid(str);
    }

    public static boolean validateEmail(String str){
        if(str.isEmpty()){
            return false;
        }

        return EMAIL_PATTERN.matcher(str).matches();
    }

    public static boolean isDigit(String str){

        StringBuilder sb = new StringBuilder(str);

        for(Character c : sb.toString().toCharArray()){
            if(!Character.isDigit(c)){
                return false;
            }

        }
        return true;
    }

    public static boolean validateNiu(String str){
        if(str.isEmpty()){
            return false;
        }
        return NIU_PATTERN.matcher(str).matches();
    }

}
