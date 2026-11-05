package com.example.luan.olxapp.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.luan.olxapp.R;
import com.example.luan.olxapp.helper.ConfiguracaoFirebase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.santalu.maskedittext.MaskEditText;

import java.util.HashMap;
import java.util.Map;

/**
 * Tela única de acesso: "Entrar" (login) ou "Criar conta" (cadastro).
 * No cadastro também guarda o perfil (nome e telefone) em usuarios/{uid}.
 */
public class CadastroActivity extends AppCompatActivity {

    private EditText campoNome, campoEmail, campoSenha, campoConfirmarSenha;
    private MaskEditText campoTelefone;
    private TextView textTitulo, textAlternar, textEsqueciSenha;
    private Button btnAcesso;

    private FirebaseAuth auth;
    private boolean modoCadastro = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        auth = ConfiguracaoFirebase.getFirebaseAutenticacao();

        campoNome = findViewById(R.id.editNome);
        campoTelefone = findViewById(R.id.editTelefone);
        campoEmail = findViewById(R.id.editEmail);
        campoSenha = findViewById(R.id.editSenha);
        campoConfirmarSenha = findViewById(R.id.editConfirmarSenha);
        textTitulo = findViewById(R.id.textTitulo);
        textAlternar = findViewById(R.id.textAlternar);
        textEsqueciSenha = findViewById(R.id.textEsqueciSenha);
        btnAcesso = findViewById(R.id.btnAcessar);

        atualizarModo();

        textAlternar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modoCadastro = !modoCadastro;
                atualizarModo();
            }
        });

        textEsqueciSenha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recuperarSenha();
            }
        });

        btnAcesso.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (modoCadastro) {
                    cadastrar();
                } else {
                    entrar();
                }
            }
        });
    }

    private void atualizarModo() {
        int cadastro = modoCadastro ? View.VISIBLE : View.GONE;
        campoNome.setVisibility(cadastro);
        campoTelefone.setVisibility(cadastro);
        campoConfirmarSenha.setVisibility(cadastro);
        textEsqueciSenha.setVisibility(modoCadastro ? View.GONE : View.VISIBLE);

        textTitulo.setText(modoCadastro ? "Criar sua conta" : "Entrar na sua conta");
        btnAcesso.setText(modoCadastro ? "Criar conta" : "Entrar");
        textAlternar.setText(modoCadastro
                ? "Já tem conta? Entrar"
                : "Ainda não tem conta? Cadastre-se");
    }

    // ---------------------------------------------------------------- login
    private void entrar() {
        String email = campoEmail.getText().toString().trim();
        String senha = campoSenha.getText().toString();

        if (!emailValido(email)) return;
        if (senha.isEmpty()) {
            campoSenha.setError("Digite a senha");
            campoSenha.requestFocus();
            return;
        }

        btnAcesso.setEnabled(false);
        auth.signInWithEmailAndPassword(email, senha)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        btnAcesso.setEnabled(true);
                        if (task.isSuccessful()) {
                            irParaHome();
                        } else {
                            mensagem(traduzirErro(task.getException()));
                        }
                    }
                });
    }

    // ------------------------------------------------------------- cadastro
    private void cadastrar() {
        final String nome = campoNome.getText().toString().trim();
        final String telefone = campoTelefone.getText() == null ? "" : campoTelefone.getText().toString();
        final String email = campoEmail.getText().toString().trim();
        String senha = campoSenha.getText().toString();
        String confirmar = campoConfirmarSenha.getText().toString();

        if (nome.length() < 2) {
            campoNome.setError("Digite seu nome");
            campoNome.requestFocus();
            return;
        }
        // "(61) 91234-5678" tem 15 caracteres; "(61) 1234-5678" teria menos dígitos
        int digitos = telefone.replaceAll("\\D", "").length();
        if (digitos < 10) {
            campoTelefone.setError("Digite o telefone com DDD");
            campoTelefone.requestFocus();
            return;
        }
        if (!emailValido(email)) return;
        if (senha.length() < 6) {
            campoSenha.setError("A senha precisa ter pelo menos 6 caracteres");
            campoSenha.requestFocus();
            return;
        }
        if (!senha.equals(confirmar)) {
            campoConfirmarSenha.setError("As senhas não são iguais");
            campoConfirmarSenha.requestFocus();
            return;
        }

        btnAcesso.setEnabled(false);
        auth.createUserWithEmailAndPassword(email, senha)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            salvarPerfil(nome, telefone, email);
                        } else {
                            btnAcesso.setEnabled(true);
                            mensagem(traduzirErro(task.getException()));
                        }
                    }
                });
    }

    // Grava nome/telefone em usuarios/{uid}. Se falhar, a conta já existe e o usuário entra mesmo assim.
    private void salvarPerfil(String nome, String telefone, String email) {
        FirebaseUser usuario = auth.getCurrentUser();
        if (usuario == null) {
            irParaHome();
            return;
        }

        Map<String, Object> perfil = new HashMap<>();
        perfil.put("nome", nome);
        perfil.put("telefone", telefone);
        perfil.put("email", email);

        ConfiguracaoFirebase.getFirebase()
                .child("usuarios")
                .child(usuario.getUid())
                .setValue(perfil)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (!task.isSuccessful()) {
                            Toast.makeText(CadastroActivity.this,
                                    "Conta criada, mas não foi possível salvar o perfil agora.",
                                    Toast.LENGTH_LONG).show();
                        }
                        irParaHome();
                    }
                });
    }

    // ------------------------------------------------------ esqueci a senha
    private void recuperarSenha() {
        final EditText campo = new EditText(this);
        campo.setHint(getString(R.string.email));
        campo.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        campo.setText(campoEmail.getText());
        int p = (int) (20 * getResources().getDisplayMetrics().density);
        campo.setPadding(p, p, p, p);

        new AlertDialog.Builder(this)
                .setTitle("Recuperar senha")
                .setMessage("Enviaremos um link para redefinir sua senha.")
                .setView(campo)
                .setPositiveButton("Enviar", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        String email = campo.getText().toString().trim();
                        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            mensagem("Digite um e-mail válido");
                            return;
                        }
                        auth.sendPasswordResetEmail(email)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            mensagem("Se o e-mail estiver cadastrado, você receberá o link.");
                                        } else {
                                            mensagem(traduzirErro(task.getException()));
                                        }
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // -------------------------------------------------------------- helpers
    private boolean emailValido(String email) {
        if (email.isEmpty()) {
            campoEmail.setError("Digite o e-mail");
            campoEmail.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            campoEmail.setError("E-mail inválido");
            campoEmail.requestFocus();
            return false;
        }
        return true;
    }

    private String traduzirErro(Exception e) {
        // Atenção à ordem: WeakPassword é um tipo de InvalidCredentials
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return "Senha muito fraca. Use pelo menos 6 caracteres.";
        } else if (e instanceof FirebaseAuthUserCollisionException) {
            return "Já existe uma conta com esse e-mail. Tente entrar.";
        } else if (e instanceof FirebaseAuthInvalidUserException) {
            return "Não encontramos uma conta com esse e-mail.";
        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return modoCadastro ? "E-mail inválido." : "E-mail ou senha incorretos.";
        } else if (e instanceof FirebaseNetworkException) {
            return "Sem conexão com a internet. Tente novamente.";
        }
        return "Erro: " + (e == null ? "desconhecido" : e.getMessage());
    }

    private void mensagem(String texto) {
        Toast.makeText(this, texto, Toast.LENGTH_LONG).show();
    }

    private void irParaHome() {
        Intent i = new Intent(getApplicationContext(), AnunciosActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }
}
