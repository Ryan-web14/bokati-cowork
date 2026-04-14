package com.sni.bokaticowork.core.generator.password;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;

@Component
public class GeneratorOfPassword {

    private static final List<Character> LETTERS = List.of('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
            'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
            'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z');
    private static final List<Character> NUMBER = List.of('0', '1', '2', '3', '4', '5', '6', '7', '8', '9');
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generate a randomly generated password of the given length
     *
     * @param length the length of the password
     *
     * @return a randomly generated password
     * */
    public static String generatePassword(int length){

        if(length < 1){
            throw new IllegalArgumentException("Password length must be greater than 0");
        }

        StringBuilder password = new StringBuilder();

        for(int i = 0; i < length; i++){

            if(RANDOM.nextDouble() > 0.6){
                int index = RANDOM.nextInt(LETTERS.size());
                password.append(LETTERS.get(index));
            }else{
                int index = RANDOM.nextInt(NUMBER.size());
                password.append(NUMBER.get(index));
            }
        }
        return password.toString();
    }
}
