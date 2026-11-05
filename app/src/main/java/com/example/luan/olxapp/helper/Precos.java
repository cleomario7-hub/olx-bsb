package com.example.luan.olxapp.helper;

/** Converte textos como "R$ 1.234,56" em centavos (123456) para filtrar e ordenar. */
public class Precos {
    public static long centavos(String valor) {
        if (valor == null) return 0;
        String digitos = valor.replaceAll("\\D", "");
        if (digitos.isEmpty() || digitos.length() > 15) return 0;
        return Long.parseLong(digitos);
    }
}
