package com.bonusflowpj.service;

public final class CpfValidator {

    private CpfValidator() {
    }

    public static String digitsOnly(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }

    public static boolean isValid(String value) {
        String cpf = digitsOnly(value);
        if (cpf == null || cpf.length() != 11 || cpf.chars().distinct().count() == 1) {
            return false;
        }
        return digit(cpf, 9) == Character.digit(cpf.charAt(9), 10)
            && digit(cpf, 10) == Character.digit(cpf.charAt(10), 10);
    }

    private static int digit(String cpf, int length) {
        int sum = 0;
        for (int index = 0; index < length; index++) {
            sum += Character.digit(cpf.charAt(index), 10) * (length + 1 - index);
        }
        int result = 11 - (sum % 11);
        return result >= 10 ? 0 : result;
    }
}
