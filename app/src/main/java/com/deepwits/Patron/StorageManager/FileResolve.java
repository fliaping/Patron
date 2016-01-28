package com.deepwits.Patron.StorageManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.util.Log;

import com.deepwits.Patron.Config;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by Payne on 1/13/16.
 */
public class FileResolve {

    private MediaFile mediaFile;
    public FileResolve(){
        this.mediaFile = mediaFile;
    }

    /**
     * 解析文件,获取MediaFile类所需的相关信息
     * @return
     */
    public int resolve(MediaFile mediaFile) {
        String path = mediaFile.getPath();
        if(null == path ){ return -1;}   //path is null
        File file = new File(path);
        if (!(file.exists())) return -2;  // file is not exists
        Boolean isCreateThumb = false;    //
        String thumbnailFileName = ".thumb_"+mediaFile.getFilename()+".jpg";  //缩略图文件名
        File thumbnail = new File(Config.THUMBNAIL_PATH+"/"+thumbnailFileName);
        if(!thumbnail.exists()){
            isCreateThumb = true;   //判断缩略图是否存在,存在就不再生成
        }else {
            mediaFile.setThumbPath(thumbnail.getPath());
            Log.e("StorageManager","缩略图已存在:"+thumbnail.getPath());
        }

        if(mediaFile.getMediaType() == MediaFile.MediaType.VIDEO.get()){
            long tmp1[] = getPlayTime(path);
            mediaFile.setWidth((int) tmp1[0]);
            mediaFile.setHeight((int) tmp1[1]);
            mediaFile.setDuration(tmp1[2]);

            Log.e("StorageManager","解析视频文件 :"+path+" width:"+tmp1[0]+" height:"+tmp1[1]+" du:"+tmp1[2]);

            //create video file thumbnail
            if(isCreateThumb){
                Bitmap bitmap = getVideoThumbnail(path, 512, 512, MediaStore.Video.Thumbnails.MINI_KIND);
                File dir = new File(Config.THUMBNAIL_PATH);
                if(!dir.exists()) dir.mkdirs();
                File osFile = new File(dir,thumbnailFileName);
                FileOutputStream outStream = null;
                try {
                    outStream = new FileOutputStream(osFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                if(bitmap!=null){
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outStream);
                    mediaFile.setThumbPath(osFile.getPath());
                    Log.e("StorageManager", "缩略图生成成功 " + osFile.getPath());
                }else {
                    Log.e("StorageManager","bitmap is null,thumbnail create failed");
                }
            }

        }
        if(mediaFile.getMediaType() == MediaFile.MediaType.PICTURE.get()){
            int tmp2[] = getPicSize(path);
            mediaFile.setWidth(tmp2[0]);
            mediaFile.setHeight(tmp2[1]);

            Log.e("StorageManager", "解析图片文件 :" + path + " width:" + tmp2[0] + " height:" + tmp2[1]);
            //create image file thumbnail
            if(isCreateThumb){
                Bitmap bitmap = getImageThumbnail(path, 512, 512);
                File dir = new File(Config.THUMBNAIL_PATH);
                if(!dir.exists()) dir.mkdirs();
                File osFile = new File(dir,thumbnailFileName);
                FileOutputStream outStream = null;
                try {
                    outStream = new FileOutputStream(osFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                if(bitmap!=null){
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outStream);
                    mediaFile.setThumbPath(osFile.getPath());
                    Log.e("StorageManager", "缩略图生成成功 " + osFile.getPath());
                }

            }
        }
        mediaFile.setThumbPath(thumbnail.getPath());
        //size
        mediaFile.setSize(file.length());
        mediaFile.setDate(file.lastModified());

        mediaFile.setEventType(resolveEventType(path));
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

        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return bitmap;
    }

    private int resolveEventType(String path){
        String[] pathString  = StorageUtil.splitString(path);
        int lastIndex = pathString.length-1;
        //设置文件类型
        if(pathString[lastIndex-2].equalsIgnoreCase(Config.NORMAL_VIDEO_DIR)){  //正常视频
            return MediaFile.EventType.NORMAL.get();
        }else if(pathString[lastIndex-2].equalsIgnoreCase(Config.LOCK_VIDEO_DIR)){  //锁定视频   (目录问题，待修改lastIndex-2)
            return MediaFile.EventType.LOCKED.get();
        }else if(pathString[lastIndex-1].equalsIgnoreCase(Config.UPLOAD_VIDEO_DIR)){  //上传视频
            return MediaFile.EventType.UPLOAD.get();
        }else if(pathString[lastIndex-1].equalsIgnoreCase(Config.TAKE_PICTURE_DIR)){  //手动照片
            return MediaFile.EventType.NORMAL.get();
        }else if(pathString[lastIndex-1].equalsIgnoreCase(Config.UPLOAD_PICTURE_DIR)){  //上传照片
            return MediaFile.EventType.UPLOAD.get();
        }else {
            Log.e("FileResolve","文件目录："+pathString[lastIndex-1]+"不匹配任何一个预定义目录,请检查存储路径");
            return 0;
        }

    }

}
