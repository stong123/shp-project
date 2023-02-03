package cx.shapefile.utils.cx;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileUtils
{
    /**
     文件不存在则创建文件，并创建不存在的目录
     */
    public static void creatEmptyDir(File file) throws IOException
    {
        if (!file.exists())
        {
            File dir = new File(file.getParent());
            dir.mkdirs();
            file.createNewFile();
        }
    }

    public static String getShortName(String path)
    {
        int				startPos = path.lastIndexOf("/");

        if(startPos < 0)
            startPos = path.lastIndexOf("\\");

        if(path.lastIndexOf(".") == -1){        //如果没有后缀，则取最后一截目录名称
            return path.substring(startPos + 1);
        }
        return path.substring(startPos + 1, path.lastIndexOf("."));
    }

    public static boolean endWiths(String filename, String type)
    {
        int 				len1 = filename.length();
        int					len2 = type.length();
        int					pos = filename.lastIndexOf(type);

        if ((pos >= 0) && (pos + len2 == len1))
        {
            return true;
        }
        return false;
    }

    /**
     * 把zip文件解压
     * @param inputFileName	待解压Zip源文件名
     * @param outFileName	解压后输出文件名或路径
     * @throws IOException
     */
    public static void unzip(String inputFileName, String outFileName) throws Exception
    {
        OutputStream os = null;
        InputStream is = null;
        ZipFile zfile = null;
        @SuppressWarnings("rawtypes") Enumeration zList = null;
        ZipEntry ze = null;
        File					file = null;

        try
        {
            //创建一个zip文件对象
            zfile = new ZipFile(inputFileName, Charset.forName("GBK"));// 支持中文的shp文件

            //提前zip文件对象中的各文件的ZipEntry对象，存入枚举对象中
            zList = zfile.entries();

            int						bufSize = 1024 * 1024;
            byte[]					buf = new byte[bufSize];
            int						readLen = 0;

            //在枚举对象中遍历每个ZipEntry文件对象
            while (zList.hasMoreElements())
            {
                ze = (ZipEntry) zList.nextElement();
                file = new File(outFileName, ze.getName());

                //创建父文件夹
                if (!file.getParentFile().exists())
                    file.getParentFile().mkdirs();

                //创建文件
                if (ze.isDirectory())
                {
                    file.mkdir();
                }
                else
                {
                    //以ZipEntry为参数得到一个InputStream，并写到OutputStream中
                    os = new BufferedOutputStream(new FileOutputStream(file));
                    is = new BufferedInputStream(zfile.getInputStream(ze));
                    while ((readLen = is.read(buf, 0, bufSize)) != -1)
                    {
                        os.write(buf, 0, readLen);
                    }
                    is.close();
                    is = null;
                    os.close();
                    os = null;
                }
            }
            zfile.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if(is != null)
                is.close();
            if(os != null)
                os.close();
        }
    }

}
