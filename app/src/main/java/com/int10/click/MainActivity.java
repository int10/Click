package com.int10.click;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置成全屏模式
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//强制为竖屏
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
		if (!isFileExists(getApplicationInfo().dataDir + "/inited")) {
			if (copyAssetsToFilesystem("todo.zip", getApplicationInfo().dataDir + "/todo.zip")) {
				if (!unZip(getApplicationInfo().dataDir + "/todo.zip", ROOTDIR)) {
					Toast.makeText(this, "解压数据出错，请重装！", Toast.LENGTH_SHORT).show();
					return;
				}
				tryCreateFile(new File(getApplicationInfo().dataDir + "/inited"));
			} else {
				Toast.makeText(this, "拷贝文件出错，请重装！", Toast.LENGTH_SHORT).show();
				return;
			}
		}
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
					if (f.isFile() && f.getName().equals("DialogButtonConfig.xml")) {
						findconfig = true;
					}
					if (f.isFile() && f.getName().equals("bg.jpg")) {
						findback = true;
					}
					if (findback && findconfig) {
						break;
					}
				}
				if (findback && findconfig) {
					Intent intent = new Intent();
					intent.setClass(MainActivity.this, ClickActivity.class);
					intent.putExtra("rootpath", curdir.getParent());
					intent.putExtra("position", position);
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

	private final int BUFFER_LENGTH = 1024;

	public byte[] getBufByte() {
		return new byte[BUFFER_LENGTH];
	}
	private boolean unZip(String zipFile, String targetPath) {
		String strEntry; //保存每个zip的条目名称

		try {
			BufferedOutputStream dest = null; //缓冲输出流
			FileInputStream fis = new FileInputStream(zipFile);
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
			ZipEntry entry; //每个zip条目的实例

			while ((entry = zis.getNextEntry()) != null) {
				int count;
				byte data[] = getBufByte();
				strEntry = entry.getName();

				File entryFile = new File(targetPath + File.separator + strEntry);
				createDir(entryFile.getParent());

				FileOutputStream fos = new FileOutputStream(entryFile);
				dest = new BufferedOutputStream(fos, BUFFER_LENGTH);
				while ((count = zis.read(data, 0, BUFFER_LENGTH)) != -1) {
					dest.write(data, 0, count);
				}
				dest.flush();
				dest.close();
			}
			zis.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public static boolean createDir(String path) {
		return createDir(new File(path));
	}

	public static boolean createDir(File file) {
		if(!file.exists()) {
			if(!createDir(file.getParent())){
				return false;
			}
			if(!file.mkdir()){
				return false;
			}
		} else {
			if(file.isFile()) {
				file.delete();
				if(!file.mkdir()){
					return false;
				}
			}
		}
		return true;
	}
	private boolean copyAssetsToFilesystem(String assetsSrc, String des){
		InputStream istream = null;
		OutputStream ostream = null;
		try{
			Context context  = getApplication();
			AssetManager am = context.getAssets();
			istream = am.open(assetsSrc);
			ostream = new FileOutputStream(des);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = istream.read(buffer))>0){
				ostream.write(buffer, 0, length);
			}
			istream.close();
			ostream.close();
		}
		catch(Exception e){
			e.printStackTrace();
			try{
				if(istream!=null)
					istream.close();
				if(ostream!=null)
					ostream.close();
			}
			catch(Exception ee){
				ee.printStackTrace();
			}
			return false;
		}
		return true;
	}

	public static boolean isFileExists(String filepath) {
		return isFileExists(new File(filepath));
	}

	public static boolean isFileExists(File file) {
		boolean exists = true;
		if ((!file.exists()) || (file.exists() && !file.isFile())) {
			exists = false;
		}
		return exists;
	}

	public static void tryCreateFile(File file) {
		if (!isFileExists(file)) {
			file.delete();
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
