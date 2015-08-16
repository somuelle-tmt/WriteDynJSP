package com.github.nilscoding.writedynjsp.utils;

/**
 * String utils
 * @author NilsCoding
 */
public class StringUtils {
    
    private StringUtils() { }
    
    /**
     * Checks if the given string is logically empty
     * @param str   string to check
     * @return  true if empty, false if not empty
     */
    public static boolean isEmpty(String str) {
        if ((str == null) || (str.length() == 0)) {
            return true;
        }
        str = str.trim();
        return (str.length() == 0);
    }
    
}
