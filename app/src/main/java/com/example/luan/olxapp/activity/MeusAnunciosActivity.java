package com.example.luan.olxapp.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.example.luan.olxapp.adapter.AdapterAnuncio;
import com.example.luan.olxapp.helper.ConfiguracaoFirebase;
import com.example.luan.olxapp.helper.RecyclerItemClickListener;
import com.example.luan.olxapp.model.Anuncio;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.example.luan.olxapp.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import dmax.dialog.SpotsDialog;

public class MeusAnunciosActivity extends AppCompatActivity {


    private RecyclerView recyclerAnuncios;
    private List<Anuncio> anuncioList = new ArrayList<>();
    private AdapterAnuncio adapterAnuncio;
    private DatabaseReference anuncioUsuarioRef;
    private AlertDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meus_anuncios);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }


        recyclerAnuncios = findViewById(R.id.recyclerAnuncios);

        anuncioUsuarioRef = ConfiguracaoFirebase.getFirebase()
                .child("meus_anuncios")
                .child(ConfiguracaoFirebase.getIdUsuario());

        recyclerAnuncios.setLayoutManager(new LinearLayoutManager(this));
        recyclerAnuncios.setHasFixedSize(true);
        adapterAnuncio = new AdapterAnuncio(anuncioList, this);
        recyclerAnuncios.setAdapter(adapterAnuncio);

        //Recupera anuncios para o usuario
        recuperarAnuncios();

        recyclerAnuncios.addOnItemTouchListener(
                new RecyclerItemClickListener(this, recyclerAnuncios, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {

                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                        Anuncio anuncioSelecionado = anuncioList.get(position);

                        anuncioSelecionado.remover();

                        adapterAnuncio.notifyDataSetChanged();
                        Toast.makeText(MeusAnunciosActivity.this, "Anuncio Excluido", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    }
                })
        );


        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), CadastrarAnuncioActivity.class));
            }
        });
    }

    private void recuperarAnuncios(){

        dialog = new SpotsDialog.Builder()
                .setContext(this)
                .setMessage("Recuperando Anúncio")
                .build();
        dialog.show();

        anuncioUsuarioRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                anuncioList.clear();
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    anuncioList.add(ds.getValue(Anuncio.class));
                }
                Collections.reverse(anuncioList);
                adapterAnuncio.notifyDataSetChanged();

                dialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });



    }

}
