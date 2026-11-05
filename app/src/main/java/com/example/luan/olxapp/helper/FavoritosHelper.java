package com.example.luan.olxapp.helper;

import android.app.Activity;

import com.example.luan.olxapp.model.Anuncio;
import com.google.firebase.database.DatabaseReference;

import java.util.Set;

public class FavoritosHelper {

    public static DatabaseReference referencia() {
        return ConfiguracaoFirebase.getFirebase()
                .child("favoritos").child(ConfiguracaoFirebase.getIdUsuario());
    }

    /** Favorita ou desfavorita. Se o usuário não estiver logado, abre o login. */
    public static void alternar(Activity activity, Anuncio anuncio, Set<String> favoritosAtuais) {
        if (!NavegacaoInferior.logado()) {
            NavegacaoInferior.pedirLogin(activity, "Entre para favoritar anúncios");
            return;
        }
        if (anuncio.getIdAnuncio() == null) return;
        DatabaseReference ref = referencia().child(anuncio.getIdAnuncio());
        if (favoritosAtuais.contains(anuncio.getIdAnuncio())) {
            ref.removeValue();
        } else {
            ref.setValue(true);
        }
    }
}
