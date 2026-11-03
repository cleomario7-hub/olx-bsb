package com.example.luan.olxapp.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.luan.olxapp.R;
import com.example.luan.olxapp.model.Anuncio;
import com.squareup.picasso.Picasso;
import com.synnapps.carouselview.CarouselView;
import com.synnapps.carouselview.ImageListener;

public class DetalhesProdutoActivity extends AppCompatActivity {

    private CarouselView carouselView;
    private TextView titulo, descricao, estado,preco;
    private Anuncio anuncioSelecionado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhes_produto);

        getSupportActionBar().setTitle("Detalhe produto");

        carouselView = findViewById(R.id.carouselView);
        titulo = findViewById(R.id.textTituloDetalhe);
        descricao = findViewById(R.id.textDescricaoDetalhe);
        estado = findViewById(R.id.textEstadoDetalhe);
        preco = findViewById(R.id.textPrecoDetalhe);


        anuncioSelecionado = (Anuncio) getIntent().getSerializableExtra("anuncioSelecionado");

        if(anuncioSelecionado != null){
            titulo.setText(anuncioSelecionado.getTitulo());
            descricao.setText(anuncioSelecionado.getDescricao());
            estado.setText(anuncioSelecionado.getEstado());
            preco.setText(anuncioSelecionado.getValor());
        }

        ImageListener imageListener = new ImageListener() {
            @Override
            public void setImageForPosition(int position, ImageView imageView) {
                String urlString = anuncioSelecionado.getFotos().get(position);
                Picasso.get().load(urlString).into(imageView);
            }
        };

        carouselView.setPageCount(anuncioSelecionado.getFotos().size());
        carouselView.setImageListener(imageListener);

        Button btnVerTelefone = findViewById(R.id.btnVerTelefone);
        btnVerTelefone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                visualizarTelefone();
            }
        });
    }

    private void visualizarTelefone(){
        Intent i = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", anuncioSelecionado.getTelefone(), null));
        startActivity(i);
    }
}
