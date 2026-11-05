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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.luan.olxapp.R;
import com.example.luan.olxapp.adapter.AdapterAnuncio;
import com.example.luan.olxapp.helper.ConfiguracaoFirebase;
import com.example.luan.olxapp.helper.FavoritosHelper;
import com.example.luan.olxapp.helper.NavegacaoInferior;
import com.example.luan.olxapp.helper.Precos;
import com.example.luan.olxapp.model.Anuncio;
import com.google.android.material.chip.Chip;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AnunciosActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private RecyclerView recyclerAnunciosPublicos;
    private AdapterAnuncio adapterAnuncio;
    private EditText editBusca;
    private TextView textVazio;
    private Chip chipRegiao, chipCategoria, chipPreco, chipOrdenar;

    // todos = tudo que veio do Firebase | anuncioList = o que está na tela (já filtrado)
    private final List<Anuncio> todosAnuncios = new ArrayList<>();
    private final List<Anuncio> anuncioList = new ArrayList<>();

    private DatabaseReference anunciosPublicosRef;
    private ValueEventListener anunciosListener;

    private String filtroEstado = null;     // null = todas as regiões
    private String filtroCategoria = null;  // null = todas as categorias
    private String filtroTexto = "";
    private long precoMin = -1, precoMax = -1; // em centavos; -1 = sem limite
    private int ordem = 0;                      // 0 recentes, 1 menor preço, 2 maior preço

    private final Set<String> favoritos = new HashSet<>();
    private ValueEventListener favoritosListener;
    private DatabaseReference favoritosRef;

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
        chipPreco = findViewById(R.id.chipPreco);
        chipOrdenar = findViewById(R.id.chipOrdenar);
        NavegacaoInferior.configurar(this, (BottomNavigationView) findViewById(R.id.bottomNav), R.id.nav_inicio);

        recyclerAnunciosPublicos.setLayoutManager(new GridLayoutManager(this, 2));
        adapterAnuncio = new AdapterAnuncio(anuncioList, this);
        recyclerAnunciosPublicos.setAdapter(adapterAnuncio);

        recuperarAnunciosPublicos();

        // Cliques do card: abrir detalhes e favoritar (coração)
        adapterAnuncio.setListener(new AdapterAnuncio.Listener() {
            @Override
            public void onAnuncioClick(Anuncio anuncio) {
                Intent i = new Intent(AnunciosActivity.this, DetalhesProdutoActivity.class);
                i.putExtra("anuncioSelecionado", anuncio);
                startActivity(i);
            }

            @Override
            public void onFavoritoClick(Anuncio anuncio) {
                FavoritosHelper.alternar(AnunciosActivity.this, anuncio, favoritos);
            }
        });

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

        chipPreco.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarFiltroPreco();
            }
        });
        chipPreco.setOnCloseIconClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                precoMin = -1;
                precoMax = -1;
                atualizarChip(chipPreco, "Preço", null);
                aplicarFiltros();
            }
        });

        chipOrdenar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String[] opcoes = {"Mais recentes", "Menor preço", "Maior preço"};
                new AlertDialog.Builder(AnunciosActivity.this)
                        .setTitle("Ordenar por")
                        .setItems(opcoes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ordem = which;
                                chipOrdenar.setText(opcoes[which]);
                                aplicarFiltros();
                            }
                        })
                        .show();
            }
        });
    }

    private void mostrarFiltroPreco() {
        android.widget.LinearLayout caixa = new android.widget.LinearLayout(this);
        caixa.setOrientation(android.widget.LinearLayout.VERTICAL);
        int p = (int) (20 * getResources().getDisplayMetrics().density);
        caixa.setPadding(p, p / 2, p, 0);

        final EditText min = new EditText(this);
        min.setHint("Preço mínimo (R$)");
        min.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        final EditText max = new EditText(this);
        max.setHint("Preço máximo (R$)");
        max.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if (precoMin >= 0) min.setText(String.valueOf(precoMin / 100));
        if (precoMax >= 0) max.setText(String.valueOf(precoMax / 100));
        caixa.addView(min);
        caixa.addView(max);

        new AlertDialog.Builder(this)
                .setTitle("Faixa de preço")
                .setView(caixa)
                .setPositiveButton("Aplicar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String a = min.getText().toString().trim();
                        String b = max.getText().toString().trim();
                        precoMin = a.isEmpty() ? -1 : Precos.centavos(a) * 100;
                        precoMax = b.isEmpty() ? -1 : Precos.centavos(b) * 100;
                        if (precoMin < 0 && precoMax < 0) {
                            atualizarChip(chipPreco, "Preço", null);
                        } else {
                            String txt = (precoMin >= 0 ? "R$ " + (precoMin / 100) : "R$ 0")
                                    + " – " + (precoMax >= 0 ? "R$ " + (precoMax / 100) : "sem limite");
                            atualizarChip(chipPreco, "Preço", txt);
                        }
                        aplicarFiltros();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
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
            long preco = Precos.centavos(a.getValor());
            if (precoMin >= 0 && preco < precoMin) continue;
            if (precoMax >= 0 && preco > precoMax) continue;
            anuncioList.add(a);
        }

        if (ordem == 1) {
            Collections.sort(anuncioList, new Comparator<Anuncio>() {
                @Override
                public int compare(Anuncio x, Anuncio y) {
                    return Long.compare(Precos.centavos(x.getValor()), Precos.centavos(y.getValor()));
                }
            });
        } else if (ordem == 2) {
            Collections.sort(anuncioList, new Comparator<Anuncio>() {
                @Override
                public int compare(Anuncio x, Anuncio y) {
                    return Long.compare(Precos.centavos(y.getValor()), Precos.centavos(x.getValor()));
                }
            });
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
        ouvirFavoritos();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (favoritosListener != null && favoritosRef != null) {
            favoritosRef.removeEventListener(favoritosListener);
            favoritosListener = null;
        }
    }

    private void ouvirFavoritos() {
        favoritos.clear();
        if (!NavegacaoInferior.logado()) {
            adapterAnuncio.setFavoritos(favoritos);
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
                adapterAnuncio.setFavoritos(favoritos);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        favoritosRef.addValueEventListener(favoritosListener);
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
