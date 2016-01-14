package com.deepwits.Patron.StorageManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.util.Log;

import com.deepwits.Patron.DefaultConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Created by Payne on 1/13/16.
 */
public class StorageResolve {

    private MediaFile mediaFile;
    public StorageResolve(MediaFile mediaFile){
        this.mediaFile = mediaFile;
        if(null != mediaFile.getPath()){
            resolve();  //解析文件
        }
    }

    /**
     * 解析文件,获取MediaFile类所需的相关信息
     * @return
     */
    public int resolve() {
        String path = mediaFile.getPath();
        File file = new File(path);
        if (!file.exists()) return -1;  // file is not exists
        if(mediaFile.getMediaType() == MediaFile.MediaType.VIDEO){
            long tmp1[] = getPlayTime(path);
            mediaFile.setWidth((int) tmp1[0]);
            mediaFile.setHeight((int) tmp1[1]);
            mediaFile.setDuration(tmp1[2]);

            //创建视频缩略图
            Bitmap bitmap = getVideoThumbnail(path, 512, 512, MediaStore.Video.Thumbnails.MINI_KIND);
            DefaultConfig config = new DefaultConfig();
            File tmp = new File(config.APP_DIR);
            if(!tmp.exists()) tmp.mkdirs();
            FileOutputStream outStream = null;
            try {
                outStream = new FileOutputStream(tmp);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outStream);

        }
        if(mediaFile.getMediaType() == MediaFile.MediaType.PICTURE){
            int tmp2[] = getPicSize(path);
            mediaFile.setWidth(tmp2[0]);
            mediaFile.setHeight(tmp2[1]);

            //创建图片缩略图
            Bitmap bitmap = getImageThumbnail(path,512,512);
            DefaultConfig config = new DefaultConfig();
            File tmp = new File(config.APP_DIR);
            if(!tmp.exists()) tmp.mkdirs();
            FileOutputStream outStream = null;
            try {
                outStream = new FileOutputStream(tmp);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outStream);
        }
        //size
        mediaFile.setSize(file.length());
        mediaFile.setDate(file.lastModified());
        return 1;
    }


    //解析MP4文件时长、宽高
    public static long[] getPlayTime(String mUri) {
        long duration, width, height;
        long[] array = new long[3];
        int index = mUri.lastIndexOf(".");
        String suffix = mUri.substring(index + 1);
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        if (mUri != null && suffix.equalsIgnoreCase("mp4")) {
            try {
                    /*HashMap<String, String> headers = null;//获取网络视频的信息
                    if (headers == null)
                    {
                        headers = new HashMap<String, String>();
                        headers.put("User-Agent", "Mozilla/5.0 (Linux; U; Android 4.4.2; zh-CN; MW-KW-001 Build/JRO03C) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 UCBrowser/1.0.0.001 U4/0.8.0 Mobile Safari/533.1");
                    }
                    mmr.setDataSource(mUri, headers);*/
                mmr.setDataSource(mUri);
                duration = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));//时长(毫秒)
                width = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));//宽
                height = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));//高
                if (duration > 0 && width > 0 && height > 0) {
                    array[0] = width;
                    array[1] = height;
                    array[2] = duration;
                } else {
                    File file = new File(mUri);
                    file.delete();
                }
            } catch (Exception ex) {
                File file = new File(mUri);
                if (file.exists()) file.delete();
                Log.e("StorageUtil", "MediaMetadataRetriever exception " + ex + " deleted " + mUri);
            } finally {
                mmr.release();
            }
        }
        return array;
    }
    public static int[] getPicSize(String url) {
        BitmapFactory.Options options = new BitmapFactory.Options();

        /**
         * 最关键在此，把options.inJustDecodeBounds = true;
         * 这里再decodeFile()，返回的bitmap为空，但此时调用options.outHeight时，已经包含了图片的高了
         */
        options.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(url, options); // 此时返回的bitmap为null
        /**
         *options.outHeight为原始图片的高
         */
        int[] array = new int[2];
        array[0] = options.outWidth;
        array[1] = options.outHeight;
        return array;
    }

    /**
     * 根据指定的图像路径和大小来获取缩略图
     * 此方法有两点好处：
     *     1. 使用较小的内存空间，第一次获取的bitmap实际上为null，只是为了读取宽度和高度，
     *        第二次读取的bitmap是根据比例压缩过的图像，第三次读取的bitmap是所要的缩略图。
     *     2. 缩略图对于原图像来讲没有拉伸，这里使用了2.2版本的新工具ThumbnailUtils，使
     *        用这个工具生成的图像不会被拉伸。
     * @param imagePath 图像的路径
     * @param width 指定输出图像的宽度
     * @param height 指定输出图像的高度
     * @return 生成的缩略图
     */
    private Bitmap getImageThumbnail(String imagePath, int width, int height) {
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        // 获取这个图片的宽和高，注意此处的bitmap为null
        bitmap = BitmapFactory.decodeFile(imagePath, options);
        options.inJustDecodeBounds = false; // 设为 false
        // 计算缩放比
        int h = options.outHeight;
        int w = options.outWidth;
        int beWidth = w / width;
        int beHeight = h / height;
        int be = 1;
        if (beWidth < beHeight) {
            be = beWidth;
        } else {
            be = beHeight;
        }
        if (be <= 0) {
            be = 1;
        }
        options.inSampleSize = be;
        // 重新读入图片，读取缩放后的bitmap，注意这次要把options.inJustDecodeBounds 设为 false
        bitmap = BitmapFactory.decodeFile(imagePath, options);
        // 利用ThumbnailUtils来创建缩略图，这里要指定要缩放哪个Bitmap对象
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return bitmap;
    }

    /**
     * 获取视频的缩略图
     * 先通过ThumbnailUtils来创建一个视频的缩略图，然后再利用ThumbnailUtils来生成指定大小的缩略图。
     * 如果想要的缩略图的宽和高都小于MICRO_KIND，则类型要使用MICRO_KIND作为kind的值，这样会节省内存。
     * @param videoPath 视频的路径
     * @param width 指定输出视频缩略图的宽度
     * @param height 指定输出视频缩略图的高度度
     * @param kind 参照MediaStore.Images.Thumbnails类中的常量MINI_KIND和MICRO_KIND。
     *            其中，MINI_KIND: 512 x 384，MICRO_KIND: 96 x 96
     * @return 指定大小的视频缩略图
     */
    private Bitmap getVideoThumbnail(String videoPath, int width, int height,
                                     int kind) {
        Bitmap bitmap = null;
        // 获取视频的缩略图
        bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, kind);
        System.out.println("w"+bitmap.getWidth());
        System.out.println("h"+bitmap.getHeight());
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return bitmap;
    }

}
