package com.hmdp.utils;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Create by rlhbili on 2022/11/30
 */
public class CodeUtils {
    private static final Random r = new Random();
//    private static List<Character> characterList = new ArrayList<Character>()
//    {{
//        for (int i = 97; i <= 122; i++) {
//            add((char) i);
//        }
//
//        for (int i = 65; i <= 90; i++) {
//            add((char) i);
//        }
//    }};

    public static String randomCode(int num) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < num; i++) {
            int nextInt = r.nextInt(10);
            sb.append(nextInt);
        }

        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.println(CodeUtils.randomCode(6));
    }
}
