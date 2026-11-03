package com.example.luan.olxapp.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.Toolbar;

import com.example.luan.olxapp.R;
import com.example.luan.olxapp.helper.ConfiguracaoFirebase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

public class CadastroActivity extends AppCompatActivity {

    private EditText campoEmail, campoSenha;
    private Switch tipoAcesso;
    private Button btnAcesso;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        campoEmail = findViewById(R.id.editEmail);
        campoSenha = findViewById(R.id.editSenha);
        btnAcesso = findViewById(R.id.btnAcessar);
        tipoAcesso = findViewById(R.id.switchAcesso);

        auth = ConfiguracaoFirebase.getFirebaseAutenticacao();


        btnAcesso.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String email = campoEmail.getText().toString();
                String senha = campoSenha.getText().toString();

                if (!email.isEmpty()){
                    if(!senha.isEmpty()){

                        //Verifica estado do switch
                        if(tipoAcesso.isChecked()){ //Cadastro

                            auth.createUserWithEmailAndPassword(
                              email, senha
                            ).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {

                                    if(task.isSuccessful()){
                                        showMessage("Cadastro realizado com sucesso!!");
                                    }
                                    else {
                                        String erroExcecao = "";

                                        try{
                                            throw task.getException();

                                        }catch (FirebaseAuthWeakPasswordException e){
                                            erroExcecao = "Digite uma senha mais forte!";
                                        }catch (FirebaseAuthInvalidCredentialsException e){
                                            erroExcecao = "Por favor, digite um e-mail válido";
                                        }catch (FirebaseAuthUserCollisionException e){
                                            erroExcecao = "Este conta já foi cadastrada";
                                        }catch (Exception e){
                                            erroExcecao = "ao cadastrar usuário: " + e.getMessage();
                                            e.printStackTrace();
                                        }

                                        showMessage(erroExcecao);
                                    }
                                }
                            });
                        }else{ //Login

                            auth.signInWithEmailAndPassword(
                                    email, senha
                            ).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {

                                    if ( task.isSuccessful()){

                                        showMessage("Logado com sucesso");
                                        startActivity(new Intent(getApplicationContext(), AnunciosActivity.class));

                                    }else{
                                        showMessage("Erro ao fazer login" + task.getException());
                                    }
                                }
                            });


                        }
                    }else{
                        showMessage("Preencha o campo senha");
                    }
                }else {
                    showMessage("Preencha o campo email");
                }
            }
        });

    }

    private void showMessage(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


}
