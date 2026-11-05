package com.example.luan.olxapp.model;

public class Mensagem {
    private String de;
    private String texto;
    private long hora;

    public Mensagem() {
    }

    public Mensagem(String de, String texto, long hora) {
        this.de = de;
        this.texto = texto;
        this.hora = hora;
    }

    public String getDe() { return de; }
    public void setDe(String de) { this.de = de; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public long getHora() { return hora; }
    public void setHora(long hora) { this.hora = hora; }
}
