package it.unibo.studio.unigo.main.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.DetailActivity;
import it.unibo.studio.unigo.main.FullSizeImageActivity;
import it.unibo.studio.unigo.main.PostActivity;

public class DetailPictureAdapter extends Adapter<DetailPictureAdapter.ImageHolder>
{
    private final int UPLOAD = 0;
    private final int DOWNLOAD = 1;
    private final String IMAGE_PATH = "path";
    private final String LOAD_FROM_FILE = "fromFile";
    private List<String> pictureList;
    // Variabile che stabilisce se attribuire l'icona di rimozione alle immagini o meno. L'adapter viene utilizzato in due contesti differenti:
    // - PostActivity, type = UPLOAD, le immagini avranno l'icona 'rimuovi' per annullare l'upload del file
    // - DetailActivity, type = DOWNLOAD, le immagini non avranno l'icona per rimuovere l'immagine in quanto è già caricata su Firebase Storage
    private int type;

    class ImageHolder extends RecyclerView.ViewHolder
    {
        Context context;
        RelativeLayout detailLayout;
        ImageView detailPic, detailPhotoIcon;
        TextView detailText;

        ImageHolder(View v)
        {
            super(v);
            context = v.getContext();
            detailLayout = (RelativeLayout) v.findViewById(R.id.detailLayout);
            detailPic = (ImageView) v.findViewById(R.id.detailPic);
            detailText = (TextView) v.findViewById(R.id.detailPhotoTxt);
            detailPhotoIcon = (ImageView) v.findViewById(R.id.detailPhotoIcon);
        }
    }

    // Costruttore richiamato da DetailActivity, contiene la lista di tutte le immagini da scaricare
    DetailPictureAdapter(List<String> pictureList)
    {
        this.pictureList = pictureList;
        type = DOWNLOAD;
    }

    // Costruttore richiamato da PostActivity, le immagini vengono caricare man mano che vengono selezionate dall'utente
    public DetailPictureAdapter()
    {
        this.pictureList = new ArrayList<>();
        type = UPLOAD;
    }

    @Override
    public ImageHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        return new ImageHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_detail_picture, parent, false));
    }

    @Override
    public void onBindViewHolder(final ImageHolder holder, final int position)
    {
        // Nome immagine
        holder.detailText.setText(pictureList.get(position).substring(pictureList.get(position).lastIndexOf("/")+1));

        // Viene controllato se l'adapter è quello dedicato alla creazione di un post (dove le immagini possono essere rimosse),
        // oppure se è quello dedicato alla sola visualizzazione
        switch (type)
        {
            case DOWNLOAD:
                // Inizio dell'animazione di attesa del download dell'immagine da Internet
                ((Animatable) holder.detailPic.getDrawable()).start();

                // Caricamento immagine da Internet
                Picasso.with(holder.context).load(pictureList.get(holder.getAdapterPosition())).noPlaceholder().into(holder.detailPic, new Callback() {
                    @Override
                    public void onSuccess() {holder.detailPic.setVisibility(View.VISIBLE); }

                    @Override
                    public void onError()
                    {
                        Toast.makeText(holder.context, "Holy Fuuuck!", Toast.LENGTH_SHORT).show();
                        holder.detailPic.setVisibility(View.GONE);
                    }
                });
                // ClickListener per visualizzare a pieno schermo l'immagine
                holder.detailPic.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        Intent intent = new Intent(holder.context, FullSizeImageActivity.class);
                        intent.putExtra(IMAGE_PATH, pictureList.get(holder.getAdapterPosition()));
                        intent.putExtra(LOAD_FROM_FILE, false);
                        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation((DetailActivity) holder.context, holder.detailPic, holder.detailPic.getTransitionName());
                        holder.context.startActivity(intent, options.toBundle());
                    }
                });

                // Viene nascosto il pulsante per rimuovere la foto
                holder.detailPhotoIcon.setVisibility(View.GONE);
                break;

            case UPLOAD:
                // Caricamento dell'immagine da file
                Picasso.with(holder.context).load(new File(pictureList.get(holder.getAdapterPosition()))).into(holder.detailPic, new Callback() {
                    @Override
                    public void onSuccess() {holder.detailPic.setVisibility(View.VISIBLE); }

                    @Override
                    public void onError()
                    {
                        Toast.makeText(holder.context, "Holy Fuuuck!", Toast.LENGTH_SHORT).show();
                        holder.detailPic.setVisibility(View.GONE);
                    }
                });
                // ClickListener per visualizzare a pieno schermo l'immagine
                holder.detailPic.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        Intent intent = new Intent(holder.context, FullSizeImageActivity.class);
                        intent.putExtra(IMAGE_PATH, pictureList.get(holder.getAdapterPosition()));
                        intent.putExtra(LOAD_FROM_FILE, true);
                        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation((PostActivity) holder.context, holder.detailPic, holder.detailPic.getTransitionName());
                        holder.context.startActivity(intent, options.toBundle());
                    }
                });

                // Viene mostrato il pulsante di rimozione dell'immagine
                holder.detailPhotoIcon.setVisibility(View.VISIBLE);
                holder.detailPhotoIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        removeElement(holder.getAdapterPosition());
                    }
                });
                break;
        }
    }

    @Override
    public int getItemCount()
    {
        return pictureList.size();
    }

    private void removeElement(int pos)
    {
        pictureList.remove(pos);
        notifyDataSetChanged();
    }

    public void addElement(String path)
    {
        pictureList.add(path);
        notifyDataSetChanged();
    }

    public String getPicture(int position)
    {
        return pictureList.get(position);
    }
}