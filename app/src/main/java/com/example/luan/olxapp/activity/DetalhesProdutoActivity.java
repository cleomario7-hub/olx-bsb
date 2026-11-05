package com.example.luan.olxapp.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.viewpager2.widget.ViewPager2;

import com.example.luan.olxapp.R;
import com.example.luan.olxapp.helper.ConfiguracaoFirebase;
import com.example.luan.olxapp.helper.NavegacaoInferior;
import com.example.luan.olxapp.model.Anuncio;
import android.widget.Toast;
import com.example.luan.olxapp.adapter.AdapterFotos;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class DetalhesProdutoActivity extends AppCompatActivity {

    private static final long INTERVALO_SLIDE_MS = 3000;

    private ViewPager2 viewPagerFotos;
    private TabLayout tabIndicadorFotos;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable proximaFoto = new Runnable() {
        @Override
        public void run() {
            RecyclerView.Adapter<?> adapter = viewPagerFotos.getAdapter();
            if (adapter != null && adapter.getItemCount() > 1) {
                int proxima = (viewPagerFotos.getCurrentItem() + 1) % adapter.getItemCount();
                viewPagerFotos.setCurrentItem(proxima, true);
                handler.postDelayed(this, INTERVALO_SLIDE_MS);
            }
        }
    };
    private TextView titulo, descricao, estado,preco;
    private Anuncio anuncioSelecionado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhes_produto);

        getSupportActionBar().setTitle("Detalhe produto");

        viewPagerFotos = findViewById(R.id.viewPagerFotos);
        tabIndicadorFotos = findViewById(R.id.tabIndicadorFotos);
        titulo = findViewById(R.id.textTituloDetalhe);
        descricao = findViewById(R.id.textDescricaoDetalhe);
        estado = findViewById(R.id.textEstadoDetalhe);
        preco = findViewById(R.id.textPrecoDetalhe);


        anuncioSelecionado = (Anuncio) getIntent().getSerializableExtra("anuncioSelecionado");

        if(anuncioSelecionado != null){
            titulo.setText((anuncioSelecionado.isVendido() ? "[VENDIDO] " : "") + anuncioSelecionado.getTitulo());
            descricao.setText(anuncioSelecionado.getDescricao());
            estado.setText(anuncioSelecionado.getEstado());
            preco.setText(anuncioSelecionado.getValor());
        }

        if (anuncioSelecionado != null && anuncioSelecionado.getFotos() != null
                && !anuncioSelecionado.getFotos().isEmpty()) {
            viewPagerFotos.setAdapter(new AdapterFotos(anuncioSelecionado.getFotos()));
            new TabLayoutMediator(tabIndicadorFotos, viewPagerFotos, (tab, position) -> { }).attach();
        } else {
            //Anúncio sem foto: esconde o slide
            findViewById(R.id.containerFotos).setVisibility(View.GONE);
        }

        Button btnVerTelefone = findViewById(R.id.btnVerTelefone);
        findViewById(R.id.btnConversar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirChat();
            }
        });
        btnVerTelefone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                visualizarTelefone();
            }
        });
    }

    private void abrirChat(){
        if (anuncioSelecionado == null) return;
        if (!NavegacaoInferior.logado()) {
            NavegacaoInferior.pedirLogin(this, "Entre na sua conta para conversar");
            return;
        }
        String dono = anuncioSelecionado.getIdUsuario();
        if (dono == null || anuncioSelecionado.getIdAnuncio() == null) {
            Toast.makeText(this, "Este anúncio é antigo e ainda não permite chat", Toast.LENGTH_LONG).show();
            return;
        }
        String eu = ConfiguracaoFirebase.getIdUsuario();
        if (dono.equals(eu)) {
            Toast.makeText(this, "Este é o seu anúncio", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(this, ChatActivity.class);
        i.putExtra("chatId", anuncioSelecionado.getIdAnuncio() + "_" + eu); // um chat por anúncio e comprador
        i.putExtra("outroId", dono);
        i.putExtra("idAnuncio", anuncioSelecionado.getIdAnuncio());
        i.putExtra("tituloAnuncio", anuncioSelecionado.getTitulo());
        startActivity(i);
    }

    private void visualizarTelefone(){
        Intent i = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", anuncioSelecionado.getTelefone(), null));
        startActivity(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(proximaFoto, INTERVALO_SLIDE_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(proximaFoto);
    }
}
