package com.example.luan.olxapp.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.blackcat.currencyedittext.CurrencyEditText;
import com.example.luan.olxapp.R;
import com.example.luan.olxapp.helper.ConfiguracaoFirebase;
import com.example.luan.olxapp.helper.Permissoes;
import com.example.luan.olxapp.model.Anuncio;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.santalu.maskedittext.MaskEditText;

import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import dmax.dialog.SpotsDialog;

public class CadastrarAnuncioActivity extends AppCompatActivity {

    private EditText campoTitulo, campoDescricao;
    private CurrencyEditText campoValor;
    private MaskEditText campoTelefone;
    private Button btnCadastrarAnuncio;
    private ImageView img1, img2, img3;
    private Spinner spUF, spCategoria;
    private StorageReference storage;
    private AlertDialog dialog;

    private String[] permissoes = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private String[] fotosRecuperadas = new String[3];
    private ArrayList<String>listaURLFotos = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastrar_anuncio);

        img1 = findViewById(R.id.img1);
        img2 = findViewById(R.id.img2);
        img3 = findViewById(R.id.img3);

        spUF = findViewById(R.id.spUF);
        spCategoria = findViewById(R.id.spCategoria);
        campoTitulo = findViewById(R.id.editTitulo);
        campoDescricao = findViewById(R.id.editDescricao);
        campoValor = findViewById(R.id.editValor);
        campoTelefone = findViewById(R.id.editTelefone);
        btnCadastrarAnuncio = findViewById(R.id.btnCadastrarAnuncio);

        Locale locale = new Locale("pt","BR");
        campoValor.setLocale(locale);

        //Validar Permissões
        Permissoes.validarPermissoes(permissoes,this,1);

        storage = ConfiguracaoFirebase.getFirebaseStorage();


        carregarDadosSpinner();


        btnCadastrarAnuncio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                validarDadosAnuncio();
            }
        });
    }

    private void salvarAnuncio(final Anuncio anuncio){

        dialog = new SpotsDialog.Builder()
                .setContext(this)
                .setMessage("Salvando Anúncio")
                .build();
        dialog.show();

        /*
         *Salvar imagem no Storage
         */
        for (int i = 0; i < fotosRecuperadas.length; i++) {

            final StorageReference imagemAnuncio = storage.child("imagens")
                    .child("anuncios")
                    .child(anuncio.getIdAnuncio())
                    .child("Imagem" + i);

            //Fazer upload do arquivo
            UploadTask uploadTask = imagemAnuncio.putFile( Uri.parse(fotosRecuperadas[i]));

            Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()){
                        throw task.getException();
                    }
                    return imagemAnuncio.getDownloadUrl();
                }
            }).addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    listaURLFotos.add(uri.toString());

                    if(listaURLFotos.size() == fotosRecuperadas.length){
                        anuncio.setFotos(listaURLFotos);
                        anuncio.salvar();
                        dialog.dismiss();
                        Toast.makeText(CadastrarAnuncioActivity.this,"Anúncio Cadastrado",Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
            });
        }
    }

    private void validarDadosAnuncio(){

        if(fotosRecuperadas[0] != null || fotosRecuperadas[1] != null || fotosRecuperadas[2] != null){
            String fone = "";

            String estado = spUF.getSelectedItem().toString();
            String categoria = spCategoria.getSelectedItem().toString();
            String titulo = campoTitulo.getText().toString();
            String valor = campoValor.getText().toString();
            String telefone = campoTelefone.getText().toString();
            if(campoTelefone.getRawText() != null){
                fone = campoTelefone.getRawText();
            }
            String descricao = campoDescricao.getText().toString();

            if( !estado.isEmpty()){
                if( !categoria.isEmpty()){
                    if( !titulo.isEmpty()){
                        if( !valor.isEmpty() && !valor.equals("0")){
                            if( !telefone.isEmpty() && fone.length() >= 10){

                                Anuncio anuncio = new Anuncio(estado,categoria,titulo,valor,telefone,descricao);
                                salvarAnuncio(anuncio);

                            }else{
                                showToast("Preencha o campo telefone, digite ao menos 10 números!");
                            }
                        }else{
                            showToast("Preencha o campo valor");
                        }
                    }else{
                        showToast("Preencha o campo titulo");
                    }
                }else{
                    showToast("Preencha o campo categoria");
                }
            }else{
                showToast("Preencha o campo estado!");
            }


        }else{
            showToast("Selecione ao menos uma foto!");
        }
    }

    private void showToast(String mensagem){
        Toast.makeText(this, mensagem,Toast.LENGTH_SHORT).show();
    }


    public void onClickImg(View v){

        switch (v.getId()){
            case R.id.img1:
                escolherImg(1);
                break;
            case R.id.img2:
                escolherImg(2);
                break;
            case R.id.img3:
                escolherImg(3);
                break;
        }
    }

    public void escolherImg(int requestCode){
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i,requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int permissaoResultado : grantResults){
            if(permissaoResultado == PackageManager.PERMISSION_DENIED){
                showAlertPermissao();
            }
        }
    }

    private void showAlertPermissao(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permissões Negadas");
        builder.setMessage("Para utilizar o app é necessário aceitar as permissões");
        builder.setCancelable(false);
        builder.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK){

            //Recuperar Imagem
            Uri imagemSelecionada = data.getData();
            String caminhoImagem = imagemSelecionada.toString();

            //Configura imagem no ImageView
            if(requestCode == 1){
                img1.setImageURI(imagemSelecionada);
                //listaFotosRecuperadas.add(caminhoImagem);
                fotosRecuperadas[0] = caminhoImagem;
            }else if( requestCode == 2){
                img2.setImageURI(imagemSelecionada);
                fotosRecuperadas[1] = caminhoImagem;
            }else if(requestCode == 3){
                img3.setImageURI(imagemSelecionada);
                fotosRecuperadas[2] = caminhoImagem;
            }

            //listaFotosRecuperadas.add(caminhoImagem);
        }
    }

    private void carregarDadosSpinner(){

        String[] estados = getResources().getStringArray(R.array.estados);
        String[] categorias = getResources().getStringArray(R.array.categorias);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                estados
        );
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        spUF.setAdapter(adapter);


        ArrayAdapter<String> adapterCategoria = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                categorias
        );
        adapterCategoria.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        spCategoria.setAdapter( adapterCategoria);
    }


}
