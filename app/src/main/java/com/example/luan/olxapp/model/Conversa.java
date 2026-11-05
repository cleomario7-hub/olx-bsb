package com.example.luan.olxapp.model;

public class Conversa {
    private String chatId;
    private String idAnuncio;
    private String tituloAnuncio;
    private String outroId;
    private String outroNome;
    private String ultimaMensagem;
    private long hora;

    public Conversa() {
    }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }
    public String getIdAnuncio() { return idAnuncio; }
    public void setIdAnuncio(String idAnuncio) { this.idAnuncio = idAnuncio; }
    public String getTituloAnuncio() { return tituloAnuncio; }
    public void setTituloAnuncio(String tituloAnuncio) { this.tituloAnuncio = tituloAnuncio; }
    public String getOutroId() { return outroId; }
    public void setOutroId(String outroId) { this.outroId = outroId; }
    public String getOutroNome() { return outroNome; }
    public void setOutroNome(String outroNome) { this.outroNome = outroNome; }
    public String getUltimaMensagem() { return ultimaMensagem; }
    public void setUltimaMensagem(String ultimaMensagem) { this.ultimaMensagem = ultimaMensagem; }
    public long getHora() { return hora; }
    public void setHora(long hora) { this.hora = hora; }
}
