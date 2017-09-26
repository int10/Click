package com.int10.click;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by int10 on 2017/9/15.
 */

public class ClickActivity extends AppCompatActivity {
	private ImageView m_iv, m_ivmask;
	MediaPlayer m_mediaplayer=null;
	protected Context m_context = this;
	boolean m_initrectmaped = false;
	String m_rootpath = null;
	String m_workpath = null;
	ButtonMapList m_buttonmaplist = null;
	int m_downx, m_downy;
	File[] m_files;
	int m_position;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置成全屏模式
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//强制为竖屏
		setContentView(R.layout.activity_click);
		Init();
	}

	private void Init() {
		Intent intent=getIntent();
		Bundle bundle=intent.getExtras();
		m_rootpath = bundle.getString("rootpath");
		File rootdir = new File(m_rootpath);
		m_files = rootdir.listFiles();
		m_position = bundle.getInt("position");
		m_workpath = m_files[m_position].getAbsolutePath();

		m_iv = (ImageView) findViewById(R.id.ivClickBackGround);
		m_iv.setOnTouchListener(new ClickActivity.TouchListenerImp());
		m_ivmask = (ImageView) findViewById(R.id.ivMask);
		m_ivmask.setVisibility(View.INVISIBLE);

		RefreshPage();
	}

	private void InitRectMapList(){
		if(m_initrectmaped) {return;}
		m_buttonmaplist = new ButtonMapList();
		if(m_buttonmaplist.Xml2List(m_workpath + "/DialogButtonConfig.xml")) {
			m_initrectmaped = true;
		}
	}

	private class TouchListenerImp implements ImageView.OnTouchListener {
		public boolean onTouch(View v, MotionEvent event) {
			int eventaction = event.getAction();
			int x, y;
			x = (int) event.getX();
			y = (int) event.getY();

			//get pic size
			BitmapDrawable bitmapDrawable = (BitmapDrawable) m_iv.getDrawable();
			int bw  = bitmapDrawable.getBitmap().getWidth();
			int bh = bitmapDrawable.getBitmap().getHeight();

			//获得ImageView中Image的真实宽高，
			int dw = m_iv.getDrawable().getBounds().width();
			int dh = m_iv.getDrawable().getBounds().height();

			//获得ImageView中Image的变换矩阵
			Matrix m = m_iv.getImageMatrix();
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
					m_downx = x;
					m_downy = y;
					break;
				case MotionEvent.ACTION_MOVE:
					break;
				case MotionEvent.ACTION_UP:
					if(Math.abs(x - m_downx) > 100 && Math.abs(y - m_downy) < 100) {
						if(x > m_downx) {
							//pre page;
							PrePage();
						} else {
							//next page;
							NextPage();
						}
						return false;
					} else {

						//event的xy转换成图片上的xy
						int imgx, imgy;
						imgx = x * bw / cw;
						imgy = y * bh / ch;
						//String target = m_rectmaplist.GetTarget(imgx, imgy);
						ButtonMapList.ButtonMap target = m_buttonmaplist.GetSoundPath(imgx, imgy);

						if (target != null) {
							//把图片里的x,y,w,h转换成屏幕上的xywh
							int scrx, scry, scrw, scrh;
							scrx = (int) (target.x * cw / bw);
							scry = (int) (target.y * ch / bh);
							scrw = (int) (target.width * cw / bw);
							scrh = (int) (target.height * ch / bh);
							Bitmap bitmap = Bitmap.createBitmap(((BitmapDrawable) m_iv.getDrawable()).getBitmap(), (int) target.x, (int) target.y, (int) target.width, (int) target.height);

							ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) m_ivmask.getLayoutParams();
							layoutParams.setMargins(scrx - 20, scry - 20, 0, 0);
							layoutParams.width = scrw + 40;
							layoutParams.height = scrh + 40;
							m_ivmask.setLayoutParams(layoutParams);
							m_ivmask.setImageBitmap(bitmap);
							m_ivmask.setVisibility(View.VISIBLE);
							PlayMedia(m_workpath + "/" + target.soundpath);
						}
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
			m_mediaplayer.setOnCompletionListener(new MediaPlayerCompletionListener());
			m_mediaplayer.prepare();
			m_mediaplayer.start();
			result = true;
		} catch (Exception e) {
		}
		return result;
	}

	private class MediaPlayerCompletionListener implements MediaPlayer.OnCompletionListener {
		@Override
		public void onCompletion(MediaPlayer mediaPlayer) {
			m_ivmask.setVisibility(View.INVISIBLE);
		}
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

	private void PrePage() {
		if(m_position == 0) {
			Toast.makeText(ClickActivity.this, "已经是第一页" , Toast.LENGTH_SHORT).show();
		} else {
			m_position--;
			RefreshPage();
		}
	}

	private void NextPage() {
		if(m_position + 1>= m_files.length) {
			Toast.makeText(ClickActivity.this, "已经是最后一页" , Toast.LENGTH_SHORT).show();
		} else {
			m_position++;
			RefreshPage();
		}
	}

	private void RefreshPage() {
		if(m_mediaplayer != null) {
			m_mediaplayer.stop();
			m_mediaplayer.release();
			m_mediaplayer = null;
		}
		m_ivmask.setVisibility(View.INVISIBLE);
		m_initrectmaped = false;
		m_workpath = m_files[m_position].getAbsolutePath();
		InitRectMapList();
		m_iv.setImageURI(Uri.parse(m_workpath + "/bg.jpg"));
	}

	public class ButtonMapList {
		private class ButtonMap {
			float x, y, width, height;
			int id;
			String soundpath = new String();
		}
		private FileInputStream m_fis=null;
		private DocumentBuilder m_builder;
		private Document m_doc;
		private Element m_rootNode = null;

		public ArrayList<ButtonMap> m_list = new ArrayList<>();
		public boolean Xml2List(String filename) {
			try {
				File newFile=new File(filename);
				m_fis=new FileInputStream(newFile);
				DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();
				m_builder = f.newDocumentBuilder();
				m_doc = m_builder.parse(m_fis);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return false;
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
				return false;
			} catch (SAXException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}

			m_rootNode = (Element) m_doc.getFirstChild();
			NodeList buttonlist = m_rootNode.getElementsByTagName("button");
			for(int i = 0; i < buttonlist.getLength(); i++) {
				Element el = (Element) buttonlist.item(i);
				ButtonMap bm = Element2ButtomMap(el);
				if( bm != null ) {
					m_list.add(bm);
				}
			}
//			for(ButtonMap bm : m_list) {
//				Log.e("int10", bm.x + " " + bm.y + " " + bm.width + " " + bm.height + " " + bm.id + " " + bm.soundpath + " ");
//				Log.e("int10", "000x" + String.valueOf((bm.x)));
//			}
			return true;
		}

		private ButtonMap Element2ButtomMap(Element rootnode)
		{
			ButtonMap bm = new ButtonMap();
			NodeList infolist = rootnode.getChildNodes();
			for(int i= 0;i<infolist.getLength();i++)
			{
				if ("x".equals(infolist.item(i).getNodeName())) {
					bm.x = Float.parseFloat(infolist.item(i).getFirstChild().getNodeValue());
				}
				if ("y".equals(infolist.item(i).getNodeName())) {
					bm.y = Float.parseFloat(infolist.item(i).getFirstChild().getNodeValue());
				}
				if ("width".equals(infolist.item(i).getNodeName())) {
					bm.width = Float.parseFloat(infolist.item(i).getFirstChild().getNodeValue());
				}
				if ("height".equals(infolist.item(i).getNodeName())) {
					bm.height = Float.parseFloat(infolist.item(i).getFirstChild().getNodeValue());
				}
				if ("soundpath".equals(infolist.item(i).getNodeName())) {
					String path = infolist.item(i).getFirstChild().getNodeValue();
					path = path.substring(path.lastIndexOf("/"));
					bm.soundpath = "sound" + path;
					Log.e("int10", "bm path:" + bm.soundpath);
				}
				bm.id = Integer.parseInt(rootnode.getAttribute("id"));
			}
			return bm;
		}

		ButtonMap GetSoundPath(float imgx, float imgy) {
			for(ButtonMap bm : m_list) {
				if(imgx > bm.x && imgx < (bm.x + bm.width) && imgy > bm.y && imgy < (bm.y + bm.height)) {
					return bm;
				}
			}
			return null;
		}
	}

}
