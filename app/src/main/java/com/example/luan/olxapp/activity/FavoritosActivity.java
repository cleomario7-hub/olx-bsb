package com.example.luan.olxapp.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.luan.olxapp.R;
import com.example.luan.olxapp.adapter.AdapterAnuncio;
import com.example.luan.olxapp.helper.ConfiguracaoFirebase;
import com.example.luan.olxapp.helper.FavoritosHelper;
import com.example.luan.olxapp.helper.NavegacaoInferior;
import com.example.luan.olxapp.model.Anuncio;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FavoritosActivity extends AppCompatActivity {

    private final Set<String> favoritos = new HashSet<>();
    private final List<Anuncio> todos = new ArrayList<>();
    private final List<Anuncio> lista = new ArrayList<>();
    private AdapterAnuncio adapter;
    private TextView textVazio;

    private DatabaseReference favoritosRef, anunciosRef;
    private ValueEventListener favoritosListener, anunciosListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favoritos);
        setTitle("Favoritos");

        textVazio = findViewById(R.id.textVazio);
        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new AdapterAnuncio(lista, this);
        recycler.setAdapter(adapter);

        adapter.setListener(new AdapterAnuncio.Listener() {
            @Override
            public void onAnuncioClick(Anuncio anuncio) {
                Intent i = new Intent(FavoritosActivity.this, DetalhesProdutoActivity.class);
                i.putExtra("anuncioSelecionado", anuncio);
                startActivity(i);
            }

            @Override
            public void onFavoritoClick(Anuncio anuncio) {
                FavoritosHelper.alternar(FavoritosActivity.this, anuncio, favoritos);
            }
        });

        NavegacaoInferior.configurar(this, (BottomNavigationView) findViewById(R.id.bottomNav), R.id.nav_favoritos);

        if (!NavegacaoInferior.logado()) {
            finish();
            return;
        }

        favoritosRef = FavoritosHelper.referencia();
        favoritosListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                favoritos.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (ds.getKey() != null) favoritos.add(ds.getKey());
                }
                adapter.setFavoritos(favoritos);
                filtrar();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        favoritosRef.addValueEventListener(favoritosListener);

        anunciosRef = ConfiguracaoFirebase.getFirebase().child("anuncios");
        anunciosListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                todos.clear();
                for (DataSnapshot estados : snapshot.getChildren()) {
                    for (DataSnapshot categorias : estados.getChildren()) {
                        for (DataSnapshot anuncios : categorias.getChildren()) {
                            Anuncio a = anuncios.getValue(Anuncio.class);
                            if (a != null) todos.add(a);
                        }
                    }
                }
                Collections.reverse(todos);
                filtrar();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        anunciosRef.addValueEventListener(anunciosListener);
    }

    private void filtrar() {
        lista.clear();
        for (Anuncio a : todos) {
            if (a.getIdAnuncio() != null && favoritos.contains(a.getIdAnuncio())) lista.add(a);
        }
        adapter.notifyDataSetChanged();
        textVazio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (favoritosRef != null && favoritosListener != null) favoritosRef.removeEventListener(favoritosListener);
        if (anunciosRef != null && anunciosListener != null) anunciosRef.removeEventListener(anunciosListener);
    }
}
