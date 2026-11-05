package com.example.luan.olxapp.helper;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.example.luan.olxapp.R;
import com.example.luan.olxapp.activity.AnunciosActivity;
import com.example.luan.olxapp.activity.CadastrarAnuncioActivity;
import com.example.luan.olxapp.activity.CadastroActivity;
import com.example.luan.olxapp.activity.ContaActivity;
import com.example.luan.olxapp.activity.ConversasActivity;
import com.example.luan.olxapp.activity.FavoritosActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/** Barra de navegação inferior usada por Início, Favoritos, Chat e Conta. */
public class NavegacaoInferior {

    public static boolean logado() {
        return ConfiguracaoFirebase.getFirebaseAutenticacao().getCurrentUser() != null;
    }

    public static void pedirLogin(Activity activity, String motivo) {
        Toast.makeText(activity, motivo, Toast.LENGTH_SHORT).show();
        activity.startActivity(new Intent(activity, CadastroActivity.class));
    }

    public static void configurar(final Activity activity, BottomNavigationView nav, final int itemAtual) {
        nav.setSelectedItemId(itemAtual);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == itemAtual) return true;

            if (id == R.id.nav_anunciar) {
                if (!logado()) {
                    pedirLogin(activity, "Entre na sua conta para anunciar");
                } else {
                    activity.startActivity(new Intent(activity, CadastrarAnuncioActivity.class));
                }
                return false;
            }

            Class<?> destino;
            if (id == R.id.nav_inicio) {
                destino = AnunciosActivity.class;
            } else if (id == R.id.nav_favoritos) {
                if (!logado()) { pedirLogin(activity, "Entre para ver seus favoritos"); return false; }
                destino = FavoritosActivity.class;
            } else if (id == R.id.nav_chat) {
                if (!logado()) { pedirLogin(activity, "Entre para ver suas conversas"); return false; }
                destino = ConversasActivity.class;
            } else {
                destino = ContaActivity.class;
            }

            Intent i = new Intent(activity, destino);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            activity.startActivity(i);
            if (!(activity instanceof AnunciosActivity)) {
                activity.finish(); // mantém a pilha: Início -> tela atual
            }
            return false;
        });
    }
}
