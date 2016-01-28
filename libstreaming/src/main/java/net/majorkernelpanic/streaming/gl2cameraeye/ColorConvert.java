package net.majorkernelpanic.streaming.gl2cameraeye;

import android.util.Log;

public class ColorConvert {

	private static final String TAG ="MediaMuxerWrapper";
	/**
	 * RGBA 转成 ARGB
	 * @param buf
	 * @return
	 */
	public static byte [] rgbaToargb(byte [] buf, int dataLen){
		if(dataLen%4!=0){
			Log.e(TAG, "输入数据不是4的倍数");
			return buf;
		}
		//a
		byte [] bmp = new byte[dataLen/4*3];//
		for(int idx=0, j=0; idx<dataLen; idx+=4){
			bmp[j++]=buf[idx+0];
			bmp[j++]=buf[idx+1];
			bmp[j++]=buf[idx+2];
		}
		return bmp;
	}
	public static void writeBmp(byte [] data, String a){
		String f = "/sdcard/rec/aaa_"+System.currentTimeMillis()+"_"+a+".bmp";
		byte [] bs = new byte[]{
				(byte)0x42, (byte)0x4d, (byte)0x38, (byte)0xec, (byte)0x5e, (byte)0x00, (byte)0x00,(byte)0x00, (byte)0x00,(byte)0x00, (byte)0x36,(byte)0x00, (byte)0x00,(byte)0x00, (byte)0x28,(byte)0x00, 
				(byte)0x00, (byte)0x00, (byte)0x80, (byte)0x07, (byte)0x00, (byte)0x00, (byte)0x38,(byte)0x04, (byte)0x00,(byte)0x00, (byte)0x01,(byte)0x00, (byte)0x18,(byte)0x00, (byte)0x00,(byte)0x00,
				(byte)0x00, (byte)0x00, (byte)0x02, (byte)0xec, (byte)0x5e, (byte)0x00, (byte)0x12,(byte)0x0b, (byte)0x00,(byte)0x00, (byte)0x12,(byte)0x0b, (byte)0x00,(byte)0x00, (byte)0x00,(byte)0x00,
				(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
		};
		try{
		java.io.OutputStream os = new java.io.FileOutputStream(new java.io.File(f));
		os.write(bs);
		os.write(data);
		os.flush();
		os.close();
		}catch(Throwable e){
			Log.e(TAG, e.getMessage(), e);
		}
	}
}
