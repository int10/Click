package com.int10.click;

import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by int10 on 2017/9/15.
 */

public class ClickActivity extends AppCompatActivity {
	private ImageView iv;
	MediaPlayer m_mediaplayer=null;
	protected Context m_context = this;
	boolean m_initrectmaped = false;
	String m_workpath = null;
	RectMapList m_rectmaplist;


	private class RectMap {
		public Rect rect = new Rect();
		public String target = null;
	}

	private class RectMapList{
		private ArrayList<RectMap> rectmaps = new ArrayList<RectMap>();
		public void AddItem(RectMap item) {
			rectmaps.add(item);
		}
		public String GetTarget(int x, int y) {
			for(RectMap item : rectmaps){
				if(item.rect.contains(x, y)) {
					return item.target;
				}
			}
			return null;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_click);
		Init();
	}

	private void Init() {
		Intent intent=getIntent();
		Bundle bundle=intent.getExtras();
		m_workpath = bundle.getString("workpath");
		InitRectMapList();
		iv = (ImageView) findViewById(R.id.ivClickBackGround);
		iv.setOnTouchListener(new ClickActivity.TouchListenerImp());
		iv.setImageURI(Uri.parse(m_workpath + "/back.jpg"));
	}

	private void InitRectMapList(){
		if(m_initrectmaped) {return;}
		m_rectmaplist = new RectMapList();
		FileInputStream inputStream = null;
		try {
			inputStream = new FileInputStream(m_workpath + "/config.txt");
			byte [] buf = new byte[inputStream.available()];
			inputStream.read(buf);
			inputStream.close();
			String bufstr = new String(buf);
			String[] itemstrlist = bufstr.split("\n");
			for(String s : itemstrlist){
				String[] infostrlsit = s.split(",");
				RectMap rectmap = new RectMap();
				rectmap.rect.set(Integer.parseInt(infostrlsit[0]), Integer.parseInt(infostrlsit[1]), Integer.parseInt(infostrlsit[2]), Integer.parseInt(infostrlsit[3]));
				rectmap.target = infostrlsit[4];
				m_rectmaplist.AddItem(rectmap);
			}
			m_initrectmaped = true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private class TouchListenerImp implements ImageView.OnTouchListener {
		public boolean onTouch(View v, MotionEvent event) {
			int eventaction = event.getAction();
			int x, y;
			x = (int) event.getX();
			y = (int) event.getY();

			//get pic size
			BitmapDrawable bitmapDrawable = (BitmapDrawable) iv.getDrawable();
			int bw  = bitmapDrawable.getBitmap().getWidth();
			int bh = bitmapDrawable.getBitmap().getHeight();

			//获得ImageView中Image的真实宽高，
			int dw = iv.getDrawable().getBounds().width();
			int dh = iv.getDrawable().getBounds().height();

			//获得ImageView中Image的变换矩阵
			Matrix m = iv.getImageMatrix();
			float[] values = new float[10];
			m.getValues(values);

			//Image在绘制过程中的变换矩阵，从中获得x和y方向的缩放系数
			float sx = values[0];
			float sy = values[4];

			//计算Image在屏幕上实际绘制的宽高
			int cw = (int)(dw * sx);
			int ch = (int)(dh * sy);

			switch (eventaction) {
				case MotionEvent.ACTION_DOWN:
					break;
				case MotionEvent.ACTION_MOVE:
					break;
				case MotionEvent.ACTION_UP:
					//event的xy转换成图片上的xy
					int imgx, imgy;
					imgx = x*bw/cw;
					imgy = y*bh/ch;
					String target = m_rectmaplist.GetTarget(imgx, imgy);

					if(target != null) {
						PlayMedia(m_workpath + "/" + target);
					}
					break;
			}
			return true;
		}
	}

	private boolean PlayMedia(String path){
		boolean result = false;
		if(m_mediaplayer != null) {
			m_mediaplayer.stop();
			m_mediaplayer.release();
			m_mediaplayer = null;
		}
		try {
			m_mediaplayer = new MediaPlayer();
			m_mediaplayer.setDataSource(ClickActivity.this, Uri.parse(path));
			m_mediaplayer.prepare();
			m_mediaplayer.start();
			result = true;
		} catch (Exception e) {
		}
		return result;
	}

	@Override
	public void onDestroy(){
		if(m_mediaplayer != null) {
			m_mediaplayer.stop();
			m_mediaplayer.release();
			m_mediaplayer = null;
		}
		super.onDestroy();
	}

}
