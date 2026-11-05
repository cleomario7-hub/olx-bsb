package com.example.luan.olxapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.luan.olxapp.R;
import com.example.luan.olxapp.model.Conversa;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdapterConversa extends RecyclerView.Adapter<AdapterConversa.Holder> {

    public interface Listener {
        void onConversaClick(Conversa conversa);
    }

    private final List<Conversa> conversas;
    private final Listener listener;
    private final SimpleDateFormat formato = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

    public AdapterConversa(List<Conversa> conversas, Listener listener) {
        this.conversas = conversas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversa, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        final Conversa c = conversas.get(position);
        h.nome.setText(c.getOutroNome() == null ? "Usuário" : c.getOutroNome());
        h.anuncio.setText(c.getTituloAnuncio());
        h.ultima.setText(c.getUltimaMensagem());
        h.hora.setText(c.getHora() > 0 ? formato.format(new Date(c.getHora())) : "");
        h.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onConversaClick(c);
            }
        });
    }

    @Override
    public int getItemCount() {
        return conversas.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView nome, anuncio, ultima, hora;

        Holder(@NonNull View v) {
            super(v);
            nome = v.findViewById(R.id.textConversaNome);
            anuncio = v.findViewById(R.id.textConversaAnuncio);
            ultima = v.findViewById(R.id.textConversaUltima);
            hora = v.findViewById(R.id.textConversaHora);
        }
    }
}
