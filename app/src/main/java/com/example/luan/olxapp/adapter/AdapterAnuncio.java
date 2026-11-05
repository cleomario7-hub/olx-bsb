package com.example.luan.olxapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.luan.olxapp.R;
import com.example.luan.olxapp.model.Anuncio;
import com.squareup.picasso.Picasso;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdapterAnuncio extends RecyclerView.Adapter<AdapterAnuncio.MyViewHolder> {

    /** Se um Listener for definido, o card mostra o coração e responde a cliques. */
    public interface Listener {
        void onAnuncioClick(Anuncio anuncio);
        void onFavoritoClick(Anuncio anuncio);
    }

    private final List<Anuncio> anuncios;
    private final Context context;
    private Listener listener;
    private Set<String> favoritos = new HashSet<>();

    public AdapterAnuncio(List<Anuncio> anuncios, Context context) {
        this.anuncios = anuncios;
        this.context = context;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setFavoritos(Set<String> ids) {
        this.favoritos = ids;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_anuncio, parent, false);
        return new MyViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        final Anuncio anuncio = anuncios.get(position);
        holder.titulo.setText(anuncio.getTitulo());
        holder.valor.setText(anuncio.getValor());
        holder.local.setText(anuncio.getEstado() + " · " + anuncio.getCategoria());
        holder.vendido.setVisibility(anuncio.isVendido() ? View.VISIBLE : View.GONE);

        List<String> urlFotos = anuncio.getFotos();
        if (urlFotos != null && !urlFotos.isEmpty()) {
            Picasso.get().load(urlFotos.get(0)).placeholder(R.drawable.padrao).fit().centerCrop().into(holder.foto);
        } else {
            holder.foto.setImageResource(R.drawable.padrao);
        }

        if (listener != null) {
            boolean fav = anuncio.getIdAnuncio() != null && favoritos.contains(anuncio.getIdAnuncio());
            holder.favorito.setVisibility(View.VISIBLE);
            holder.favorito.setImageResource(fav ? R.drawable.ic_favorite_24dp : R.drawable.ic_favorite_border_24dp);
            holder.favorito.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onFavoritoClick(anuncio);
                }
            });
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onAnuncioClick(anuncio);
                }
            });
        } else {
            holder.favorito.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return anuncios.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView titulo, valor, local, vendido;
        ImageView foto, favorito;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            titulo = itemView.findViewById(R.id.textTitulo);
            valor = itemView.findViewById(R.id.textValor);
            local = itemView.findViewById(R.id.textLocal);
            vendido = itemView.findViewById(R.id.textVendido);
            foto = itemView.findViewById(R.id.imageAnuncio);
            favorito = itemView.findViewById(R.id.imageFavorito);
        }
    }
}
