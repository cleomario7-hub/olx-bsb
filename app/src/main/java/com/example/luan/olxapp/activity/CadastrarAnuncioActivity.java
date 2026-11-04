package com.example.luan.olxapp.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
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

import com.example.luan.olxapp.helper.ProgressoDialog;
import com.example.luan.olxapp.helper.Recursos;

public class CadastrarAnuncioActivity extends AppCompatActivity {

    private EditText campoTitulo, campoDescricao;
    private CurrencyEditText campoValor;
    private MaskEditText campoTelefone;
    private Button btnCadastrarAnuncio;
    private ImageView img1, img2, img3;
    private Spinner spUF, spCategoria;
    private StorageReference storage;
    private AlertDialog dialog;

    private String[] fotosRecuperadas = new String[3];
    private ArrayList<String>listaURLFotos = new ArrayList<>();

    // Qual foto (1, 2 ou 3) está sendo escolhida no momento
    private int fotoSelecionando = 0;

    // Seletor de fotos do sistema (não precisa de permissão de armazenamento)
    private final ActivityResultLauncher<PickVisualMediaRequest> seletorFoto =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri == null) return; // usuário cancelou

                if (fotoSelecionando == 1) {
                    img1.setImageURI(uri);
                    fotosRecuperadas[0] = uri.toString();
                } else if (fotoSelecionando == 2) {
                    img2.setImageURI(uri);
                    fotosRecuperadas[1] = uri.toString();
                } else if (fotoSelecionando == 3) {
                    img3.setImageURI(uri);
                    fotosRecuperadas[2] = uri.toString();
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastrar_anuncio);

        img1 = findViewById(R.id.img1);
        img2 = findViewById(R.id.img2);
        img3 = findViewById(R.id.img3);

        //Sem Storage ativo, esconde a escolha de fotos
        if (!Recursos.FOTOS_ATIVAS) {
            ((View) img1.getParent()).setVisibility(View.GONE);
        }

        spUF = findViewById(R.id.spUF);
        spCategoria = findViewById(R.id.spCategoria);
        campoTitulo = findViewById(R.id.editTitulo);
        campoDescricao = findViewById(R.id.editDescricao);
        campoValor = findViewById(R.id.editValor);
        campoTelefone = findViewById(R.id.editTelefone);
        btnCadastrarAnuncio = findViewById(R.id.btnCadastrarAnuncio);

        Locale locale = new Locale("pt","BR");
        campoValor.setLocale(locale);

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

        dialog = ProgressoDialog.criar(this, "Salvando Anúncio", true);
        dialog.show();

        listaURLFotos.clear();

        //Fotos escolhidas (só valem se o recurso de fotos estiver ativo)
        final ArrayList<String> fotosParaEnviar = new ArrayList<>();
        if (Recursos.FOTOS_ATIVAS) {
            for (String foto : fotosRecuperadas) {
                if (foto != null) fotosParaEnviar.add(foto);
            }
        }

        //Sem fotos: salva direto
        if (fotosParaEnviar.isEmpty()) {
            finalizarCadastro(anuncio);
            return;
        }

        /*
         *Salvar imagens no Storage
         */
        for (int i = 0; i < fotosParaEnviar.size(); i++) {

            final StorageReference imagemAnuncio = storage.child("imagens")
                    .child("anuncios")
                    .child(anuncio.getIdAnuncio())
                    .child("Imagem" + i);

            //Fazer upload do arquivo
            UploadTask uploadTask = imagemAnuncio.putFile( Uri.parse(fotosParaEnviar.get(i)));

            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
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

                    if(listaURLFotos.size() == fotosParaEnviar.size()){
                        anuncio.setFotos(listaURLFotos);
                        finalizarCadastro(anuncio);
                    }
                }
            }).addOnFailureListener(e -> {
                dialog.dismiss();
                showToast("Não foi possível enviar as fotos. Tente novamente.");
            });
        }
    }

    private void finalizarCadastro(Anuncio anuncio){
        anuncio.salvar();
        dialog.dismiss();
        Toast.makeText(CadastrarAnuncioActivity.this,"Anúncio Cadastrado",Toast.LENGTH_SHORT).show();
        finish();
    }

    private void validarDadosAnuncio(){

        if(!Recursos.FOTOS_ATIVAS || fotosRecuperadas[0] != null || fotosRecuperadas[1] != null || fotosRecuperadas[2] != null){
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

        int id = v.getId();

        if (id == R.id.img1) {
            escolherImg(1);
        } else if (id == R.id.img2) {
            escolherImg(2);
        } else if (id == R.id.img3) {
            escolherImg(3);
        }
    }

    public void escolherImg(int numeroFoto){
        fotoSelecionando = numeroFoto;
        seletorFoto.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
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
