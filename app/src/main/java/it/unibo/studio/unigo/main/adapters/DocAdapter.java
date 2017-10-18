package it.unibo.studio.unigo.main.adapters;

import android.app.NotificationManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
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
        return new ImageHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_detail_file, parent, false));
    }

    @Override
    public int getItemCount()
    {
        return docList.size();
    }

    @Override
    public void onBindViewHolder(final ImageHolder holder, int position)
    {
        final String fileName = docList.get(position).substring(docList.get(position).lastIndexOf("%2F") + 3, docList.get(position).lastIndexOf("?"));

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

        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                new AsyncDownload(docList.get(holder.getAdapterPosition()), fileName, holder.context).execute();
                // Prova con un file di 14MB
                //new AsyncDownload("https://firebasestorage.googleapis.com/v0/b/unigo-569da.appspot.com/o/Avenged%20Sevenfold%20-%20This%20Means%20War.mp3?alt=media&token=c87a7433-224b-490a-b0ae-64627246d2b1", fileName, holder.context).execute();
            }
        });
    }

    private class AsyncDownload extends AsyncTask<Void, Void, Void>
    {
        private NotificationManager mNotifyManager;
        private NotificationCompat.Builder mBuilder;
        private String downloadUrl, fileName;
        private Context context;

        AsyncDownload(String downloadUrl, String fileName, Context context)
        {
            this.downloadUrl = downloadUrl;
            this.fileName = fileName;
            this.context = context;
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();

            mNotifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mBuilder = new NotificationCompat.Builder(context);
            mBuilder.setContentTitle(fileName)
                    .setContentText("Download in corso")
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), android.R.drawable.stat_sys_download))
                    .setColor(Color.RED)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true);
            mNotifyManager.notify(0, mBuilder.build());
        }

        protected Void doInBackground(Void... params)
        {
            downloadFromUrl(downloadUrl, fileName);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid)
        {
            super.onPostExecute(aVoid);
            mBuilder.setOngoing(false)
                    .setContentText("Download completato")
                    .setSmallIcon(R.drawable.ic_done_black_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_done_black_24dp));
            mNotifyManager.notify(0, mBuilder.build());
        }
    }

    private void downloadFromUrl(String DownloadUrl, String fileName)
    {
        try
        {
            File root = android.os.Environment.getExternalStorageDirectory();

            File dir = new File (root.getAbsolutePath() + "/Downloads");
            if(!dir.exists())
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();

            URL url = new URL(DownloadUrl);
            File file = new File(dir, fileName);

            URLConnection ucon = url.openConnection();
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            byte[] data = new byte[50];
            int current;
            while((current = bis.read(data,0,data.length)) != -1)
                buffer.write(data,0,current);

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(buffer.toByteArray());
            fos.flush();
            fos.close();
        }
        catch (IOException e)
        {
            Log.d("DownloadManager", "Error: " + e);
        }
    }
}