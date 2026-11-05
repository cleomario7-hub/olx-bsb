package com.example.luan.olxapp.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.luan.olxapp.R;
import com.example.luan.olxapp.helper.ConfiguracaoFirebase;
import com.example.luan.olxapp.helper.NavegacaoInferior;
import com.example.luan.olxapp.model.Anuncio;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/** Painel do cliente: perfil, números da conta e atalhos. */
public class ContaActivity extends AppCompatActivity {

    private TextView textNome, textEmail, textTelefone;
    private TextView statAtivos, statVendidos, statFavoritos, statConversas;
    private View painel;
    private Button btnEntrar;

    private String nomeAtual = "", telefoneAtual = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conta);
        setTitle("Minha conta");

        textNome = findViewById(R.id.textContaNome);
        textEmail = findViewById(R.id.textContaEmail);
        textTelefone = findViewById(R.id.textContaTelefone);
        statAtivos = findViewById(R.id.statAtivos);
        statVendidos = findViewById(R.id.statVendidos);
        statFavoritos = findViewById(R.id.statFavoritos);
        statConversas = findViewById(R.id.statConversas);
        painel = findViewById(R.id.painel);
        btnEntrar = findViewById(R.id.btnEntrar);

        NavegacaoInferior.configurar(this, (BottomNavigationView) findViewById(R.id.bottomNav), R.id.nav_conta);

        btnEntrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ContaActivity.this, CadastroActivity.class));
            }
        });
        findViewById(R.id.btnMeusAnuncios).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ContaActivity.this, MeusAnunciosActivity.class));
            }
        });
        findViewById(R.id.btnEditarPerfil).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editarPerfil();
            }
        });
        findViewById(R.id.btnSair).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConfiguracaoFirebase.getFirebaseAutenticacao().signOut();
                Intent i = new Intent(ContaActivity.this, AnunciosActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregar();
    }

    private void carregar() {
        FirebaseUser usuario = ConfiguracaoFirebase.getFirebaseAutenticacao().getCurrentUser();
        if (usuario == null) {
            painel.setVisibility(View.GONE);
            btnEntrar.setVisibility(View.VISIBLE);
            textNome.setText("Você não está logado");
            textEmail.setText("Entre para ver seu painel, favoritos e conversas.");
            textTelefone.setText("");
            return;
        }
        painel.setVisibility(View.VISIBLE);
        btnEntrar.setVisibility(View.GONE);
        textEmail.setText(usuario.getEmail());

        String uid = usuario.getUid();
        DatabaseReference raiz = ConfiguracaoFirebase.getFirebase();

        raiz.child("usuarios").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                String nome = s.child("nome").getValue(String.class);
                String tel = s.child("telefone").getValue(String.class);
                nomeAtual = nome == null ? "" : nome;
                telefoneAtual = tel == null ? "" : tel;
                textNome.setText(nomeAtual.isEmpty() ? "Minha conta" : nomeAtual);
                textTelefone.setText(telefoneAtual);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
            }
        });

        raiz.child("meus_anuncios").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                int ativos = 0, vendidos = 0;
                for (DataSnapshot ds : s.getChildren()) {
                    Anuncio a = ds.getValue(Anuncio.class);
                    if (a == null) continue;
                    if (a.isVendido()) vendidos++; else ativos++;
                }
                statAtivos.setText(String.valueOf(ativos));
                statVendidos.setText(String.valueOf(vendidos));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
            }
        });

        raiz.child("favoritos").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                statFavoritos.setText(String.valueOf(s.getChildrenCount()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
            }
        });

        raiz.child("usuario_chats").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                statConversas.setText(String.valueOf(s.getChildrenCount()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
            }
        });
    }

    private void editarPerfil() {
        LinearLayout caixa = new LinearLayout(this);
        caixa.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (20 * getResources().getDisplayMetrics().density);
        caixa.setPadding(p, p / 2, p, 0);

        final EditText nome = new EditText(this);
        nome.setHint("Nome completo");
        nome.setText(nomeAtual);
        final EditText telefone = new EditText(this);
        telefone.setHint("Telefone com DDD");
        telefone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        telefone.setText(telefoneAtual);
        caixa.addView(nome);
        caixa.addView(telefone);

        new AlertDialog.Builder(this)
                .setTitle("Editar perfil")
                .setView(caixa)
                .setPositiveButton("Salvar", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        String n = nome.getText().toString().trim();
                        if (n.length() < 2) {
                            Toast.makeText(ContaActivity.this, "Digite seu nome", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Map<String, Object> up = new HashMap<>();
                        up.put("nome", n);
                        up.put("telefone", telefone.getText().toString().trim());
                        ConfiguracaoFirebase.getFirebase().child("usuarios")
                                .child(ConfiguracaoFirebase.getIdUsuario()).updateChildren(up);
                        nomeAtual = n;
                        telefoneAtual = telefone.getText().toString().trim();
                        textNome.setText(nomeAtual);
                        textTelefone.setText(telefoneAtual);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
