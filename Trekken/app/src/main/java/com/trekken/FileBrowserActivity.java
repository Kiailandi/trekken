package com.trekken;

import java.io.File;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.text.DateFormat;

import android.os.Bundle;
import android.app.ListActivity;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

public class FileBrowserActivity extends ListActivity {

    private File currentDir;
    private FileArrayAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentDir = new File("/sdcard/");
        fill(currentDir);
    }

    private void fill(File f) {
        File[] dirs = f.listFiles();
        this.setTitle("Current Dir: " + f.getName());
        List<Item> directories = new ArrayList<Item>();
        List<Item> files = new ArrayList<Item>();

        try {
            for (File ff : dirs) {
                Date lastModDate = new Date(ff.lastModified());
                DateFormat formatter = DateFormat.getDateTimeInstance();
                String date_modify = formatter.format(lastModDate);

                if (ff.isDirectory() && !ff.isHidden()) {
                    File[] fbuf = ff.listFiles();
                    int buf = 0;

                    if (fbuf != null)
                        buf = fbuf.length;
                    else
                        buf = 0;

                    String num_item = String.valueOf(buf);

                    if (buf == 0)
                        num_item = num_item + " item";
                    else
                        num_item = num_item + " items";

                    //String formated = lastModDate.toString();
                    directories.add(new Item(ff.getName(), num_item, date_modify, ff.getAbsolutePath(), "directory_icon"));
                } else if (!ff.isHidden())
                    files.add(new Item(ff.getName(), ff.length() + " Byte", date_modify, ff.getAbsolutePath(), "file_icon"));
            }
        } catch (Exception e) {
            Log.e("ERROR", e.toString());
        }

        Collections.sort(directories);
        Collections.sort(files);
        directories.addAll(files);

        if (!f.getName().equalsIgnoreCase("sdcard"))
            directories.add(0, new Item("..", "Parent Directory", "", f.getParent(), "directory_up"));

        adapter = new FileArrayAdapter(FileBrowserActivity.this, R.layout.file_browser, directories);

        this.setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Item o = adapter.getItem(position);
        if (o.getImage().equalsIgnoreCase("directory_icon") || o.getImage().equalsIgnoreCase("directory_up")) {
            currentDir = new File(o.getPath());
            fill(currentDir);
        } else {
            onFileClick(o);
        }
    }

    private void onFileClick(Item object) {
        Intent intent = new Intent();
        intent.putExtra("GetPath", currentDir.toString());
        intent.putExtra("GetFileName", object.getName());
        setResult(RESULT_OK, intent);
        finish();
    }

}

