package com.example.luan.olxapp.model;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.luan.olxapp.helper.ConfiguracaoFirebase;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.Serializable;
import java.util.List;

public class Anuncio implements Serializable {

    private String idAnuncio;
    private String estado;
    private String categoria;
    private String titulo;
    private String valor;
    private String telefone;
    private String descricao;
    private List<String> fotos;

    public Anuncio() {
    }

    public Anuncio(String estado, String categoria, String titulo, String valor, String telefone, String descricao) {

        DatabaseReference anuncioRef = ConfiguracaoFirebase.getFirebase().child("meus_anuncios");
        this.idAnuncio = anuncioRef.push().getKey();
        this.estado = estado;
        this.categoria = categoria;
        this.titulo = titulo;
        this.valor = valor;
        this.telefone = telefone;
        this.descricao = descricao;
    }

    public void salvar(){

        String idUsuario = ConfiguracaoFirebase.getIdUsuario();

        DatabaseReference anuncioRef = ConfiguracaoFirebase.getFirebase()
                .child("meus_anuncios");

        anuncioRef.child(idUsuario)
                .child(idAnuncio)
                .setValue(this);

        salvarAnuncioPublico();
    }

    public void remover(){

        String idUsuario = ConfiguracaoFirebase.getIdUsuario();

        DatabaseReference anuncioRef = ConfiguracaoFirebase.getFirebase()
                .child("meus_anuncios")
                .child( idUsuario )
                .child(getIdAnuncio());

        anuncioRef.removeValue();
        removerAnuncioPublico();
        removeFotosStorage();
    }

    public void removerAnuncioPublico(){

        DatabaseReference anuncioRef = ConfiguracaoFirebase.getFirebase()
                .child("anuncios")
                .child(getEstado())
                .child(getCategoria())
                .child(getIdAnuncio());

        anuncioRef.removeValue();
    }

    public void salvarAnuncioPublico(){


        DatabaseReference anuncioRef = ConfiguracaoFirebase.getFirebase()
                .child("anuncios");

        anuncioRef.child( getEstado() )
                .child( getCategoria() )
                .child( getIdAnuncio() )
                .setValue(this);

    }

    public void removeFotosStorage(){

        List<String> listaDeFotos = this.getFotos();
        for(int i = 0; i < listaDeFotos.size(); i++){

            StorageReference storageReference = FirebaseStorage.getInstance().getReference();
            StorageReference storage;

            storage = storageReference.child("imagens").child("anuncios").child(this.getIdAnuncio())
                        .child("Imagem"+i);

            storage.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {

                    //Adicione o que você quiser aqui após a exclusão ser bem sucedida.
                    Log.i("FOTO", "onSuccess: Foto deletada");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                    //E aqui caso não seja
                    Log.i("FOTO", "onFailure: Erro para deletar Foto");
                }
            });
        }
    }


    public String getIdAnuncio() {
        return idAnuncio;
    }

    public void setIdAnuncio(String idAnuncio) {
        this.idAnuncio = idAnuncio;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public List<String> getFotos() {
        return fotos;
    }

    public void setFotos(List<String> fotos) {
        this.fotos = fotos;
    }

}
