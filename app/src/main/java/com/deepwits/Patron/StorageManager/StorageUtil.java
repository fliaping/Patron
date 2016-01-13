package com.deepwits.Patron.StorageManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xp on 15-9-23.
 * 存储工具包
 */
public class StorageUtil {
    private static final String TAG = "StorageUtil";
    public static String getStorageDir() {
        String path = "";
        List<String> pathList= getExtSDCardPaths();
        if(pathList.size()>1){//paths.get(0)肯定是外置SD卡的位置，因为它是primary external storage.
            Log.e(TAG, "*****************pathList.get(0) is " + pathList.get(0));
            Log.e(TAG, "^_^ ^_^ ^_^ ^_^ ^_^ ^_^ ^_^ ^_^ ^_^pathList.get(1) is " + pathList.get(1));
            path = pathList.get(1); //    /storage/sdcard1
        } else if(pathList.size() == 1) {
            //path = pathList.get(0); //   /storage/sdcard0*/
            return path;
        }
        //path = pathList.get(0);
        return path;
    }

    /**
     * 获取内置SD卡路径
     * @return
     */
    public String getInnerSDCardPath() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    /**
     * 获取外置SD卡路径
     * @return  应该就一条记录或空
     */
    public static List<String> getExtSDCardPaths() {
        List<String> paths = new ArrayList<String>();
        String extFileStatus = Environment.getExternalStorageState(); //外部sd卡状态
        File extFile = Environment.getExternalStorageDirectory();
        //MEDIA_MOUNTED表示"SD卡正常挂载"，
        // 而MEDIA_UNMOUNTED则表示"有介质，未挂载，在系统中删除"
        if (extFileStatus.equals(Environment.MEDIA_MOUNTED)  //sd卡正常挂载
                && extFile.exists() && extFile.isDirectory()
                && extFile.canWrite()) {
            paths.add(extFile.getAbsolutePath());
        }
        try {
            // obtain executed result of command line code of 'mount', to judgea
            // whether tfCard exists by the result
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("mount");
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            int mountPathIndex = 1;
            while ((line = br.readLine()) != null) {
                // format of sdcard file system: vfat/fuse
                if ((!line.contains("fat") && !line.contains("fuse") && !line
                        .contains("storage"))
                        || line.contains("secure")
                        || line.contains("asec")
                        || line.contains("firmware")
                        || line.contains("shell")
                        || line.contains("obb")
                        || line.contains("legacy") || line.contains("data")) {
                    continue;
                }
                String[] parts = line.split(" ");
                int length = parts.length;
                if (mountPathIndex >= length) {
                    continue;
                }
                String mountPath = parts[mountPathIndex];
                if (!mountPath.contains("/") || mountPath.contains("data")
                        || mountPath.contains("Data")) {
                    continue;
                }
                File mountRoot = new File(mountPath);
                if (!mountRoot.exists() || !mountRoot.isDirectory()
                        || !mountRoot.canWrite()) {
                    continue;
                }
                boolean equalsToPrimarySD = mountPath.equals(extFile
                        .getAbsolutePath());
                if (equalsToPrimarySD) {
                    continue;
                }
                paths.add(mountPath);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return paths;
    }

    public static String getInnerStorageDir() {
        if (!(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))) {
            return "";
        }
        File dirFile = Environment.getExternalStorageDirectory();
        Log.d(TAG, dirFile.getAbsolutePath());
        return dirFile.getAbsolutePath();
    }

    public static String getTFDir() {
        String path = "/storage/sdcard1";
//        try {
//            InputStream ins=Runtime.getRuntime().exec("mount").getInputStream();
//            BufferedReader reader=new BufferedReader(new InputStreamReader(ins));
//            String line="";
//            while((line=reader.readLine())!=null){
//                if(line.contains("sdcard")){
//                    if(line.contains("vfat")||line.contains("fuse")){
//                        String split[]=line.split(" ");
//                        path=split[1];
//                        Log.d(TAG, path);
//
//                    }
//                }
//            }
//
//            reader.close();
//            ins.close();
//
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }finally{
//
//        }
        return path;
    }

    public static long getTFTotalSize(String path) {
        long blockSize = 0;
        long totalBlocks = 0;
        try {
            StatFs stat = new StatFs(path);
            blockSize = stat.getBlockSize();
            totalBlocks = stat.getBlockCount();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return blockSize * totalBlocks;
    }

    public static long getAvailableSize(String path) {
        try {
            File base = new File(path);
            StatFs stat = new StatFs(base.getPath());
            long nAvailableCount = stat.getBlockSize() * ((long) stat.getAvailableBlocks());
            return nAvailableCount;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return 0;
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
                    /*HashMap<String, String> headers = null;
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

    public static int getCharacterPosition(String src, String sub, int n, boolean isForword) {
        Log.e(TAG, "getCharacterPosition: src:" + src + "sub:" + sub);
        Matcher slashMatcher = Pattern.compile(sub).matcher(src);
        int mIdx = 0;
        if (isForword) {
            while (slashMatcher.find()) {
                Log.e(TAG, "slashMatcher.find()");
                mIdx++;
                //当"sub"符号第n次出现的位置
                if (mIdx == n) {
                    break;
                }
            }
        } else {
            while (slashMatcher.find()) {
                mIdx++;
            }
            int tmp = 0;
            while (slashMatcher.find()) {
                tmp++;
                if (tmp == mIdx - n + 1) break;
            }
        }
        Log.e(TAG, "getCharacterPosition" + String.valueOf(slashMatcher.start()));
        return slashMatcher.start();
    }


    public static String[] splitString(String src) {
        if (src != null) {
            Pattern p = Pattern.compile("[" + File.separator + "]+");
            String[] result = p.split(src);
            return result;
        } else {
            return null;
        }
    }

    public static String[] splitString(String src, String split) {
        if (src != null && split != null) {
            Pattern p = Pattern.compile("[" + split + "]+");
            String[] result = p.split(src);
            return result;
        } else {
            return null;
        }
    }

    public static String splitString(String src, int countDown) {
        if (src != null && countDown > 0) {
            Pattern p = Pattern.compile("[" + File.separator + "]+");
            String[] result = p.split(src);
            int length = result.length;
            if (countDown > length) return null;
            return result[length - countDown];
        } else return null;
    }


    public static String getFileNoNamePath(String src) {
        String path[] = splitString(src);
        int length = path.length;
        String res = "";
        for (int i = 0; i < length - 1; i++) {
            res += path[i] + File.separator;
        }
        return res;
    }

    public static String getFilePrefix(String fileName) {
        if (fileName != null) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        } else
            return null;
    }
    //检查sd卡是否存在
    public static boolean isSDExist(){
        boolean ret = false;
        if (getExtSDCardPaths().size() > 1){
            ret = true;
        }else{
            ret = false;
        }
        return ret;
    }
    public static boolean isDirectoryWriteable(File parentFile) {
        boolean ret = parentFile.exists();
        return ret;
    }
}

  /*  *//**
     * 该函数获取路径的倒数第 countDown 个字符串,example : "/a/b/c/d",coutDown为1时返回d
     * @param src 路径字符串
     * @param countDown 倒数第几个
     * @return 字符串
     *//*
    public  String splitString(String src, int countDown) {
        if (src != null && countDown > 0) {
            Pattern p = Pattern.compile("[" + File.separator + "]+");
            String[] result = p.split(src);
            int length = result.length;
            if (countDown > length) return null;
            return result[length - countDown];
        } else return null;
    }

    *//**
     * 该方法将src 按照 split 分割,返回字符串数组
     * @param src 路径字符串
     * @param split 按照该字符串分割
     * @return 路径字符串数组
     *//*
    public String[] splitString(String src, String split) {
        if (src != null && split != null) {
            Pattern p = Pattern.compile("[" + split + "]+");
            String[] result = p.split(src);
            return result;
        } else {
            return null;
        }
    }*/