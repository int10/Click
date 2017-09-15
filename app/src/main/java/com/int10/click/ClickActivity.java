package com.int10.click;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

/**
 * Created by int10 on 2017/9/15.
 */

public class ClickActivity extends AppCompatActivity {
	private ImageView iv;
	MediaPlayer m_mediaplayer=null;
	protected Context m_context = this;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_click);
		init();
	}

	private void init() {
		iv = (ImageView) findViewById(R.id.ivClickBackGround);
		iv.setOnTouchListener(new ClickActivity.TouchListenerImp());
	}

	private class TouchListenerImp implements ImageView.OnTouchListener {
		public boolean onTouch(View v, MotionEvent event) {
			int eventaction = event.getAction();
			int x, y;
			x = (int) event.getX();
			y = (int) event.getY();

			//获取图片大小
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

			Rect rect = new Rect();
			rect.set(360*cw/bw, 285*ch/bh, (360+67)*cw/bw, (285+67)*ch/bh);

			//360 285 67 67
			switch (eventaction) {
				case MotionEvent.ACTION_DOWN:
					break;

				case MotionEvent.ACTION_MOVE:
					break;

				case MotionEvent.ACTION_UP:
					if(rect.contains(x, y))	{
						PlayMedia(Environment.getExternalStorageDirectory().getPath() + "/1.mp3");
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
			m_mediaplayer.setDataSource(path);
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
