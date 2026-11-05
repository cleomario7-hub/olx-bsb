package com.example.luan.olxapp.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.luan.olxapp.R;
import com.example.luan.olxapp.adapter.AdapterMensagem;
import com.example.luan.olxapp.helper.ConfiguracaoFirebase;
import com.example.luan.olxapp.model.Mensagem;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chat entre comprador e vendedor de um anúncio.
 * Estrutura no banco:
 *   chats/{chatId}/mensagens/{id}   -> {de, texto, hora}
 *   chats/{chatId}/participantes/{uid} -> true
 *   usuario_chats/{uid}/{chatId}    -> resumo para a lista de conversas
 * chatId = idDoAnuncio + "_" + idDoComprador
 */
public class ChatActivity extends AppCompatActivity {

    private final List<Mensagem> mensagens = new ArrayList<>();
    private AdapterMensagem adapter;
    private RecyclerView recycler;
    private EditText editMensagem;

    private String chatId, outroId, idAnuncio, tituloAnuncio, meuId;
    private String meuNome = "Usuário";
    private String outroNome = "Usuário";

    private DatabaseReference mensagensRef;
    private ChildEventListener mensagensListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatId = getIntent().getStringExtra("chatId");
        outroId = getIntent().getStringExtra("outroId");
        idAnuncio = getIntent().getStringExtra("idAnuncio");
        tituloAnuncio = getIntent().getStringExtra("tituloAnuncio");
        String nomeExtra = getIntent().getStringExtra("outroNome");
        if (nomeExtra != null) outroNome = nomeExtra;

        if (chatId == null || outroId == null
                || ConfiguracaoFirebase.getFirebaseAutenticacao().getCurrentUser() == null) {
            Toast.makeText(this, "Conversa indisponível", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        meuId = ConfiguracaoFirebase.getIdUsuario();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(outroNome);
        }
        ((TextView) findViewById(R.id.textAnuncioChat)).setText(tituloAnuncio == null ? "" : tituloAnuncio);

        editMensagem = findViewById(R.id.editMensagem);
        recycler = findViewById(R.id.recyclerMensagens);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recycler.setLayoutManager(lm);
        adapter = new AdapterMensagem(mensagens, meuId);
        recycler.setAdapter(adapter);

        findViewById(R.id.btnEnviar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enviar();
            }
        });

        carregarNomes();
        ouvirMensagens();
    }

    private void carregarNomes() {
        DatabaseReference usuarios = ConfiguracaoFirebase.getFirebase().child("usuarios");
        usuarios.child(meuId).child("nome").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                String n = s.getValue(String.class);
                if (n != null && !n.isEmpty()) meuNome = n;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
            }
        });
        usuarios.child(outroId).child("nome").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                String n = s.getValue(String.class);
                if (n != null && !n.isEmpty()) {
                    outroNome = n;
                    if (getSupportActionBar() != null) getSupportActionBar().setTitle(outroNome);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
            }
        });
    }

    private void ouvirMensagens() {
        mensagensRef = ConfiguracaoFirebase.getFirebase().child("chats").child(chatId).child("mensagens");
        mensagensListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previous) {
                Mensagem m = snapshot.getValue(Mensagem.class);
                if (m == null) return;
                mensagens.add(m);
                adapter.notifyItemInserted(mensagens.size() - 1);
                recycler.scrollToPosition(mensagens.size() - 1);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previous) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previous) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this,
                        "Não foi possível carregar a conversa", Toast.LENGTH_SHORT).show();
            }
        };
        mensagensRef.addChildEventListener(mensagensListener);
    }

    private void enviar() {
        String texto = editMensagem.getText().toString().trim();
        if (texto.isEmpty()) return;
        editMensagem.setText("");

        long agora = System.currentTimeMillis();
        DatabaseReference raiz = ConfiguracaoFirebase.getFirebase();
        String key = mensagensRef.push().getKey();
        if (key == null) return;

        Map<String, Object> msg = new HashMap<>();
        msg.put("de", meuId);
        msg.put("texto", texto);
        msg.put("hora", agora);

        Map<String, Object> paraMim = resumo(outroId, outroNome, texto, agora);
        Map<String, Object> paraOutro = resumo(meuId, meuNome, texto, agora);

        // Tudo numa única gravação: mensagem + participantes + resumo das duas conversas
        Map<String, Object> up = new HashMap<>();
        up.put("chats/" + chatId + "/mensagens/" + key, msg);
        up.put("chats/" + chatId + "/participantes/" + meuId, true);
        up.put("chats/" + chatId + "/participantes/" + outroId, true);
        up.put("usuario_chats/" + meuId + "/" + chatId, paraMim);
        up.put("usuario_chats/" + outroId + "/" + chatId, paraOutro);

        raiz.updateChildren(up).addOnFailureListener(e ->
                Toast.makeText(ChatActivity.this, "Erro ao enviar a mensagem", Toast.LENGTH_SHORT).show());
    }

    // Resumo mostrado na lista de conversas de quem recebe (outro = a outra pessoa)
    private Map<String, Object> resumo(String outro, String nomeOutro, String ultima, long hora) {
        Map<String, Object> r = new HashMap<>();
        r.put("idAnuncio", idAnuncio);
        r.put("tituloAnuncio", tituloAnuncio);
        r.put("outroId", outro);
        r.put("outroNome", nomeOutro);
        r.put("ultimaMensagem", ultima);
        r.put("hora", hora);
        return r;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mensagensRef != null && mensagensListener != null) {
            mensagensRef.removeEventListener(mensagensListener);
        }
    }
}
