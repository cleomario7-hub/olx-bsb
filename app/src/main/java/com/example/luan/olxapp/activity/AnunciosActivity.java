package com.example.luan.olxapp.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.luan.olxapp.R;
import com.example.luan.olxapp.adapter.AdapterAnuncio;
import com.example.luan.olxapp.helper.ConfiguracaoFirebase;
import com.example.luan.olxapp.helper.RecyclerItemClickListener;
import com.example.luan.olxapp.model.Anuncio;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dmax.dialog.SpotsDialog;

public class AnunciosActivity extends AppCompatActivity {

    FirebaseAuth auth;
    private RecyclerView recyclerAnunciosPublicos;
    private Button btnRegiao, btnCategoria;
    private AdapterAnuncio adapterAnuncio;
    private List<Anuncio> anuncioList = new ArrayList<>();
    private DatabaseReference anunciosPublicosRef;
    private AlertDialog dialog;
    private String filtroEstado, filtroCategoria;
    private boolean filtrandoPoEstado = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anuncios);

        //Configurações iniciais
        auth = ConfiguracaoFirebase.getFirebaseAutenticacao();

        anunciosPublicosRef = ConfiguracaoFirebase.getFirebase()
                .child("anuncios");

        recyclerAnunciosPublicos = findViewById(R.id.recyclerAnunciosPublicos);
        btnCategoria = findViewById(R.id.btnCategoria);
        btnRegiao = findViewById(R.id.btnRegiao);

        recyclerAnunciosPublicos.setLayoutManager(new LinearLayoutManager(this));
        recyclerAnunciosPublicos.setHasFixedSize(true);
        adapterAnuncio = new AdapterAnuncio(anuncioList, this);
        recyclerAnunciosPublicos.setAdapter(adapterAnuncio);

        recuperarAnunciosPublicos();

        //Aplicar evento de clique
        recyclerAnunciosPublicos.addOnItemTouchListener(
                new RecyclerItemClickListener(this, recyclerAnunciosPublicos, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        Anuncio anuncioSelecionado = anuncioList.get(position);
                        Intent i = new Intent(AnunciosActivity.this, DetalhesProdutoActivity.class);
                        i.putExtra("anuncioSelecionado", anuncioSelecionado);
                        startActivity(i);

                    }

                    @Override
                    public void onLongItemClick(View view, int position) {

                    }

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    }
                })
        );


        btnRegiao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogFiltroEstado();
            }
        });

        btnCategoria.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogFiltroCategoria();
            }
        });
    }


    private void showDialogFiltroCategoria(){

        if(filtrandoPoEstado){
            AlertDialog.Builder dialogCategoria = new AlertDialog.Builder(this);
            dialogCategoria.setTitle("Selecione a categoria desejada");

            //Configurar o spinner
            View viewSpinner = getLayoutInflater().inflate(R.layout.dialog_spinner, null);

            final Spinner spinner = viewSpinner.findViewById(R.id.spinnerFiltro);
            String[] categoria = getResources().getStringArray(R.array.categorias);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, categoria
            );
            adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
            spinner.setAdapter(adapter);

            dialogCategoria.setView(viewSpinner);
            dialogCategoria.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    filtroCategoria = spinner.getSelectedItem().toString();
                    recuperarAnunciosPorFiltroCategoria();
                }
            }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            AlertDialog dialog = dialogCategoria.create();
            dialog.show();
        }
        else {
            Toast.makeText(this, "Escolha primeiro uma região!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showDialogFiltroEstado(){
        AlertDialog.Builder dialogEstado = new AlertDialog.Builder(this);
        dialogEstado.setTitle("Selecione o estado desejado");

        //Configurar o spinner
        View viewSpinner = getLayoutInflater().inflate(R.layout.dialog_spinner, null);

        final Spinner spinner = viewSpinner.findViewById(R.id.spinnerFiltro);
        String[] estados = getResources().getStringArray(R.array.estados);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, estados
        );
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        spinner.setAdapter(adapter);

        dialogEstado.setView(viewSpinner);
        dialogEstado.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                filtroEstado = spinner.getSelectedItem().toString();
                recuperarAnunciosPorFiltroEstado();
                filtrandoPoEstado = true;
            }
        }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        AlertDialog dialog = dialogEstado.create();
        dialog.show();
    }

    private void recuperarAnunciosPorFiltroCategoria(){

        dialog = new SpotsDialog.Builder()
                .setContext(this)
                .setMessage("Recuperando anúncios ")
                .setCancelable(false)
                .build();
        dialog.show();

        anunciosPublicosRef = ConfiguracaoFirebase.getFirebase()
                .child("anuncios")
                .child(filtroEstado)
                .child(filtroCategoria);

        anuncioList.clear();

        anunciosPublicosRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for(DataSnapshot anuncios : dataSnapshot.getChildren()){
                    Anuncio anuncio = anuncios.getValue(Anuncio.class);
                    anuncioList.add(anuncio);
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


    private void recuperarAnunciosPorFiltroEstado(){

        dialog = new SpotsDialog.Builder()
                .setContext(this)
                .setMessage("Recuperando anúncios")
                .setCancelable(false)
                .build();
        dialog.show();

        anunciosPublicosRef = ConfiguracaoFirebase.getFirebase()
                .child("anuncios")
                .child(filtroEstado);

        anuncioList.clear();
        anunciosPublicosRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for(DataSnapshot categorias : dataSnapshot.getChildren()){
                    for(DataSnapshot anuncios: categorias.getChildren()){
                        Anuncio anuncio = anuncios.getValue(Anuncio.class);
                        anuncioList.add(anuncio);

                    }
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

    private void recuperarAnunciosPublicos(){

        dialog = new SpotsDialog.Builder()
                .setContext(this)
                .setMessage("Recuperando anúncios")
                .setCancelable(false)
                .build();
        dialog.show();

        anuncioList.clear();

        anunciosPublicosRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for(DataSnapshot estados : dataSnapshot.getChildren()){
                    for(DataSnapshot categorias : estados.getChildren()){
                        for(DataSnapshot anuncios: categorias.getChildren()){
                            Anuncio anuncio = anuncios.getValue(Anuncio.class);
                            anuncioList.add(anuncio);
                        }
                    }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        if(auth.getCurrentUser() == null){ //usuario deslogado
            menu.setGroupVisible(R.id.group_deslogado, true);

        }else{ //usuario logado
            menu.setGroupVisible(R.id.group_logado, true);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.menu_cadastrar:
                startActivity(new Intent(getApplicationContext(), CadastroActivity.class));
                break;
            case R.id.menu_sair:
                auth.signOut();
                invalidateOptionsMenu();
                break;
            case R.id.menu_anuncios:
                startActivity(new Intent(getApplicationContext(), MeusAnunciosActivity.class));
                break;
        }


        return super.onOptionsItemSelected(item);
    }
}
