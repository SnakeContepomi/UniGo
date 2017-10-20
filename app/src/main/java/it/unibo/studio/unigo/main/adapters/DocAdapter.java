package it.unibo.studio.unigo.main.adapters;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
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
import it.unibo.studio.unigo.main.DetailActivity;

public class DocAdapter extends RecyclerView.Adapter<DocAdapter.ImageHolder>
{
    private List<String> docList;
    // Indice dell'elemento che ha richiesto i permessi di scrittura sulla memoria interna
    // (utilizzato per riprendere il download dell'elemento una volta ottenuti i permessi di scrittura
    //  da DetailActivity)
    private int elementRequestPermission;
    private Context context;

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

    DocAdapter(List<String> docList, Context context)
    {
        this.docList = docList;
        this.context = context;
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
                if (context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                    new AsyncDownload(docList.get(holder.getAdapterPosition()), fileName).execute();
                else
                {
                    elementRequestPermission = holder.getAdapterPosition();
                    ((DetailActivity)context).getWritePermission();
                }
            }
        });
    }

    // Metodo che permette di scaricare l'elemento selezionato, una volta ottenuti i permessi di scrittura sulla memoria interna
    // (viene utilizzato solamente la prima volta che vengono richiesti i permessi)
    public void downloadFileAfterPermissions()
    {
        String fileName = docList.get(elementRequestPermission).substring(docList.get(elementRequestPermission).lastIndexOf("%2F") + 3, docList.get(elementRequestPermission).lastIndexOf("?"));
        new AsyncDownload(docList.get(elementRequestPermission), fileName).execute();
        elementRequestPermission = -1;
    }

    private class AsyncDownload extends AsyncTask<Void, Void, String>
    {
        private NotificationManager mNotifyManager;
        private NotificationCompat.Builder mBuilder;
        private String downloadUrl, fileName;

        AsyncDownload(String downloadUrl, String fileName)
        {
            this.downloadUrl = downloadUrl;
            this.fileName = fileName;
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

        protected String doInBackground(Void... params)
        {
            return downloadFromUrl(downloadUrl, fileName);
        }

        @Override
        protected void onPostExecute(String params)
        {
            super.onPostExecute(params);
            mBuilder.setAutoCancel(true)
                    .setOngoing(false)
                    .setContentText("Download completato")
                    .setSmallIcon(R.drawable.ic_done_black_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_done_black_24dp));

            // Al click della notifica viene aperta una finestra per scegliere con quale applicazione visionare il contenuto della cartella download
            // (si consiglia di avere installato un file manager)
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            Uri uri = Uri.parse(Environment.getExternalStorageDirectory().getPath()+ "/" + Environment.DIRECTORY_DOWNLOADS);
            intent.setDataAndType(uri, "text/csv");
            PendingIntent pIntent = PendingIntent.getActivity(context, 0, Intent.createChooser(intent, "Open folder"), PendingIntent.FLAG_CANCEL_CURRENT);
            mBuilder.setContentIntent(pIntent);
            mNotifyManager.notify(0, mBuilder.build());
        }
    }

    private String downloadFromUrl(String DownloadUrl, String fileName)
    {
        File dir = new File (Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Environment.DIRECTORY_DOWNLOADS);
        if(!dir.exists())
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        File file = new File(dir, fileName);
        try
        {
            URL url = new URL(DownloadUrl);

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
        catch (IOException e) { e.printStackTrace(); }

        return file.getAbsolutePath();
    }
}