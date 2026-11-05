package com.example.luan.olxapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.luan.olxapp.R;
import com.example.luan.olxapp.model.Mensagem;

import java.util.List;

public class AdapterMensagem extends RecyclerView.Adapter<AdapterMensagem.Holder> {

    private static final int MINHA = 1, OUTRA = 0;
    private final List<Mensagem> mensagens;
    private final String meuId;

    public AdapterMensagem(List<Mensagem> mensagens, String meuId) {
        this.mensagens = mensagens;
        this.meuId = meuId;
    }

    @Override
    public int getItemViewType(int position) {
        return meuId.equals(mensagens.get(position).getDe()) ? MINHA : OUTRA;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == MINHA ? R.layout.item_mensagem_minha : R.layout.item_mensagem_outra;
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.texto.setText(mensagens.get(position).getTexto());
    }

    @Override
    public int getItemCount() {
        return mensagens.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView texto;

        Holder(@NonNull View itemView) {
            super(itemView);
            texto = itemView.findViewById(R.id.textMensagem);
        }
    }
}
