package it.unibo.studio.unigo.main.adapters;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import it.unibo.studio.unigo.R;

class DocAdapter extends RecyclerView.Adapter<DocAdapter.ImageHolder>
{
    private List<String> docList;

    class ImageHolder extends RecyclerView.ViewHolder
    {
        Context context;
        LinearLayout layout;
        TextView text;
        ImageView imgExtension, imgDownload;

        ImageHolder(View v)
        {
            super(v);
            context = v.getContext();
            layout = (LinearLayout) v.findViewById(R.id.fileItemLayout);
            imgExtension = (ImageView) v.findViewById(R.id.fileItemExtensionIcon);
            text = (TextView) v.findViewById(R.id.fileItemTxt);
            imgDownload = (ImageView) v.findViewById(R.id.fileItemDownloadIcon);
        }
    }

    DocAdapter(List<String> docList)
    {
        this.docList = docList;
    }

    @Override
    public ImageHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        return new ImageHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_detail, parent, false));
    }

    @Override
    public int getItemCount()
    {
        return docList.size();
    }

    @Override
    public void onBindViewHolder(final ImageHolder holder, int position)
    {
        String fileName = docList.get(position).substring(docList.get(position).lastIndexOf("%2F") + 3, docList.get(position).lastIndexOf("?"));

        // Viene impostata un'icona per indicare la tipologia del file (in base alla sua estensione)
        if (fileName.contains(".xls") || fileName.contains(".xlsx"))
            holder.imgExtension.setBackground(ContextCompat.getDrawable(holder.context, R.drawable.ic_file_excel_box));
        else if (fileName.contains(".mp3") || fileName.contains(".wma") || fileName.contains(".midi"))
            holder.imgExtension.setBackground(ContextCompat.getDrawable(holder.context, R.drawable.ic_file_music));
        else if (fileName.contains(".pdf"))
            holder.imgExtension.setBackground(ContextCompat.getDrawable(holder.context, R.drawable.ic_file_pdf_box));
        else if (fileName.contains(".ppt") || fileName.contains(".pptx"))
            holder.imgExtension.setBackground(ContextCompat.getDrawable(holder.context, R.drawable.ic_file_powerpoint_box));
        else if (fileName.contains(".doc") || fileName.contains(".docx") || fileName.contains(".txt"))
            holder.imgExtension.setBackground(ContextCompat.getDrawable(holder.context, R.drawable.ic_file_word_box));
        else if (fileName.contains(".iso") || fileName.contains(".zip") || fileName.contains(".rar") || fileName.contains(".7z") || fileName.contains(".tar.gz"))
            holder.imgExtension.setBackground(ContextCompat.getDrawable(holder.context, R.drawable.ic_file_zip));
        else if (fileName.contains(".jpg") || fileName.contains(".jpeg") || fileName.contains(".png") || fileName.contains(".bmp") || fileName.contains(".gif"))
            holder.imgExtension.setBackground(ContextCompat.getDrawable(holder.context, R.drawable.ic_file_image));
        else
            holder.imgExtension.setBackground(ContextCompat.getDrawable(holder.context, R.drawable.ic_file));

        // Nome Chip
        holder.text.setText(fileName);

        holder.imgDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                //holder.context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(docList.get(holder.getAdapterPosition()))));
            }
        });
    }
}