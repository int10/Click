package com.int10.click;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends AppCompatActivity {
	protected int m_requestcode = 100;
	protected Context m_context = this;
	private final String ROOTDIR = Environment.getExternalStorageDirectory().getPath() + "/int10click";
	private File[] m_curfiles;
	private File m_rootdir, m_curdir;
	private FileListAdapter m_flada;
	private ListView m_lvfilelist;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Init();
	}

	private void Init() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			int permission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
			if (permission != PackageManager.PERMISSION_GRANTED) {
				requestPermissions(new String[]{
						Manifest.permission.WRITE_EXTERNAL_STORAGE,
						Manifest.permission.READ_EXTERNAL_STORAGE}, m_requestcode);
			} else {
				InitDirList();
			}
		}
		m_flada = new FileListAdapter();
		m_lvfilelist = (ListView) findViewById(R.id.lvFileList);
		m_lvfilelist.setAdapter(m_flada);
		m_lvfilelist.setOnItemClickListener(new fileItemListener());
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == m_requestcode) {
			if (permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)
					&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				InitDirList();
			}
		}
	}

	private void InitDirList() {
		m_rootdir = new File(ROOTDIR);
		m_curdir = m_rootdir;
		m_curfiles = m_rootdir.listFiles();
		m_flada = new FileListAdapter();
	}

	public class FileListAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			return m_curfiles.length;
		}

		@Override
		public Object getItem(int position) {
			return m_curfiles[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater layout_inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View layout = layout_inflater.inflate(R.layout.item_filelist, null);
			TextView tvname = (TextView) layout.findViewById(R.id.tvFileListItem);
			tvname.setText(m_curfiles[position].getName());
			return layout;
		}
	}

	public class fileItemListener implements AdapterView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			if (m_curfiles[position].isDirectory()) {
				File curdir;
				curdir = m_curfiles[position];
				File[] curfiles = curdir.listFiles();
				boolean findconfig = false, findback = false;
				//search config.txt and back.jpg , if all exist means it can play..if not ,enter this dir.
				for (File f : curfiles) {
					if (f.isFile() && f.getName().equals("config.txt")) {
						findconfig = true;
					}
					if (f.isFile() && f.getName().equals("back.jpg")) {
						findback = true;
					}
					if (findback && findconfig) {
						break;
					}
				}
				if (findback && findconfig) {
					Intent intent = new Intent();
					intent.setClass(MainActivity.this, ClickActivity.class);
					intent.putExtra("workpath", curdir.getAbsolutePath());
					startActivity(intent);
				} else {
					m_curdir = m_curfiles[position];
					m_curfiles = m_curdir.listFiles();
					m_lvfilelist.setAdapter(m_flada);
				}
			}
		}
	}

	@Override
	public void onBackPressed()
	{
		if(m_curdir.equals(m_rootdir)) {
			this.finish();
		} else {
			m_curdir = m_curdir.getParentFile();
			m_curfiles = m_curdir.listFiles();
			m_lvfilelist.setAdapter(m_flada);
		}
	}
}
