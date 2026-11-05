package com.example.luan.olxapp.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.luan.olxapp.R;
import com.example.luan.olxapp.adapter.AdapterAnuncio;
import com.example.luan.olxapp.helper.ConfiguracaoFirebase;
import com.example.luan.olxapp.helper.RecyclerItemClickListener;
import com.example.luan.olxapp.model.Anuncio;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AnunciosActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private RecyclerView recyclerAnunciosPublicos;
    private AdapterAnuncio adapterAnuncio;
    private EditText editBusca;
    private TextView textVazio;
    private Chip chipRegiao, chipCategoria;

    // todos = tudo que veio do Firebase | anuncioList = o que está na tela (já filtrado)
    private final List<Anuncio> todosAnuncios = new ArrayList<>();
    private final List<Anuncio> anuncioList = new ArrayList<>();

    private DatabaseReference anunciosPublicosRef;
    private ValueEventListener anunciosListener;

    private String filtroEstado = null;     // null = todas as regiões
    private String filtroCategoria = null;  // null = todas as categorias
    private String filtroTexto = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anuncios);

        auth = ConfiguracaoFirebase.getFirebaseAutenticacao();
        anunciosPublicosRef = ConfiguracaoFirebase.getFirebase().child("anuncios");

        recyclerAnunciosPublicos = findViewById(R.id.recyclerAnunciosPublicos);
        editBusca = findViewById(R.id.editBusca);
        textVazio = findViewById(R.id.textVazio);
        chipRegiao = findViewById(R.id.chipRegiao);
        chipCategoria = findViewById(R.id.chipCategoria);
        FloatingActionButton fabAnunciar = findViewById(R.id.fabAnunciar);

        recyclerAnunciosPublicos.setLayoutManager(new GridLayoutManager(this, 2));
        adapterAnuncio = new AdapterAnuncio(anuncioList, this);
        recyclerAnunciosPublicos.setAdapter(adapterAnuncio);

        recuperarAnunciosPublicos();

        // Clique no anúncio (a posição é da lista filtrada, que é a que está na tela)
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

        // Busca por texto enquanto digita
        editBusca.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtroTexto = s.toString();
                aplicarFiltros();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Chips de filtro: clique abre a lista, o "x" limpa o filtro
        chipRegiao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarEscolha("Selecione o estado", R.array.estados, new Escolha() {
                    @Override
                    public void escolheu(String valor) {
                        filtroEstado = valor;
                        atualizarChip(chipRegiao, "Região", filtroEstado);
                        aplicarFiltros();
                    }
                });
            }
        });
        chipRegiao.setOnCloseIconClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filtroEstado = null;
                atualizarChip(chipRegiao, "Região", null);
                aplicarFiltros();
            }
        });

        chipCategoria.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarEscolha("Selecione a categoria", R.array.categorias, new Escolha() {
                    @Override
                    public void escolheu(String valor) {
                        filtroCategoria = valor;
                        atualizarChip(chipCategoria, "Categoria", filtroCategoria);
                        aplicarFiltros();
                    }
                });
            }
        });
        chipCategoria.setOnCloseIconClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filtroCategoria = null;
                atualizarChip(chipCategoria, "Categoria", null);
                aplicarFiltros();
            }
        });

        // Botão "Anunciar": logado vai para o cadastro de anúncio, deslogado vai para o login
        fabAnunciar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (auth.getCurrentUser() == null) {
                    Toast.makeText(AnunciosActivity.this,
                            "Entre na sua conta para anunciar", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getApplicationContext(), CadastroActivity.class));
                } else {
                    startActivity(new Intent(getApplicationContext(), CadastrarAnuncioActivity.class));
                }
            }
        });
    }

    private interface Escolha {
        void escolheu(String valor);
    }

    private void mostrarEscolha(String titulo, int arrayRes, final Escolha escolha) {
        final String[] opcoes = getResources().getStringArray(arrayRes);
        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setItems(opcoes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        escolha.escolheu(opcoes[which]);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void atualizarChip(Chip chip, String padrao, String valor) {
        if (valor == null) {
            chip.setText(padrao);
            chip.setCloseIconVisible(false);
        } else {
            chip.setText(valor);
            chip.setCloseIconVisible(true);
        }
    }

    // Remove acentos e deixa minúsculo: "Câmera" casa com "camera"
    private String normalizar(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        return n.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase(Locale.getDefault());
    }

    private void aplicarFiltros() {
        String busca = normalizar(filtroTexto).trim();

        anuncioList.clear();
        for (Anuncio a : todosAnuncios) {
            if (filtroEstado != null && !filtroEstado.equals(a.getEstado())) continue;
            if (filtroCategoria != null && !filtroCategoria.equals(a.getCategoria())) continue;
            if (!busca.isEmpty()) {
                String texto = normalizar(a.getTitulo()) + " " + normalizar(a.getDescricao());
                if (!texto.contains(busca)) continue;
            }
            anuncioList.add(a);
        }

        adapterAnuncio.notifyDataSetChanged();
        textVazio.setVisibility(anuncioList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // Um único listener: baixa tudo, e os filtros são aplicados na memória.
    private void recuperarAnunciosPublicos() {
        anunciosListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // limpa antes de recarregar (evita anúncios duplicados quando o banco muda)
                todosAnuncios.clear();

                for (DataSnapshot estados : dataSnapshot.getChildren()) {
                    for (DataSnapshot categorias : estados.getChildren()) {
                        for (DataSnapshot anuncios : categorias.getChildren()) {
                            Anuncio anuncio = anuncios.getValue(Anuncio.class);
                            if (anuncio != null) todosAnuncios.add(anuncio);
                        }
                    }
                }

                Collections.reverse(todosAnuncios); // mais novos primeiro
                aplicarFiltros();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AnunciosActivity.this,
                        "Erro ao carregar anúncios", Toast.LENGTH_SHORT).show();
            }
        };
        anunciosPublicosRef.addValueEventListener(anunciosListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (anunciosListener != null) {
            anunciosPublicosRef.removeEventListener(anunciosListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu(); // atualiza o menu após login/logout
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean logado = auth.getCurrentUser() != null;
        menu.setGroupVisible(R.id.group_deslogado, !logado);
        menu.setGroupVisible(R.id.group_logado, logado);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_cadastrar) {
            startActivity(new Intent(getApplicationContext(), CadastroActivity.class));
        } else if (id == R.id.menu_sair) {
            auth.signOut();
            invalidateOptionsMenu();
        } else if (id == R.id.menu_anuncios) {
            startActivity(new Intent(getApplicationContext(), MeusAnunciosActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }
}
