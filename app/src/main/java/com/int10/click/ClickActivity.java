package com.int10.click;

import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

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
	private ImageView iv;
	MediaPlayer m_mediaplayer=null;
	protected Context m_context = this;
	boolean m_initrectmaped = false;
	String m_workpath = null;
	ButtonMapList m_buttonmaplist = null;

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
		iv.setImageURI(Uri.parse(m_workpath + "/bg.jpg"));
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
					//String target = m_rectmaplist.GetTarget(imgx, imgy);
					String target = m_buttonmaplist.GetSoundPath(imgx, imgy);

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
			for(ButtonMap bm : m_list) {
				Log.e("int10", bm.x + " " + bm.y + " " + bm.width + " " + bm.height + " " + bm.id + " " + bm.soundpath + " ");
				Log.e("int10", "000x" + String.valueOf((bm.x)));
			}
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

		String GetSoundPath(float imgx, float imgy) {
			for(ButtonMap bm : m_list) {
				if(imgx > bm.x && imgx < (bm.x + bm.width) && imgy > bm.y && imgy < (bm.y + bm.height)) {
					return bm.soundpath;
				}
			}
			return null;
		}
	}

}
