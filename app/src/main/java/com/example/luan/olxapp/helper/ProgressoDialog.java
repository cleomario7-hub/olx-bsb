package com.example.luan.olxapp.helper;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.luan.olxapp.R;

/**
 * Dialog de progresso simples (substitui a biblioteca SpotsDialog, que não é mais mantida).
 */
public class ProgressoDialog {

    public static AlertDialog criar(Context context, String mensagem, boolean cancelavel) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_progresso, null);
        ((TextView) view.findViewById(R.id.textMensagemProgresso)).setText(mensagem);

        return new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(cancelavel)
                .create();
    }
}
