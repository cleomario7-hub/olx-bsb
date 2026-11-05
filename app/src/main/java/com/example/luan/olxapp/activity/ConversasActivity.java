package com.example.luan.olxapp.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.luan.olxapp.R;
import com.example.luan.olxapp.adapter.AdapterConversa;
import com.example.luan.olxapp.helper.ConfiguracaoFirebase;
import com.example.luan.olxapp.helper.NavegacaoInferior;
import com.example.luan.olxapp.model.Conversa;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ConversasActivity extends AppCompatActivity {

    private final List<Conversa> conversas = new ArrayList<>();
    private AdapterConversa adapter;
    private TextView textVazio;
    private DatabaseReference ref;
    private ValueEventListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversas);
        setTitle("Conversas");

        textVazio = findViewById(R.id.textVazio);
        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapter = new AdapterConversa(conversas, new AdapterConversa.Listener() {
            @Override
            public void onConversaClick(Conversa c) {
                Intent i = new Intent(ConversasActivity.this, ChatActivity.class);
                i.putExtra("chatId", c.getChatId());
                i.putExtra("outroId", c.getOutroId());
                i.putExtra("outroNome", c.getOutroNome());
                i.putExtra("idAnuncio", c.getIdAnuncio());
                i.putExtra("tituloAnuncio", c.getTituloAnuncio());
                startActivity(i);
            }
        });
        recycler.setAdapter(adapter);

        NavegacaoInferior.configurar(this, (BottomNavigationView) findViewById(R.id.bottomNav), R.id.nav_chat);

        if (!NavegacaoInferior.logado()) {
            finish();
            return;
        }

        ref = ConfiguracaoFirebase.getFirebase().child("usuario_chats").child(ConfiguracaoFirebase.getIdUsuario());
        listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                conversas.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Conversa c = ds.getValue(Conversa.class);
                    if (c != null) {
                        c.setChatId(ds.getKey());
                        conversas.add(c);
                    }
                }
                Collections.sort(conversas, new Comparator<Conversa>() {
                    @Override
                    public int compare(Conversa a, Conversa b) {
                        return Long.compare(b.getHora(), a.getHora()); // mais recente primeiro
                    }
                });
                adapter.notifyDataSetChanged();
                textVazio.setVisibility(conversas.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        ref.addValueEventListener(listener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ref != null && listener != null) ref.removeEventListener(listener);
    }
}
